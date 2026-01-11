package com.project.content_service.exception.customException;

public class UsernameOrEmailAlreadyExistsException extends RuntimeException{
    public UsernameOrEmailAlreadyExistsException(String message) {
        super(message);
    }
}
