package com.project.user_service.exception.customException;

public class UsernameOrEmailAlreadyExistsException extends RuntimeException{
    public UsernameOrEmailAlreadyExistsException(String message) {
        super(message);
    }
}
