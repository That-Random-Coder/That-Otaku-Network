package com.project.content_service.exception.customException;


public class EmailAlreadySendException extends RuntimeException{
    public EmailAlreadySendException(String message) {
        super(message);
    }
}
