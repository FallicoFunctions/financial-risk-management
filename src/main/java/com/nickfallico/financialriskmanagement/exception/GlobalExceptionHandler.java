package com.nickfallico.financialriskmanagement.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
        String errorId = generateErrorId();
        logger.error("Fraud Detection Exception [ErrorID: {}]: {}", errorId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "FRAUD_DETECTED",
            "Potential fraudulent activity detected",
            errorId,
            Instant.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RiskManagementException.class)
    public ResponseEntity<ErrorResponse> handleRiskManagementException(RiskManagementException ex) {
        String errorId = generateErrorId();
        logger.error("Risk Management Exception [ErrorID: {}]: {}", errorId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            errorId,
            Instant.now()
        );
        
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorId = generateErrorId();
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        logger.error("Validation Error [ErrorID: {}]: {}", errorId, errors);

        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            errorId,
            Instant.now(),
            errors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransactionValidationException.class)
    public ResponseEntity<ErrorResponse> handleTransactionValidationException(TransactionValidationException ex) {
        String errorId = generateErrorId();
        logger.error("Transaction Validation Error [ErrorID: {}]: {}", errorId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            errorId,
            Instant.now()
        );
        
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(RiskAssessmentException.class)
    public ResponseEntity<ErrorResponse> handleRiskAssessmentException(RiskAssessmentException ex) {
        String errorId = generateErrorId();
        logger.error("Risk Assessment Error [ErrorID: {}]: {}", errorId, ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            errorId,
            Instant.now()
        );
        
        return new ResponseEntity<>(error, ex.getStatus());
    }

    // Fallback handler for any unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        String errorId = generateErrorId();
        logger.error("Unexpected Error [ErrorID: {}]: Unhandled exception", errorId, ex);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            errorId,
            Instant.now()
        );
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Generate a unique error ID for traceability
    private String generateErrorId() {
        return UUID.randomUUID().toString();
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private String errorId;
        private Instant timestamp;
        private List<String> details;

        // Constructor for errors without detailed list
        public ErrorResponse(String errorCode, String message, String errorId, Instant timestamp) {
            this.errorCode = errorCode;
            this.message = message;
            this.errorId = errorId;
            this.timestamp = timestamp;
            this.details = Collections.emptyList();
        }
    }
}