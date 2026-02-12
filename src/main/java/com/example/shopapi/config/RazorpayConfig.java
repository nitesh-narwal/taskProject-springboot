package com.example.shopapi.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Razorpay Configuration
 * 
 * This configuration class sets up the Razorpay client for payment processing.
 * Razorpay is used instead of Stripe for payment gateway integration.
 * 
 * WHY RAZORPAY:
 * - Popular payment gateway in India
 * - Supports UPI, Cards, Net Banking, Wallets
 * - Easy integration with webhook support
 * - Supports INR and international currencies
 */
@Configuration
@Getter
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    @Value("${razorpay.company-name:Shop API}")
    private String companyName;

    @Value("${razorpay.success-url}")
    private String successUrl;

    @Value("${razorpay.cancel-url}")
    private String cancelUrl;

    /**
     * Creates a RazorpayClient bean for interacting with Razorpay API.
     * 
     * WHY THIS IS NEEDED:
     * - RazorpayClient is the main class to interact with Razorpay APIs
     * - It requires Key ID and Key Secret for authentication
     * - Making it a Bean allows dependency injection throughout the app
     */
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}

