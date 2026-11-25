package com.myhourly.jewellery.product.mapper;

import com.myhourly.jewellery.product.dto.ProductRequestDto;
import com.myhourly.jewellery.product.dto.ProductResponseDto;
import com.myhourly.jewellery.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {


    public ProductResponseDto toResponse(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getMetalType(),
                product.getPurity(),
                product.getGrossWeight(),
                product.getStoneWeight(),
                product.getNetWeight(),
                product.getMakingChargeType(),
                product.getMakingChargeValue(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
