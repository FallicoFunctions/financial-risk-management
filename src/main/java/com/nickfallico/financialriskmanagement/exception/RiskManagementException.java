package com.nickfallico.financialriskmanagement.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RiskManagementException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public RiskManagementException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    // Common risk management error types
    public static class TransactionRiskException extends RiskManagementException {
        public TransactionRiskException(String message) {
            super(message, HttpStatus.FORBIDDEN, "RISK_THRESHOLD_EXCEEDED");
        }
    }

    public static class FraudDetectedException extends RiskManagementException {
        public FraudDetectedException(String message) {
            super(message, HttpStatus.UNAUTHORIZED, "POTENTIAL_FRAUD_DETECTED");
        }
    }
}