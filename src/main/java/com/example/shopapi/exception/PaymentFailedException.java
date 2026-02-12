package com.example.shopapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentFailedException extends RuntimeException {

    private final String orderId;
    private final String paymentId;

    public PaymentFailedException(String message) {
        super(message);
        this.orderId = null;
        this.paymentId = null;
    }

    public PaymentFailedException(String message, String orderId, String paymentId) {
        super(message);
        this.orderId = orderId;
        this.paymentId = paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}

