package com.example.shopapi.repository.mongo;

import com.example.shopapi.document.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);

    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);

    List<Product> findByIdIn(List<String> ids);

    @Query("{'name': {$regex: ?0, $options: 'i'}, 'active': true}")
    Page<Product> searchByName(String keyword, Pageable pageable);

    @Query("{'$or': [{'name': {$regex: ?0, $options: 'i'}}, {'description': {$regex: ?0, $options: 'i'}}], 'active': true}")
    Page<Product> searchByKeyword(String keyword, Pageable pageable);

    @Query("{'price': {$gte: ?0, $lte: ?1}, 'active': true}")
    Page<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("{'category': ?0, 'price': {$gte: ?1, $lte: ?2}, 'active': true}")
    Page<Product> findByCategoryAndPriceRange(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    @Query("{'stockQuantity': {$gt: 0}, 'active': true}")
    Page<Product> findInStock(Pageable pageable);

    @Query("{'stockQuantity': {$lte: ?0}, 'active': true}")
    List<Product> findLowStockProducts(int threshold);

    @Query("{'createdBy': ?0}")
    Page<Product> findByCreatedBy(Long createdBy, Pageable pageable);

    long countByActiveTrue();

    long countByCategory(String category);

    @Query(value = "{'active': true}", count = true)
    long countActiveProducts();

    @Query("{'category': ?0, 'active': true, '_id': {$ne: ?1}}")
    List<Product> findRelatedProducts(String category, String excludeId, Pageable pageable);

    List<Product> findDistinctCategoriesByActiveTrue();

    @Query(value = "{}", fields = "{'category': 1}")
    List<Product> findAllCategories();
}

