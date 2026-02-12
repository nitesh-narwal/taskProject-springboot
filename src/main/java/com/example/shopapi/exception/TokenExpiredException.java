package com.example.shopapi.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }

    public static TokenExpiredException verificationToken() {
        return new TokenExpiredException("Email verification token has expired");
    }

    public static TokenExpiredException passwordResetToken() {
        return new TokenExpiredException("Password reset token has expired");
    }

    public static TokenExpiredException refreshToken() {
        return new TokenExpiredException("Refresh token has expired");
    }
}
