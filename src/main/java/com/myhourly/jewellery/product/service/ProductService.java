package com.myhourly.jewellery.product.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.DuplicateResourceException;
import com.myhourly.jewellery.product.dto.ProductRequestDto;
import com.myhourly.jewellery.product.dto.ProductResponseDto;
import com.myhourly.jewellery.product.entity.Product;
import com.myhourly.jewellery.product.mapper.ProductMapper;
import com.myhourly.jewellery.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
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


}
