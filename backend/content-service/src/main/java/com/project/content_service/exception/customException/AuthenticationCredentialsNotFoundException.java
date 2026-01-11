package com.project.content_service.exception.customException;

public class AuthenticationCredentialsNotFoundException extends RuntimeException{
    public AuthenticationCredentialsNotFoundException(String message) {
        super(message);
    }
}
