package com.example.shopapi.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stockQuantity;
    private String imageUrl;
    private List<String> additionalImages;
    private String sku;
    private String brand;
    private Double rating;
    private Integer reviewCount;
    private List<ProductAttributeDto> attributes;
    private List<String> tags;
    private Boolean active;
    private Boolean featured;
    private BigDecimal discountPrice;
    private Integer discountPercentage;
    private BigDecimal effectivePrice;
    private Boolean inStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAttributeDto {
        private String name;
        private String value;
    }
}

