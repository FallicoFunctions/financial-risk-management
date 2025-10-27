package com.nickfallico.financialriskmanagement.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RiskManagementException.class)
    public ResponseEntity<ErrorResponse> handleRiskManagementException(RiskManagementException ex) {
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