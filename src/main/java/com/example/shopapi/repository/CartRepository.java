package com.example.shopapi.repository;

import com.example.shopapi.model.CartItem;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CartRepository {

    private final List<CartItem> cartItems = new ArrayList<>();

    public CartItem save(CartItem cartItem) {
        Optional<CartItem> existing = findByItemId(cartItem.getItemId());
        if (existing.isPresent()) {
            // Update quantity if item already in cart
            existing.get().setQuantity(cartItem.getQuantity());
            return existing.get();
        } else {
            cartItems.add(cartItem);
            return cartItem;
        }
    }

    public Optional<CartItem> findByItemId(Long itemId) {
        return cartItems.stream()
                .filter(cartItem -> cartItem.getItemId().equals(itemId))
                .findFirst();
    }

    public List<CartItem> findAll() {
        return new ArrayList<>(cartItems);
    }

    public void deleteByItemId(Long itemId) {
        cartItems.removeIf(cartItem -> cartItem.getItemId().equals(itemId));
    }

    public void clear() {
        cartItems.clear();
    }

    public boolean existsByItemId(Long itemId) {
        return cartItems.stream().anyMatch(cartItem -> cartItem.getItemId().equals(itemId));
    }
}

