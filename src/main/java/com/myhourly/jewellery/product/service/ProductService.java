package com.myhourly.jewellery.product.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.DuplicateResourceException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.product.dto.ProductRequestDto;
import com.myhourly.jewellery.product.dto.ProductResponseDto;
import com.myhourly.jewellery.product.entity.Product;
import com.myhourly.jewellery.product.enums.MetalType;
import com.myhourly.jewellery.product.enums.Purity;
import com.myhourly.jewellery.product.mapper.ProductMapper;
import com.myhourly.jewellery.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

//    Validates the stone weight against the gross weight.

    private void validateWeights(BigDecimal grossWeight, BigDecimal stoneWeight) {
        if (stoneWeight.compareTo(grossWeight) > 0) {
            throw new BusinessValidationException("stone weight cannot exceed gross weight");
        }
    }

    //    Normalizes the SKU by removing extra whitespace and converting it to uppercase.
    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }


    @Transactional
    public ProductResponseDto create(ProductRequestDto productRequestDto) {

        validateWeights(productRequestDto.grossWeight(), productRequestDto.stoneWeight());

        String sku = normalizeSku(productRequestDto.sku());

        if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("A product with SKU" + sku + " already exists");
        }

//        Convert the request DTO into a Product entity.
        Product product = productMapper.toEntity(productRequestDto);

//        store normalized SKU consistently

        product.updateDetails(
                sku,
                productRequestDto.name().trim(),
                productRequestDto.category().trim(),
                productRequestDto.metalType(),
                productRequestDto.purity(),
                productRequestDto.grossWeight(),
                productRequestDto.stoneWeight(),
                productRequestDto.makingChargeType(),
                productRequestDto.makingChargeValue()
        );

//      Save the product to the database.
        Product saved = productRepository.save(product);

//      Convert the saved entity into a response DTO.
        return productMapper.toResponse(saved);

    }

    public ProductResponseDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with ID:" + id
                        )
                );
//        Convert the Product entity into a response DTO.
        return productMapper.toResponse(product);
    }


    public Page<ProductResponseDto> search(
            String keyword,
            String category,
            MetalType metalType,
            Purity purity,
            Boolean active,
            int page,
            int size,
            String sortBy,
            String direction) {

        // Step 1: Validate the requested page number.
        if (page < 0) {
            throw new BusinessValidationException(
                    "Page number cannot be negative"
            );
        }

        // Step 2: Limit page size to prevent excessively large responses.
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessValidationException(
                    "Page size must be between 1 and 100"
            );
        }

        // Step 3: Build sorting rules after validating the field and direction.
        Sort sort = buildSort(sortBy, direction);

        // Step 4: Create the pagination request.
        Pageable pageable = PageRequest.of(page, size, sort);

        // Step 5: Search the database and convert each entity to a response DTO.
        return productRepository.searchProducts(
                        normalizeFilter(keyword),
                        normalizeFilter(category),
                        metalType,
                        purity,
                        active,
                        pageable
                )
                .map(productMapper::toResponse);
    }

    private Sort buildSort(String sortBy, String direction) {

        // Allow sorting only by approved Product entity properties.
        if (!"name".equals(sortBy)
                && !"sku".equals(sortBy)
                && !"createdAt".equals(sortBy)
                && !"netWeight".equals(sortBy)) {

            throw new BusinessValidationException(
                    "Unsupported sort field"
            );
        }

        // Convert ASC or DESC text into Spring's Sort.Direction.
        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new BusinessValidationException(
                    "Sort direction must be ASC or DESC"
            );
        }

        // Return the sorting configuration.
        return Sort.by(sortDirection, sortBy);
    }

    private String normalizeFilter(String value) {

        // Treat null or blank input as no filter.
        if (value == null || value.isBlank()) {
            return null;
        }

        // Remove unnecessary spaces from the input.
        return value.trim();
    }

    @Transactional
    public ProductResponseDto update(
            Long id,
            ProductRequestDto request) {

        // Step 1: Validate the gross weight and stone weight.
        validateWeights(
                request.grossWeight(),
                request.stoneWeight()
        );

        // Find the existing product.
        Product product = findProduct(id);

        // Normalize the SKU before checking for duplicates.
        String sku = normalizeSku(request.sku());

        // Prevent another product from using the same SKU.
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateResourceException(
                    "A product with SKU " + sku + " already exists"
            );
        }


        // Apply normalized values and updated product details.
        product.updateDetails(
                sku,
                request.name().trim(),
                request.category().trim(),
                request.metalType(),
                request.purity(),
                request.grossWeight(),
                request.stoneWeight(),
                request.makingChargeType(),
                request.makingChargeValue()
        );

        // Convert the updated entity into a response DTO.
        return productMapper.toResponse(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with ID: " + id
                        )
                );
    }


    @Transactional
    public ProductResponseDto changeStatus(
            Long id,
            boolean active) {

        // Find the existing product.
        Product product = findProduct(id);

        // Change the product's active status.
        product.changeStatus(active);

        // Convert the updated entity into response DTO.
        return productMapper.toResponse(product);
    }

}
