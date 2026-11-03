package com.myhourly.jewellery.metalrate.controller;

import com.myhourly.jewellery.metalrate.dto.MetalRatePageResponse;
import com.myhourly.jewellery.metalrate.dto.MetalRateRequest;
import com.myhourly.jewellery.metalrate.dto.MetalRateResponse;
import com.myhourly.jewellery.metalrate.dto.MetalValueRequest;
import com.myhourly.jewellery.metalrate.dto.MetalValueResponse;
import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;
import com.myhourly.jewellery.metalrate.service.MetalRateService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/metal-rates")
public class MetalRateController {

    private final MetalRateService service;

    public MetalRateController(MetalRateService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MetalRateResponse> create(@Valid @RequestBody MetalRateRequest request) {
        MetalRateResponse created = service.createRate(request);
        return ResponseEntity
                .created(URI.create("/api/metal-rates/" + created.id()))
                .body(created);
    }

    @GetMapping("/current")
    public MetalRateResponse current(
            @RequestParam MetalType metalType,
            @RequestParam Purity purity) {
        return service.getCurrentRate(metalType, purity);
    }

    @GetMapping("/current/all")
    public List<MetalRateResponse> allCurrent() {
        return service.getAllCurrentRates();
    }

    @GetMapping("/applicable")
    public MetalRateResponse applicable(
            @RequestParam MetalType metalType,
            @RequestParam Purity purity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return service.getApplicableRate(metalType, purity, at);
    }

    @GetMapping("/history")
    public MetalRatePageResponse history(
            @RequestParam(required = false) MetalType metalType,
            @RequestParam(required = false) Purity purity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getHistory(metalType, purity, from, to, page, size);
    }

    @GetMapping("/{id}")
    public MetalRateResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("/metal-value")
    public MetalValueResponse metalValue(@Valid @RequestBody MetalValueRequest request) {
        return service.calculateMetalValue(request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteScheduled(@PathVariable Long id) {
        service.deleteScheduledRate(id);
        return ResponseEntity.noContent().build();
    }
}
