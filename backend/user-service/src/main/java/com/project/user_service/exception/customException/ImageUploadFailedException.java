package com.project.user_service.exception.customException;

public class ImageUploadFailedException extends RuntimeException{
    public ImageUploadFailedException(String message) {
        super(message);
    }
}
