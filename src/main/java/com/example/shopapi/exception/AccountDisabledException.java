package com.example.shopapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Your account has been disabled. Please contact support.");
    }

    public AccountDisabledException(String message) {
        super(message);
    }
}

