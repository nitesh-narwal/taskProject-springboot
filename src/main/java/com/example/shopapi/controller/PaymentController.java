package com.example.shopapi.controller;
import com.example.shopapi.dto.common.ApiResponse;
import com.example.shopapi.dto.payment.PaymentResponse;
import com.example.shopapi.dto.payment.PaymentVerificationRequest;
import com.example.shopapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 * PaymentController - REST endpoints for Razorpay payment operations
 * 
 * RAZORPAY PAYMENT FLOW:
 * 1. POST /api/payment/create-order/{orderId} - Create Razorpay order
 * 2. Frontend opens Razorpay checkout modal with returned order_id
 * 3. User completes payment in Razorpay modal
 * 4. POST /api/payment/verify - Verify payment signature
 * 5. Webhook /api/webhook/razorpay - Server-to-server confirmation (backup)
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Razorpay payment processing endpoints")
public class PaymentController {
    private final PaymentService paymentService;
    /**
     * Create Razorpay order for payment
     * 
     * WHY THIS ENDPOINT:
     * - Razorpay requires creating an "order" before accepting payment
     * - Returns order_id and key_id needed by frontend to open checkout modal
     *
     * SUPPORTED PATHS:
     * - POST /api/payment/create-order/{orderId} (original)
     * - POST /api/payment/create-session/{orderId} (alias for frontend compatibility)
     */
    @PostMapping({"/create-order/{orderId}", "/create-session/{orderId}"})
    @PreAuthorize("hasAnyRole('USER', 'WORKER', 'ADMIN')")
    @Operation(summary = "Create a Razorpay payment order", 
               description = "Creates a Razorpay order for the given order ID. Returns details needed for frontend checkout.")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPaymentOrder(@PathVariable String orderId) {
        PaymentResponse response = paymentService.createPaymentOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment order created successfully", response));
    }
    /**
     * Verify payment after user completes checkout
     * 
     * WHY THIS ENDPOINT:
     * - After payment in Razorpay modal, frontend receives signature
     * - This endpoint verifies the signature to confirm payment authenticity
     * - Only after verification, order is marked as PAID
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('USER', 'WORKER', 'ADMIN')")
    @Operation(summary = "Verify Razorpay payment", 
               description = "Verifies the payment signature after successful checkout. Marks order as paid if valid.")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {
        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", response));
    }
}
