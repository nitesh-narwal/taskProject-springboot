package com.example.shopapi.service;

import com.example.shopapi.exception.CartItemNotFoundException;
import com.example.shopapi.exception.ItemNotFoundException;
import com.example.shopapi.model.CartItem;
import com.example.shopapi.model.Item;
import com.example.shopapi.repository.CartRepository;
import com.example.shopapi.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;

    public CartService(CartRepository cartRepository, ItemRepository itemRepository) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
    }

    public CartItem addToCart(CartItem cartItem) {
        // Verify item exists
        if (!itemRepository.existsById(cartItem.getItemId())) {
            throw new ItemNotFoundException(cartItem.getItemId());
        }

        // If item already in cart, add to existing quantity
        if (cartRepository.existsByItemId(cartItem.getItemId())) {
            CartItem existing = cartRepository.findByItemId(cartItem.getItemId()).get();
            existing.setQuantity(existing.getQuantity() + cartItem.getQuantity());
            return cartRepository.save(existing);
        }

        return cartRepository.save(cartItem);
    }

    public CartItem updateCartItemQuantity(Long itemId, Integer quantity) {
        CartItem cartItem = cartRepository.findByItemId(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
        cartItem.setQuantity(quantity);
        return cartRepository.save(cartItem);
    }

    public void removeFromCart(Long itemId) {
        if (!cartRepository.existsByItemId(itemId)) {
            throw new CartItemNotFoundException(itemId);
        }
        cartRepository.deleteByItemId(itemId);
    }

    public void clearCart() {
        cartRepository.clear();
    }

    public List<CartItem> getAllCartItems() {
        return cartRepository.findAll();
    }

    public Double calculateTotalPrice() {
        List<CartItem> cartItems = cartRepository.findAll();
        double total = 0.0;
        for (CartItem cartItem : cartItems) {
            Item item = itemRepository.findById(cartItem.getItemId()).orElse(null);
            if (item != null) {
                total += item.getPrice() * cartItem.getQuantity();
            }
        }
        return Math.round(total * 100.0) / 100.0; // Round to 2 decimal places
    }
}

