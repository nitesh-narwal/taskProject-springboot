package com.example.shopapi.service;
import com.example.shopapi.document.Product;
import com.example.shopapi.dto.product.ProductFilterCriteria;
import com.example.shopapi.dto.product.ProductRequest;
import com.example.shopapi.dto.product.ProductResponse;
import com.example.shopapi.exception.ResourceNotFoundException;
import com.example.shopapi.repository.mongo.ProductRepository;
import com.example.shopapi.security.UserPrincipal;
import com.example.shopapi.util.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;
    private final CloudinaryService cloudinaryService;
    private final AuditService auditService;
    @Cacheable(value = "products", key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }
    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapToResponse(product);
    }
    public Page<ProductResponse> searchProducts(ProductFilterCriteria criteria, Pageable pageable) {
        Query query = buildSearchQuery(criteria);
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(criteria.getSortDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        query.with(Sort.by(direction, sortBy)).with(pageable);
        List<Product> products = mongoTemplate.find(query, Product.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Product.class);
        return PageableExecutionUtils.getPage(products.stream().map(this::mapToResponse).collect(Collectors.toList()), pageable, () -> total);
    }
    private Query buildSearchQuery(ProductFilterCriteria criteria) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("active").is(true));
        if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(criteria.getKeyword(), "i"),
                    Criteria.where("description").regex(criteria.getKeyword(), "i")));
        }
        if (criteria.getCategory() != null) criteriaList.add(Criteria.where("category").is(criteria.getCategory()));
        if (criteria.getMinPrice() != null) criteriaList.add(Criteria.where("price").gte(criteria.getMinPrice()));
        if (criteria.getMaxPrice() != null) criteriaList.add(Criteria.where("price").lte(criteria.getMaxPrice()));
        if (Boolean.TRUE.equals(criteria.getInStock())) criteriaList.add(Criteria.where("stockQuantity").gt(0));
        if (Boolean.TRUE.equals(criteria.getFeatured())) criteriaList.add(Criteria.where("featured").is(true));
        if (criteria.getBrand() != null) criteriaList.add(Criteria.where("brand").is(criteria.getBrand()));
        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return query;
    }
    @Caching(evict = {@CacheEvict(value = "products", allEntries = true), @CacheEvict(value = "featuredProducts", allEntries = true)})
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName()).description(request.getDescription())
                .price(request.getPrice()).category(request.getCategory())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .sku(request.getSku()).brand(request.getBrand()).tags(request.getTags())
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .discountPrice(request.getDiscountPrice()).discountPercentage(request.getDiscountPercentage())
                .active(true).createdBy(getCurrentUserId()).build();
        if (request.getAttributes() != null) {
            product.setAttributes(request.getAttributes().stream()
                    .map(a -> Product.ProductAttribute.builder().name(a.getName()).value(a.getValue()).build())
                    .collect(Collectors.toList()));
        }
        product = productRepository.save(product);
        auditService.logAction("PRODUCT_CREATED", "PRODUCT", product.getId(), "Product created: " + product.getName());
        return mapToResponse(product);
    }
    @Caching(evict = {@CacheEvict(value = "products", allEntries = true), @CacheEvict(value = "product", key = "#id")})
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setName(request.getName()); product.setDescription(request.getDescription());
        product.setPrice(request.getPrice()); product.setCategory(request.getCategory());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku()); product.setBrand(request.getBrand()); product.setTags(request.getTags());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());
        product.setDiscountPrice(request.getDiscountPrice()); product.setDiscountPercentage(request.getDiscountPercentage());
        product.setUpdatedBy(getCurrentUserId());
        product = productRepository.save(product);
        auditService.logAction("PRODUCT_UPDATED", "PRODUCT", product.getId(), "Product updated");
        return mapToResponse(product);
    }
    @Caching(evict = {@CacheEvict(value = "products", allEntries = true), @CacheEvict(value = "product", key = "#id")})
    public ProductResponse uploadProductImage(String id, MultipartFile file) throws IOException {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        if (product.getImageUrl() != null) cloudinaryService.deleteImage(product.getImageUrl());
        product.setImageUrl(cloudinaryService.uploadImage(file));
        product.setUpdatedBy(getCurrentUserId());
        return mapToResponse(productRepository.save(product));
    }
    @Caching(evict = {@CacheEvict(value = "products", allEntries = true), @CacheEvict(value = "product", key = "#id")})
    public void deleteProduct(String id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        if (product.getImageUrl() != null) cloudinaryService.deleteImage(product.getImageUrl());
        product.setActive(false); product.setUpdatedBy(getCurrentUserId());
        productRepository.save(product);
        auditService.logAction("PRODUCT_DELETED", "PRODUCT", product.getId(), "Product deleted");
    }
    @Caching(evict = {@CacheEvict(value = "products", allEntries = true), @CacheEvict(value = "product", key = "#id")})
    public ProductResponse updateStock(String id, int quantity) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setStockQuantity(quantity); product.setUpdatedBy(getCurrentUserId());
        return mapToResponse(productRepository.save(product));
    }
    public List<ProductResponse> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold).stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    private ProductResponse mapToResponse(Product p) {
        return ProductResponse.builder().id(p.getId()).name(p.getName()).description(p.getDescription())
                .price(p.getPrice()).category(p.getCategory()).stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl()).additionalImages(p.getAdditionalImages()).sku(p.getSku())
                .brand(p.getBrand()).rating(p.getRating()).reviewCount(p.getReviewCount())
                .tags(p.getTags()).active(p.getActive()).featured(p.getFeatured())
                .discountPrice(p.getDiscountPrice()).discountPercentage(p.getDiscountPercentage())
                .effectivePrice(p.getEffectivePrice()).inStock(p.isInStock())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
    private Long getCurrentUserId() {
        try { return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        } catch (Exception e) { return null; }
    }
}
