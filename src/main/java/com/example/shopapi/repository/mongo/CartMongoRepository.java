package com.example.shopapi.repository.mongo;

import com.example.shopapi.document.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartMongoRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    boolean existsByUserId(Long userId);
}

