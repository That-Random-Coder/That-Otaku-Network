package com.project.content_service.exception;

import com.project.content_service.exception.customException.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    /*
     * =========================
     * Validation Exceptions
     * =========================
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ApiError>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ApiError> errors = new ArrayList<>();

        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(ApiError.builder()
                    .keyError(error.getField())
                    .valueError(error.getDefaultMessage())
                    .timeStamp(LocalDateTime.now())
                    .build());
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<List<ApiError>> hendleIllegalArgumentException(IllegalArgumentException exception) {
        return buildError(
                exception.getMessage(),
                "null value",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeException.class)
    public ResponseEntity<List<ApiError>> handleHttpMediaTypeException(
            HttpMediaTypeException exception) {
        return buildError(
                exception.getTypeMessageCode(),
                exception.getLocalizedMessage(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<List<ApiError>> handleMultipartException(
            MultipartException exception) {
        return buildError(
                exception.getMessage(),
                "Multipart request required",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ImageUploadFailedException.class)
    public ResponseEntity<List<ApiError>> handleImageUploadFailedException(
            ImageUploadFailedException exception) {
        return buildError(
                exception.getMessage(),
                "Image Upload Failed",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<List<ApiError>> handleNullPointerException(
            NullPointerException exception) {
        return buildError(
                exception.getMessage(),
                "Can't be Null",
                HttpStatus.BAD_REQUEST);
    }
    /*
     * =========================
     * Authentication & Authorization
     * =========================
     */

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<List<ApiError>> handleBadCredentialsException(BadCredentialsException exception) {
        return buildError(
                exception.getMessage(),
                "Bad Credential",
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<List<ApiError>> handleDisabledException(DisabledException exception) {
        return buildError(
                exception.getMessage(),
                "Disable User",
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<List<ApiError>> handleAuthenticationException(AuthenticationException exception) {
        return buildError(
                exception.getMessage(),
                "Authentication Failed",
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleAuthenticationCredentialsNotFoundException(
            AuthenticationCredentialsNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "Authentication Credentials Not Found",
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<List<ApiError>> handleAccessDenied(AccessDeniedException exception) {
        return buildError(
                exception.getMessage(),
                "Access Denied",
                HttpStatus.FORBIDDEN);
    }

    /*
     * =========================
     * Business / Custom Exceptions
     * =========================
     */

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleUserNotFoundException(UserNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "User Not Found",
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleGroupNotFoundException(GroupNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "Group Not Found",
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ContentNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleContentNotFoundException(ContentNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "Content Not Found",
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoFollowingException.class)
    public ResponseEntity<List<ApiError>> handleNoFollowingException(NoFollowingException exception) {
        return buildError(
                exception.getMessage(),
                "No Following Exception",
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UsernameOrEmailAlreadyExistsException.class)
    public ResponseEntity<List<ApiError>> handleUserAlreadyExists(UsernameOrEmailAlreadyExistsException exception) {
        return buildError(
                exception.getMessage(),
                "Username or Email Already Exists",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<List<ApiError>> handleUserAlreadyExistsException(UserAlreadyExistsException exception) {
        return buildError(
                exception.getMessage(),
                "User Already Exists",
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExpireOrWrongRefreshTokenException.class)
    public ResponseEntity<List<ApiError>> handleExpireOrWrongRefreshTokenException(
            ExpireOrWrongRefreshTokenException exception) {
        return buildError(
                exception.getMessage(),
                "Expire Or Wrong Refresh Token",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(VerificationCodeExpiredException.class)
    public ResponseEntity<List<ApiError>> handleVerificationCodeExpiredException(
            VerificationCodeExpiredException exception) {
        return buildError(
                exception.getMessage(),
                "Verification Code Expired",
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailAlreadySendException.class)
    public ResponseEntity<List<ApiError>> handleEmailAlreadySendException(
            EmailAlreadySendException exception) {
        return buildError(
                exception.getMessage(),
                "Email Already Send",
                HttpStatus.NOT_FOUND);
    }

    /*
     * =========================
     * Database Exceptions
     * =========================
     */

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<List<ApiError>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        Throwable rootCause = exception.getRootCause();
        String message = rootCause != null ? rootCause.getMessage() : exception.getMessage();

        if (message.contains("email")) {
            message = "email";
        } else if (message.contains("username")) {
            message = "username";
        }

        return buildError(
                message,
                "Already Exists",
                HttpStatus.BAD_REQUEST);
    }

    /*
     * =========================
     * Generic Exception
     * =========================
     */

    // @ExceptionHandler(RuntimeException.class)
    // public ResponseEntity<List<ApiError>> handleGenericException(RuntimeException
    // exception) {
    // return buildError(
    // exception.getMessage(),
    // "Error Occured",
    // HttpStatus.INTERNAL_SERVER_ERROR);
    // }

    /*
     * =========================
     * Helper Method
     * =========================
     */

    private ResponseEntity<List<ApiError>> buildError(
            String keyError,
            String valueError,
            HttpStatus status) {

        List<ApiError> error = List.of(
                ApiError.builder()
                        .keyError(keyError)
                        .valueError(valueError)
                        .timeStamp(LocalDateTime.now())
                        .build());

        return new ResponseEntity<>(error, status);
    }
}
