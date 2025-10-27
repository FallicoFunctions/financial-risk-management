package com.nickfallico.financialriskmanagement.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

public class TransactionValidationException extends RuntimeException {
    @Getter
    private final HttpStatus status;
    @Getter
    private final String errorCode;

    public TransactionValidationException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "TRANSACTION_VALIDATION_ERROR";
    }
}