package com.example.shopapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public static InvalidTokenException verificationToken() {
        return new InvalidTokenException("Invalid email verification token");
    }

    public static InvalidTokenException passwordResetToken() {
        return new InvalidTokenException("Invalid password reset token");
    }

    public static InvalidTokenException refreshToken() {
        return new InvalidTokenException("Invalid refresh token");
    }

    public static InvalidTokenException accessToken() {
        return new InvalidTokenException("Invalid access token");
    }
}

