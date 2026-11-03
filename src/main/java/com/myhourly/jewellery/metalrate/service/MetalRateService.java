package com.myhourly.jewellery.metalrate.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.DuplicateResourceException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.metalrate.dto.MetalRatePageResponse;
import com.myhourly.jewellery.metalrate.dto.MetalRateRequest;
import com.myhourly.jewellery.metalrate.dto.MetalRateResponse;
import com.myhourly.jewellery.metalrate.dto.MetalValueRequest;
import com.myhourly.jewellery.metalrate.dto.MetalValueResponse;
import com.myhourly.jewellery.metalrate.entity.MetalRate;
import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;
import com.myhourly.jewellery.metalrate.mapper.MetalRateMapper;
import com.myhourly.jewellery.metalrate.repository.MetalRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MetalRateService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final Duration BACKDATE_TOLERANCE = Duration.ofMinutes(5);
    public static final String SYSTEM_USER = "system";

    private final MetalRateRepository repository;
    private final MetalRateMapper mapper;
    private final Clock clock;

    @Autowired
    public MetalRateService(MetalRateRepository repository, MetalRateMapper mapper) {
        this(repository, mapper, Clock.systemDefaultZone());
    }

    MetalRateService(MetalRateRepository repository, MetalRateMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public MetalRateResponse createRate(MetalRateRequest request) {
        validatePurity(request.metalType(), request.purity());

        LocalDateTime now = now();
        LocalDateTime effectiveFrom = request.effectiveFrom() == null
                ? now
                : request.effectiveFrom().truncatedTo(ChronoUnit.SECONDS);

        if (effectiveFrom.isBefore(now.minus(BACKDATE_TOLERANCE))) {
            throw new BusinessValidationException(
                    "Effective date cannot be in the past; record a new rate instead of back-dating");
        }

        if (repository.existsByMetalTypeAndPurityAndEffectiveFrom(
                request.metalType(), request.purity(), effectiveFrom)) {
            throw new DuplicateResourceException(
                    "A rate for " + request.metalType() + " " + request.purity()
                            + " already exists at " + effectiveFrom);
        }

        MetalRate rate = mapper.toEntity(request);
        rate.setEffectiveFrom(effectiveFrom);
        rate.setCurrency(MetalRate.DEFAULT_CURRENCY);
        rate.setCreatedBy(currentUser());
        rate.setCreatedAt(now);

        return mapper.toResponse(repository.save(rate));
    }

    public MetalRateResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    public MetalRateResponse getCurrentRate(MetalType metalType, Purity purity) {
        return getApplicableRate(metalType, purity, null);
    }

    public MetalRateResponse getApplicableRate(MetalType metalType, Purity purity, LocalDateTime at) {
        return mapper.toResponse(findApplicable(metalType, purity, at));
    }

    public List<MetalRateResponse> getAllCurrentRates() {
        LocalDateTime now = now();
        return Arrays.stream(Purity.values())
                .map(purity -> repository
                        .findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                                purity.getMetalType(), purity, now))
                .flatMap(Optional::stream)
                .map(mapper::toResponse)
                .toList();
    }

    public MetalRatePageResponse getHistory(
            MetalType metalType, Purity purity,
            LocalDateTime from, LocalDateTime to,
            int page, int size) {

        if (metalType != null && purity != null) {
            validatePurity(metalType, purity);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessValidationException("'from' must be on or before 'to'");
        }
        if (page < 0) {
            throw new BusinessValidationException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessValidationException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("effectiveFrom"), Sort.Order.desc("id")));
        Page<MetalRate> result = repository.search(metalType, purity, from, to, pageable);

        return new MetalRatePageResponse(
                result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    public MetalValueResponse calculateMetalValue(MetalValueRequest request) {
        return calculateMetalValue(request.metalType(), request.purity(), request.netWeight(), request.at());
    }

    public MetalValueResponse calculateMetalValue(
            MetalType metalType, Purity purity, BigDecimal netWeight, LocalDateTime at) {

        if (netWeight == null || netWeight.signum() <= 0) {
            throw new BusinessValidationException("Net weight must be greater than zero");
        }

        MetalRate rate = findApplicable(metalType, purity, at);
        BigDecimal metalValue = netWeight
                .multiply(rate.getRatePerGram())
                .setScale(2, RoundingMode.HALF_UP);

        return new MetalValueResponse(
                metalType,
                purity,
                netWeight,
                rate.getId(),
                rate.getRatePerGram(),
                rate.getCurrency(),
                rate.getEffectiveFrom(),
                metalValue);
    }

    @Transactional
    public void deleteScheduledRate(Long id) {
        MetalRate rate = findById(id);
        if (!rate.getEffectiveFrom().isAfter(now())) {
            throw new BusinessValidationException(
                    "Only rates scheduled for the future can be deleted; record a new rate to correct an active one");
        }
        repository.delete(rate);
    }

    private MetalRate findApplicable(MetalType metalType, Purity purity, LocalDateTime at) {
        if (metalType == null || purity == null) {
            throw new BusinessValidationException("Metal type and purity are required");
        }
        validatePurity(metalType, purity);

        LocalDateTime pointInTime = at == null ? now() : at;
        return repository
                .findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                        metalType, purity, pointInTime)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No rate found for " + metalType + " " + purity + " effective at " + pointInTime));
    }

    private MetalRate findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metal rate not found with id " + id));
    }

    private void validatePurity(MetalType metalType, Purity purity) {
        if (!purity.belongsTo(metalType)) {
            throw new BusinessValidationException(
                    "Purity " + purity + " is not valid for metal " + metalType);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return SYSTEM_USER;
        }
        return authentication.getName();
    }
}
