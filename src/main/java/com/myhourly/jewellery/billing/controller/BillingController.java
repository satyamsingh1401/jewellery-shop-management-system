package com.myhourly.jewellery.billing.controller;

import com.myhourly.jewellery.billing.dto.BillCalculationRequest;
import com.myhourly.jewellery.billing.dto.BillCalculationResponse;
import com.myhourly.jewellery.billing.service.BillingCalculatorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingCalculatorService calculatorService;

    public BillingController(BillingCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/calculate")
    public BillCalculationResponse calculate(@Valid @RequestBody BillCalculationRequest request) {
        return calculatorService.calculate(request);
    }
}