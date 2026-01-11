package com.project.recommendation_service.exception;

public class InteractionNotFoundException extends RuntimeException{
    public InteractionNotFoundException(String message) {
        super(message);
    }
}
