package com.example.shopapi.controller;

import com.example.shopapi.dto.common.ApiResponse;
import com.example.shopapi.dto.common.PagedResponse;
import com.example.shopapi.dto.product.ProductFilterCriteria;
import com.example.shopapi.dto.product.ProductRequest;
import com.example.shopapi.dto.product.ProductResponse;
import com.example.shopapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getAllProducts(pageable);

        return ResponseEntity.ok(PagedResponse.of(
                products.getContent(), page, size, products.getTotalElements()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products with filters")
    public ResponseEntity<PagedResponse<ProductResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        ProductFilterCriteria criteria = ProductFilterCriteria.builder()
                .keyword(keyword).category(category).minPrice(minPrice).maxPrice(maxPrice)
                .inStock(inStock).featured(featured).brand(brand).sortBy(sortBy).sortDir(sortDir)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.searchProducts(criteria, pageable);

        return ResponseEntity.ok(PagedResponse.of(
                products.getContent(), page, size, products.getTotalElements()));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<PagedResponse<ProductResponse>> getByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.searchProducts(
                ProductFilterCriteria.builder().category(category).build(), pageable);

        return ResponseEntity.ok(PagedResponse.of(
                products.getContent(), page, size, products.getTotalElements()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Create a new product (Worker/Admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Update a product (Worker/Admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id, @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Upload product image (Worker/Admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadImage(
            @PathVariable String id, @RequestParam("file") MultipartFile file) throws IOException {
        ProductResponse product = productService.uploadProductImage(id, file);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Delete a product (Worker/Admin only)")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Update product stock (Worker/Admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable String id, @RequestParam int quantity) {
        ProductResponse product = productService.updateStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", product));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Get low stock products (Worker/Admin only)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold) {
        List<ProductResponse> products = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}

