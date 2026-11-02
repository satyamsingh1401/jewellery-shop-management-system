package com.myhourly.jewellery.product.controller;

import com.myhourly.jewellery.product.dto.ProductRequestDto;
import com.myhourly.jewellery.product.dto.ProductResponseDto;
import com.myhourly.jewellery.product.dto.ProductStatusRequestDto;
import com.myhourly.jewellery.product.enums.MetalType;
import com.myhourly.jewellery.product.enums.Purity;
import com.myhourly.jewellery.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(
            @PathVariable @Positive Long id) {

        // Pass the product ID to the service layer.
        ProductResponseDto response = productService.getById(id);

        // Return the product details with HTTP 200 OK.
        return ResponseEntity.ok(response);

    }


    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> search(

            // Optional filters provided through URL query parameters.
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) MetalType metalType,
            @RequestParam(required = false) Purity purity,
            @RequestParam(required = false) Boolean active,

            // Pagination settings with default values.
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,

            // Sorting settings with default values.
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        // Pass all search criteria to the service layer.
        Page<ProductResponseDto> response = productService.search(
                keyword,
                category,
                metalType,
                purity,
                active,
                page,
                size,
                sortBy,
                direction
        );

        // Return the matching products with HTTP 200 OK.
        return ResponseEntity.ok(response);
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProductRequestDto request) {

        // Pass the product ID and updated details to the service.
        ProductResponseDto response = productService.update(id, request);

//        Return the updated product with HTTP 200 OK.
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponseDto> changeStatus(
            @PathVariable @Positive Long id, @Valid @RequestBody ProductStatusRequestDto request) {

        // Send product ID and requested status to service.
        ProductResponseDto response = productService.changeStatus(id, request.active());

        // Return updated product details.
        return ResponseEntity.ok(response);
    }

}
