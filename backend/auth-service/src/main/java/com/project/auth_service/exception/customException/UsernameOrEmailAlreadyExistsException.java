package com.project.auth_service.exception.customException;

public class UsernameOrEmailAlreadyExistsException extends RuntimeException{
    public UsernameOrEmailAlreadyExistsException(String message) {
        super(message);
    }
}
