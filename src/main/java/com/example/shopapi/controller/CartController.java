package com.example.shopapi.controller;

import com.example.shopapi.model.CartItem;
import com.example.shopapi.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Legacy Cart Controller - Disabled to avoid conflicts with CartControllerNew.
 * Use CartControllerNew for all cart operations.
 */
// @RestController  // Disabled - using CartControllerNew instead
// @RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<CartItem> addToCart(@Valid @RequestBody CartItem cartItem) {
        CartItem addedItem = cartService.addToCart(cartItem);
        return new ResponseEntity<>(addedItem, HttpStatus.CREATED);
    }

    @PutMapping("/update/{itemId}")
    public ResponseEntity<CartItem> updateCartItem(
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> request) {
        Integer quantity = request.get("quantity");
        if (quantity == null || quantity < 1) {
            return ResponseEntity.badRequest().build();
        }
        CartItem updatedItem = cartService.updateCartItemQuantity(itemId, quantity);
        return ResponseEntity.ok(updatedItem);
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long itemId) {
        cartService.removeFromCart(itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getAllCartItems() {
        List<CartItem> cartItems = cartService.getAllCartItems();
        return ResponseEntity.ok(cartItems);
    }

    @GetMapping("/total")
    public ResponseEntity<Map<String, Double>> getCartTotal() {
        Double total = cartService.calculateTotalPrice();
        Map<String, Double> response = new HashMap<>();
        response.put("total", total);
        return ResponseEntity.ok(response);
    }
}

