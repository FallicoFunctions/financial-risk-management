package com.nickfallico.financialriskmanagement.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

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

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private Instant timestamp;
    }
}