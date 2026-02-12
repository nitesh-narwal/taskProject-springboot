package com.example.shopapi.service;
import com.example.shopapi.config.RazorpayConfig;
import com.example.shopapi.document.Order;
import com.example.shopapi.dto.payment.PaymentRequest;
import com.example.shopapi.dto.payment.PaymentResponse;
import com.example.shopapi.dto.payment.PaymentVerificationRequest;
import com.example.shopapi.entity.PaymentTransaction;
import com.example.shopapi.entity.User;
import com.example.shopapi.exception.PaymentFailedException;
import com.example.shopapi.exception.ResourceNotFoundException;
import com.example.shopapi.repository.jpa.PaymentTransactionRepository;
import com.example.shopapi.repository.jpa.UserRepository;
import com.example.shopapi.repository.mongo.OrderRepository;
import com.example.shopapi.security.UserPrincipal;
import com.example.shopapi.util.EmailService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Formatter;
/**
 * PaymentService - Handles all Razorpay payment operations
 * 
 * WHY RAZORPAY:
 * - Razorpay is a popular Indian payment gateway supporting UPI, Cards, Net Banking
 * - Unlike Stripe's redirect-based checkout, Razorpay uses a client-side SDK
 * - The flow is: Create Order -> Client opens Razorpay modal -> Verify Payment
 * 
 * PAYMENT FLOW:
 * 1. Frontend calls createPaymentOrder() to get Razorpay order_id
 * 2. Frontend opens Razorpay checkout modal with the order_id
 * 3. User completes payment in Razorpay modal
 * 4. Frontend receives payment response and calls verifyPayment()
 * 5. Backend verifies signature and marks order as paid
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    /**
     * Creates a Razorpay order for payment
     * 
     * WHY THIS METHOD:
     * - Razorpay requires creating an "order" first before payment
     * - The order_id is used by frontend to open checkout modal
     * - Amount is in paise (smallest currency unit) - INR 100 = 10000 paise
     */
    @Transactional
    public PaymentResponse createPaymentOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        // Verify order is in payable state
        if (order.getStatus() != Order.OrderStatus.CREATED && order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Order is not in a payable state. Current status: " + order.getStatus());
        }
        Long userId = getCurrentUserId();
        if (!order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }
        try {
            // Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            // Amount in paise (smallest currency unit) - multiply by 100
            orderRequest.put("amount", order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", razorpayConfig.getCurrency());
            orderRequest.put("receipt", "order_" + orderId);
            // Add notes for reference
            JSONObject notes = new JSONObject();
            notes.put("orderId", orderId);
            notes.put("userId", userId.toString());
            orderRequest.put("notes", notes);
            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");
            // Update our order with Razorpay order ID
            order.setPaymentSessionId(razorpayOrderId);
            order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
            orderRepository.save(order);
            // Create payment transaction record
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .gatewaySessionId(razorpayOrderId)
                    .amount(order.getTotalAmount())
                    .currency(razorpayConfig.getCurrency())
                    .status(PaymentTransaction.PaymentStatus.PENDING)
                    .gateway("RAZORPAY")
                    .build();
            paymentTransactionRepository.save(transaction);
            auditService.logAction("PAYMENT_ORDER_CREATED", "PAYMENT", razorpayOrderId, 
                    "Razorpay order created for order " + orderId);
            log.info("Razorpay order created: {} for order {}", razorpayOrderId, orderId);
            // Return response with details needed by frontend
            return PaymentResponse.builder()
                    .orderId(orderId)
                    .razorpayOrderId(razorpayOrderId)
                    .razorpayKeyId(razorpayConfig.getKeyId())
                    .amount(order.getTotalAmount())
                    .currency(razorpayConfig.getCurrency())
                    .companyName(razorpayConfig.getCompanyName())
                    .status("PENDING")
                    .build();
        } catch (RazorpayException e) {
            log.error("Razorpay error creating payment order: {}", e.getMessage());
            auditService.logFailedAction("PAYMENT_ORDER_FAILED", "PAYMENT", orderId, 
                    "Failed to create Razorpay order", e.getMessage());
            throw new PaymentFailedException("Failed to create payment order: " + e.getMessage());
        }
    }
    /**
     * Verifies payment signature and confirms payment
     * 
     * WHY THIS METHOD:
     * - After user completes payment, Razorpay returns signature to frontend
     * - We must verify the signature to ensure payment is authentic
     * - Signature = HMAC-SHA256(razorpay_order_id + "|" + razorpay_payment_id, secret)
     */
    @Transactional
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        try {
            // Verify signature
            String generatedSignature = generateSignature(
                    request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                    razorpayConfig.getKeySecret()
            );
            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                log.error("Payment signature verification failed for order: {}", request.getRazorpayOrderId());
                throw new PaymentFailedException("Payment signature verification failed");
            }
            // Find the payment transaction
            PaymentTransaction transaction = paymentTransactionRepository
                    .findByGatewaySessionId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", "razorpayOrderId", request.getRazorpayOrderId()));
            // Update transaction
            transaction.setGatewayPaymentId(request.getRazorpayPaymentId());
            transaction.setStatus(PaymentTransaction.PaymentStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);
            // Mark order as paid
            orderService.markOrderAsPaid(transaction.getOrderId(), request.getRazorpayPaymentId(), "razorpay");
            // Send confirmation email
            User user = userRepository.findById(transaction.getUserId()).orElse(null);
            if (user != null) {
                Order order = orderRepository.findById(transaction.getOrderId()).orElse(null);
                if (order != null) {
                    emailService.sendPaymentSuccessEmail(
                            user.getEmail(), 
                            user.getFullName(), 
                            transaction.getOrderId(), 
                            order.getTotalAmount().toString()
                    );
                }
            }
            auditService.logAction("PAYMENT_VERIFIED", "PAYMENT", request.getRazorpayPaymentId(),
                    "Payment verified for order " + transaction.getOrderId());
            log.info("Payment verified successfully for order: {}", transaction.getOrderId());
            return PaymentResponse.builder()
                    .orderId(transaction.getOrderId())
                    .razorpayOrderId(request.getRazorpayOrderId())
                    .razorpayPaymentId(request.getRazorpayPaymentId())
                    .status("COMPLETED")
                    .message("Payment successful")
                    .build();
        } catch (Exception e) {
            log.error("Payment verification failed: {}", e.getMessage());
            throw new PaymentFailedException("Payment verification failed: " + e.getMessage());
        }
    }
    /**
     * Handles Razorpay webhook events
     * 
     * WHY WEBHOOK:
     * - Webhooks provide server-to-server confirmation
     * - Handles cases where frontend verification fails
     * - More reliable than client-side verification alone
     */
    @Transactional
    public void handleWebhook(String payload, String signature) {
        try {
            // Verify webhook signature
            String expectedSignature = generateSignature(payload, razorpayConfig.getWebhookSecret());
            if (!expectedSignature.equals(signature)) {
                log.error("Webhook signature verification failed");
                throw new IllegalArgumentException("Invalid webhook signature");
            }
            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("event");
            log.info("Received Razorpay webhook event: {}", event);
            switch (event) {
                case "payment.captured" -> handlePaymentCaptured(webhookData);
                case "payment.failed" -> handlePaymentFailed(webhookData);
                case "order.paid" -> handleOrderPaid(webhookData);
                default -> log.info("Unhandled webhook event: {}", event);
            }
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage());
            throw new RuntimeException("Webhook processing failed", e);
        }
    }
    private void handlePaymentCaptured(JSONObject webhookData) {
        JSONObject paymentEntity = webhookData.getJSONObject("payload")
                .getJSONObject("payment").getJSONObject("entity");
        String paymentId = paymentEntity.getString("id");
        String orderId = paymentEntity.getString("order_id");
        log.info("Payment captured - Payment ID: {}, Order ID: {}", paymentId, orderId);
        PaymentTransaction transaction = paymentTransactionRepository
                .findByGatewaySessionId(orderId).orElse(null);
        if (transaction != null && transaction.getStatus() == PaymentTransaction.PaymentStatus.PENDING) {
            transaction.setGatewayPaymentId(paymentId);
            transaction.setStatus(PaymentTransaction.PaymentStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);
            orderService.markOrderAsPaid(transaction.getOrderId(), paymentId, "razorpay");
            auditService.logAction("PAYMENT_CAPTURED_WEBHOOK", "PAYMENT", paymentId,
                    "Payment captured via webhook for order " + transaction.getOrderId());
        }
    }
    private void handlePaymentFailed(JSONObject webhookData) {
        JSONObject paymentEntity = webhookData.getJSONObject("payload")
                .getJSONObject("payment").getJSONObject("entity");
        String paymentId = paymentEntity.getString("id");
        String orderId = paymentEntity.getString("order_id");
        String errorDescription = paymentEntity.optString("error_description", "Payment failed");
        log.error("Payment failed - Payment ID: {}, Order ID: {}, Reason: {}", paymentId, orderId, errorDescription);
        PaymentTransaction transaction = paymentTransactionRepository
                .findByGatewaySessionId(orderId).orElse(null);
        if (transaction != null) {
            transaction.setStatus(PaymentTransaction.PaymentStatus.FAILED);
            transaction.setFailureReason(errorDescription);
            paymentTransactionRepository.save(transaction);
            orderService.markOrderAsFailed(transaction.getOrderId(), errorDescription);
            // Send failure email
            User user = userRepository.findById(transaction.getUserId()).orElse(null);
            if (user != null) {
                emailService.sendPaymentFailedEmail(user.getEmail(), user.getFullName(), 
                        transaction.getOrderId(), errorDescription);
            }
            auditService.logFailedAction("PAYMENT_FAILED_WEBHOOK", "PAYMENT", paymentId,
                    "Payment failed via webhook", errorDescription);
        }
    }
    private void handleOrderPaid(JSONObject webhookData) {
        JSONObject orderEntity = webhookData.getJSONObject("payload")
                .getJSONObject("order").getJSONObject("entity");
        String razorpayOrderId = orderEntity.getString("id");
        log.info("Order paid webhook received for Razorpay order: {}", razorpayOrderId);
    }
    /**
     * Generates HMAC-SHA256 signature
     * 
     * WHY THIS METHOD:
     * - Razorpay uses HMAC-SHA256 for signature verification
     * - Ensures payment data hasn't been tampered with
     */
    private String generateSignature(String data, String secret) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes());
        return bytesToHex(hash);
    }
    private String bytesToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
    private Long getCurrentUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}
