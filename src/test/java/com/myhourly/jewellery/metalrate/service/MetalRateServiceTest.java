package com.myhourly.jewellery.metalrate.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.DuplicateResourceException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.metalrate.dto.MetalRatePageResponse;
import com.myhourly.jewellery.metalrate.dto.MetalRateRequest;
import com.myhourly.jewellery.metalrate.dto.MetalRateResponse;
import com.myhourly.jewellery.metalrate.dto.MetalValueResponse;
import com.myhourly.jewellery.metalrate.entity.MetalRate;
import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;
import com.myhourly.jewellery.metalrate.mapper.MetalRateMapper;
import com.myhourly.jewellery.metalrate.repository.MetalRateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetalRateServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0, 0);

    @Mock
    private MetalRateRepository repository;

    private MetalRateService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new MetalRateService(repository, new MetalRateMapper(), clock);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRateDefaultsEffectiveFromToNowAndRecordsAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "admin@shop.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(repository.existsByMetalTypeAndPurityAndEffectiveFrom(MetalType.GOLD, Purity.K22, NOW))
                .thenReturn(false);
        when(repository.save(any(MetalRate.class))).thenAnswer(invocation -> {
            MetalRate saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        MetalRateResponse response = service.createRate(
                new MetalRateRequest(MetalType.GOLD, Purity.K22, new BigDecimal("7000.00"), null, "  Morning rate "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.effectiveFrom()).isEqualTo(NOW);
        assertThat(response.createdBy()).isEqualTo("admin@shop.com");
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.remarks()).isEqualTo("Morning rate");
    }

    @Test
    void createRateUsesSystemUserWhenNobodyIsAuthenticated() {
        when(repository.save(any(MetalRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MetalRateResponse response = service.createRate(
                new MetalRateRequest(MetalType.SILVER, Purity.SILVER_925, new BigDecimal("95.50"), null, null));

        assertThat(response.createdBy()).isEqualTo(MetalRateService.SYSTEM_USER);
    }

    @Test
    void createRateAcceptsFutureEffectiveDate() {
        LocalDateTime tomorrow = NOW.plusDays(1).withNano(123);
        when(repository.save(any(MetalRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MetalRateResponse response = service.createRate(
                new MetalRateRequest(MetalType.GOLD, Purity.K24, new BigDecimal("7600.00"), tomorrow, null));

        assertThat(response.effectiveFrom()).isEqualTo(NOW.plusDays(1));
    }

    @Test
    void createRateRejectsPurityThatDoesNotMatchMetal() {
        assertThatThrownBy(() -> service.createRate(
                new MetalRateRequest(MetalType.SILVER, Purity.K22, new BigDecimal("95.00"), null, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("not valid for metal SILVER");
        verify(repository, never()).save(any());
    }

    @Test
    void createRateRejectsBackDatedEntries() {
        LocalDateTime yesterday = NOW.minusDays(1);

        assertThatThrownBy(() -> service.createRate(
                new MetalRateRequest(MetalType.GOLD, Purity.K22, new BigDecimal("7000.00"), yesterday, null)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("cannot be in the past");
    }

    @Test
    void createRateAllowsSmallClockSkew() {
        LocalDateTime twoMinutesAgo = NOW.minusMinutes(2);
        when(repository.save(any(MetalRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MetalRateResponse response = service.createRate(
                new MetalRateRequest(MetalType.GOLD, Purity.K22, new BigDecimal("7000.00"), twoMinutesAgo, null));

        assertThat(response.effectiveFrom()).isEqualTo(twoMinutesAgo);
    }

    @Test
    void createRateRejectsDuplicateEffectiveTime() {
        when(repository.existsByMetalTypeAndPurityAndEffectiveFrom(MetalType.GOLD, Purity.K22, NOW))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createRate(
                new MetalRateRequest(MetalType.GOLD, Purity.K22, new BigDecimal("7000.00"), null, null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getCurrentRateLooksUpLatestRateEffectiveNow() {
        when(repository.findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                MetalType.GOLD, Purity.K22, NOW))
                .thenReturn(Optional.of(rate(5L, MetalType.GOLD, Purity.K22, "7100.00", NOW.minusHours(1))));

        MetalRateResponse response = service.getCurrentRate(MetalType.GOLD, Purity.K22);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.ratePerGram()).isEqualByComparingTo("7100.00");
    }

    @Test
    void getCurrentRateThrowsWhenNoRateExists() {
        when(repository.findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                MetalType.PLATINUM, Purity.PLATINUM_950, NOW))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentRate(MetalType.PLATINUM, Purity.PLATINUM_950))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getApplicableRateUsesRequestedPointInTime() {
        LocalDateTime lastWeek = NOW.minusWeeks(1);
        when(repository.findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                MetalType.GOLD, Purity.K18, lastWeek))
                .thenReturn(Optional.of(rate(2L, MetalType.GOLD, Purity.K18, "5200.00", lastWeek.minusDays(1))));

        MetalRateResponse response = service.getApplicableRate(MetalType.GOLD, Purity.K18, lastWeek);

        assertThat(response.id()).isEqualTo(2L);
    }

    @Test
    void getAllCurrentRatesSkipsPuritiesWithoutRates() {
        when(repository.findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                any(MetalType.class), any(Purity.class), eq(NOW)))
                .thenReturn(Optional.empty());
        when(repository.findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                MetalType.GOLD, Purity.K22, NOW))
                .thenReturn(Optional.of(rate(1L, MetalType.GOLD, Purity.K22, "7000.00", NOW)));
        when(repository.findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                MetalType.SILVER, Purity.SILVER_925, NOW))
                .thenReturn(Optional.of(rate(2L, MetalType.SILVER, Purity.SILVER_925, "95.00", NOW)));

        List<MetalRateResponse> rates = service.getAllCurrentRates();

        assertThat(rates).extracting(MetalRateResponse::purity)
                .containsExactly(Purity.K22, Purity.SILVER_925);
    }

    @Test
    void calculateMetalValueMultipliesNetWeightByRateAndRoundsHalfUp() {
        when(repository.findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                MetalType.GOLD, Purity.K22, NOW))
                .thenReturn(Optional.of(rate(9L, MetalType.GOLD, Purity.K22, "7000.55", NOW.minusHours(2))));

        MetalValueResponse response = service.calculateMetalValue(
                MetalType.GOLD, Purity.K22, new BigDecimal("10.255"), null);

        assertThat(response.metalValue()).isEqualByComparingTo("71790.64");
        assertThat(response.metalValue().scale()).isEqualTo(2);
        assertThat(response.rateId()).isEqualTo(9L);
        assertThat(response.ratePerGram()).isEqualByComparingTo("7000.55");
    }

    @Test
    void calculateMetalValueMatchesRequirementExample() {
        when(repository.findFirstByMetalTypeAndPurityAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
                MetalType.GOLD, Purity.K22, NOW))
                .thenReturn(Optional.of(rate(1L, MetalType.GOLD, Purity.K22, "7000.00", NOW)));

        MetalValueResponse response = service.calculateMetalValue(
                MetalType.GOLD, Purity.K22, new BigDecimal("10"), null);

        assertThat(response.metalValue()).isEqualByComparingTo("70000.00");
    }

    @Test
    void calculateMetalValueRejectsNonPositiveWeight() {
        assertThatThrownBy(() -> service.calculateMetalValue(
                MetalType.GOLD, Purity.K22, BigDecimal.ZERO, null))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void getHistoryAppliesFiltersAndNewestFirstSorting() {
        MetalRate older = rate(1L, MetalType.GOLD, Purity.K22, "6900.00", NOW.minusDays(2));
        MetalRate newer = rate(2L, MetalType.GOLD, Purity.K22, "7000.00", NOW.minusDays(1));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(repository.search(eq(MetalType.GOLD), eq(Purity.K22), eq(null), eq(null), pageable.capture()))
                .thenAnswer(invocation -> new PageImpl<>(List.of(newer, older), PageRequest.of(0, 10), 2));

        MetalRatePageResponse page = service.getHistory(MetalType.GOLD, Purity.K22, null, null, 0, 10);

        assertThat(page.content()).extracting(MetalRateResponse::id).containsExactly(2L, 1L);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(pageable.getValue().getSort().getOrderFor("effectiveFrom").isDescending()).isTrue();
    }

    @Test
    void getHistoryRejectsInvertedDateRange() {
        assertThatThrownBy(() -> service.getHistory(null, null, NOW, NOW.minusDays(1), 0, 20))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void getHistoryRejectsOversizedPage() {
        assertThatThrownBy(() -> service.getHistory(null, null, null, null, 0, MetalRateService.MAX_PAGE_SIZE + 1))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void deleteScheduledRateRemovesFutureRate() {
        MetalRate future = rate(3L, MetalType.GOLD, Purity.K22, "7200.00", NOW.plusDays(1));
        when(repository.findById(3L)).thenReturn(Optional.of(future));

        service.deleteScheduledRate(3L);

        verify(repository).delete(future);
    }

    @Test
    void deleteScheduledRateRejectsRateAlreadyInEffect() {
        MetalRate active = rate(4L, MetalType.GOLD, Purity.K22, "7000.00", NOW.minusHours(1));
        when(repository.findById(4L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.deleteScheduledRate(4L))
                .isInstanceOf(BusinessValidationException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    private static MetalRate rate(Long id, MetalType metalType, Purity purity, String ratePerGram,
                                  LocalDateTime effectiveFrom) {
        MetalRate rate = new MetalRate();
        rate.setId(id);
        rate.setMetalType(metalType);
        rate.setPurity(purity);
        rate.setRatePerGram(new BigDecimal(ratePerGram));
        rate.setEffectiveFrom(effectiveFrom);
        rate.setCreatedBy("admin@shop.com");
        rate.setCreatedAt(effectiveFrom);
        return rate;
    }
}
