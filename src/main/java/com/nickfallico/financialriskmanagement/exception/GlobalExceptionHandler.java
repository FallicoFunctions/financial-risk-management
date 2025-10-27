package com.nickfallico.financialriskmanagement.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.AllArgsConstructor;
import lombok.Getter;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FraudDetectionException.class)
    public ResponseEntity<ErrorResponse> handleFraudDetectionException(FraudDetectionException ex) {
        logger.error("Fraud Detection Exception: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "FRAUD_DETECTED",
            ex.getMessage(),
            Instant.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RiskManagementException.class)
    public ResponseEntity<ErrorResponse> handleRiskManagementException(RiskManagementException ex) {
        logger.error("Risk Management Exception: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            Instant.now()
        );
        
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            Instant.now(),
            errors
        );

        logger.error("Validation Error: {}", errors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransactionValidationException.class)
    public ResponseEntity<ErrorResponse> handleTransactionValidationException(TransactionValidationException ex) {
        logger.error("Transaction Validation Error: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            Instant.now()
        );
        
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(RiskAssessmentException.class)
    public ResponseEntity<ErrorResponse> handleRiskAssessmentException(RiskAssessmentException ex) {
        logger.error("Risk Assessment Error: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            Instant.now()
        );
        
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private Instant timestamp;
        private List<String> details;

        // Constructor for errors without detailed list
        public ErrorResponse(String errorCode, String message, Instant timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.timestamp = timestamp;
            this.details = Collections.emptyList();
        }
    }
}