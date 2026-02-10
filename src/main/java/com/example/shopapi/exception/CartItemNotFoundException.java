package com.example.shopapi.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long itemId) {
        super("Cart item not found with item id: " + itemId);
    }
}

