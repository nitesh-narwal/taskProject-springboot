package com.example.shopapi.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
@CompoundIndex(name = "category_price_idx", def = "{'category': 1, 'price': 1}")
@CompoundIndex(name = "active_stock_idx", def = "{'active': 1, 'stockQuantity': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;

    @TextIndexed(weight = 3)
    private String name;

    @TextIndexed
    private String description;

    private BigDecimal price;

    @Indexed
    private String category;

    @Builder.Default
    private Integer stockQuantity = 0;

    private String imageUrl;

    @Builder.Default
    private List<String> additionalImages = new ArrayList<>();

    private String sku;

    private String brand;

    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    @Builder.Default
    private List<ProductAttribute> attributes = new ArrayList<>();

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean featured = false;

    private BigDecimal discountPrice;

    private Integer discountPercentage;

    @Indexed
    private Long createdBy;

    private Long updatedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductAttribute {
        private String name;
        private String value;
    }

    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    public BigDecimal getEffectivePrice() {
        if (discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) > 0) {
            return discountPrice;
        }
        return price;
    }
}

