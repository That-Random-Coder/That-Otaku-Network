package com.project.auth_service.exception.customException;

public class ExpireOrWrongRefreshTokenException extends RuntimeException{
    public ExpireOrWrongRefreshTokenException(String message) {
        super(message);
    }
}
