package com.project.auth_service.exception;

import com.project.auth_service.exception.customException.*;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    /* =========================
       Validation Exceptions
       ========================= */

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

    /* =========================
       Authentication & Authorization
       ========================= */

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<List<ApiError>> handleBadCredentialsException(BadCredentialsException exception) {
        return buildError(
                exception.getMessage(),
                "Bad Credential",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<List<ApiError>> handleDisabledException(DisabledException exception) {
        return buildError(
                exception.getMessage(),
                "Disable User",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<List<ApiError>> handleAuthenticationException(AuthenticationException exception) {
        return buildError(
                exception.getMessage(),
                "Authentication Failed",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleAuthenticationCredentialsNotFoundException(
            AuthenticationCredentialsNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "Authentication Credentials Not Found",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<List<ApiError>> handleAccessDenied(AccessDeniedException exception) {
        return buildError(
                exception.getMessage(),
                "Access Denied",
                HttpStatus.FORBIDDEN
        );
    }

    /* =========================
       JWT & Security
       ========================= */

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<List<ApiError>> handleJwtException(JwtException exception) {
        return buildError(
                exception.getMessage(),
                "Invalid JWT Token",
                HttpStatus.UNAUTHORIZED
        );
    }

    /* =========================
       Business / Custom Exceptions
       ========================= */

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleUserNotFoundException(UserNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "User Not Found",
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(UsernameOrEmailAlreadyExistsException.class)
    public ResponseEntity<List<ApiError>> handleUserAlreadyExists(UsernameOrEmailAlreadyExistsException exception) {
        return buildError(
                exception.getMessage(),
                "Username or Email Already Exists",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ExpireOrWrongRefreshTokenException.class)
    public ResponseEntity<List<ApiError>> handleExpireOrWrongRefreshTokenException(
            ExpireOrWrongRefreshTokenException exception) {
        return buildError(
                exception.getMessage(),
                "Expire Or Wrong Refresh Token",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(VerificationCodeExpiredException.class)
    public ResponseEntity<List<ApiError>> handleVerificationCodeExpiredException(
            VerificationCodeExpiredException exception) {
        return buildError(
                exception.getMessage(),
                "Verification Code Expired",
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<List<ApiError>> handleInvalidVerificationCodeException(
            InvalidVerificationCodeException exception) {
        return buildError(
                exception.getMessage(),
                "Invalid Verification Code",
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(EmailAlreadySendException.class)
    public ResponseEntity<List<ApiError>> handleEmailAlreadySendException(
            EmailAlreadySendException exception) {
        return buildError(
                exception.getMessage(),
                "Email Already Send",
                HttpStatus.NOT_FOUND
        );
    }

    /* =========================
       Database Exceptions
       ========================= */

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
                HttpStatus.BAD_REQUEST
        );
    }

    /* =========================
       Generic Exception
       ========================= */

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<List<ApiError>> handleGenericException(RuntimeException exception) {
        return buildError(
                exception.getMessage(),
                "Error Occured",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /* =========================
       Helper Method
       ========================= */

    private ResponseEntity<List<ApiError>> buildError(
            String keyError,
            String valueError,
            HttpStatus status) {

        List<ApiError> error = List.of(
                ApiError.builder()
                        .keyError(keyError)
                        .valueError(valueError)
                        .timeStamp(LocalDateTime.now())
                        .build()
        );

        return new ResponseEntity<>(error, status);
    }
}
