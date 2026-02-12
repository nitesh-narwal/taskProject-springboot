package com.example.shopapi.controller;
import com.example.shopapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/**
 * WebhookController - Handles Razorpay webhook events
 * 
 * WHY WEBHOOKS:
 * - Server-to-server communication for payment confirmation
 * - More reliable than client-side verification alone
 * - Handles edge cases (user closes browser after payment, network issues)
 * 
 * RAZORPAY WEBHOOK EVENTS:
 * - payment.captured: Payment successfully captured
 * - payment.failed: Payment failed
 * - order.paid: Order marked as paid
 * 
 * SETUP IN RAZORPAY DASHBOARD:
 * 1. Go to Settings -> Webhooks
 * 2. Add webhook URL: https://yourdomain.com/api/webhook/razorpay
 * 3. Select events: payment.captured, payment.failed, order.paid
 * 4. Copy webhook secret to application.yml
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Razorpay webhook endpoints")
public class WebhookController {
    private final PaymentService paymentService;
    /**
     * Handle Razorpay webhook events
     * 
     * WHY X-Razorpay-Signature HEADER:
     * - Razorpay signs webhook payload with your webhook secret
     * - We verify signature to ensure webhook is from Razorpay
     * - Prevents malicious actors from faking payment confirmations
     */
    @PostMapping("/razorpay")
    @Operation(summary = "Handle Razorpay webhook events",
               description = "Receives and processes webhook events from Razorpay for payment confirmations")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        log.info("Received Razorpay webhook");
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("Webhook processed successfully");
    }
}
