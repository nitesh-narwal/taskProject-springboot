package com.example.shopapi.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
    public static UserAlreadyExistsException forUsername(String username) {
        return new UserAlreadyExistsException("User with username '" + username + "' already exists");
    }
    public static UserAlreadyExistsException forEmail(String email) {
        return new UserAlreadyExistsException("User with email '" + email + "' already exists");
    }
}
