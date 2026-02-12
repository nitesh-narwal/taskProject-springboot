package com.example.shopapi.service;

import com.example.shopapi.document.Cart;
import com.example.shopapi.document.Product;
import com.example.shopapi.dto.cart.AddToCartRequest;
import com.example.shopapi.dto.cart.CartResponse;
import com.example.shopapi.exception.InsufficientStockException;
import com.example.shopapi.exception.ResourceNotFoundException;
import com.example.shopapi.repository.mongo.CartMongoRepository;
import com.example.shopapi.repository.mongo.ProductRepository;
import com.example.shopapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceNew {

    private final CartMongoRepository cartRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart() {
        Long userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createNewCart(userId));
        return mapToResponse(cart);
    }

    public CartResponse addToCart(AddToCartRequest request) {
        Long userId = getCurrentUserId();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(product.getId(), request.getQuantity(), product.getStockQuantity());
        }

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createNewCart(userId));

        Cart.CartItem cartItem = Cart.CartItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getEffectivePrice())
                .quantity(request.getQuantity())
                .imageUrl(product.getImageUrl())
                .build();

        cart.addItem(cartItem);
        cart = cartRepository.save(cart);
        log.info("Item added to cart for user {}: product {}", userId, request.getProductId());
        return mapToResponse(cart);
    }

    public CartResponse updateCartItem(String productId, int quantity) {
        Long userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getId(), quantity, product.getStockQuantity());
        }

        cart.updateItemQuantity(productId, quantity);
        cart = cartRepository.save(cart);
        return mapToResponse(cart);
    }

    public CartResponse removeFromCart(String productId) {
        Long userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", userId));
        cart.removeItem(productId);
        cart = cartRepository.save(cart);
        log.info("Item removed from cart for user {}: product {}", userId, productId);
        return mapToResponse(cart);
    }

    public void clearCart() {
        Long userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cart.clear();
            cartRepository.save(cart);
            log.info("Cart cleared for user {}", userId);
        }
    }

    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cart.clear();
            cartRepository.save(cart);
        }
    }

    private Cart createNewCart(Long userId) {
        return Cart.builder().userId(userId).build();
    }

    private CartResponse mapToResponse(Cart cart) {
        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(cart.getItems().stream()
                        .map(item -> CartResponse.CartItemDto.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .imageUrl(item.getImageUrl())
                                .subtotal(item.getSubtotal())
                                .build())
                        .collect(Collectors.toList()))
                .totalAmount(cart.getTotalAmount())
                .totalItems(cart.getTotalItems())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private Long getCurrentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getId();
    }
}

