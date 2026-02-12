package com.example.shopapi.dto.payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
/**
 * PaymentResponse - Response DTO for Razorpay payment operations
 * 
 * WHY THESE FIELDS:
 * - razorpayOrderId: Required by frontend to open Razorpay checkout modal
 * - razorpayKeyId: Public key needed by Razorpay JS SDK
 * - razorpayPaymentId: Returned after successful payment verification
 * - amount & currency: Display to user before payment
 * - companyName: Shown in Razorpay checkout modal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    // Our internal order ID
    private String orderId;
    // Razorpay order ID (starts with "order_")
    private String razorpayOrderId;
    // Razorpay payment ID (after payment completion, starts with "pay_")
    private String razorpayPaymentId;
    // Razorpay Key ID (public key for frontend)
    private String razorpayKeyId;
    // Payment amount
    private BigDecimal amount;
    // Currency (INR, USD, etc.)
    private String currency;
    // Company name displayed in checkout
    private String companyName;
    // Payment status (PENDING, COMPLETED, FAILED)
    private String status;
    // Success/error message
    private String message;
}
