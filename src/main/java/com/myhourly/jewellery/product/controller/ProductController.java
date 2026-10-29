package com.myhourly.jewellery.product.controller;

import com.myhourly.jewellery.product.dto.ProductRequestDto;
import com.myhourly.jewellery.product.dto.ProductResponseDto;
import com.myhourly.jewellery.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Validated
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductRequestDto request) {
// Step 1: Pass the validated request to the service layer.

        ProductResponseDto response = productService.create(request);

//  step 2: Return the created product with HTTP 201 created
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
