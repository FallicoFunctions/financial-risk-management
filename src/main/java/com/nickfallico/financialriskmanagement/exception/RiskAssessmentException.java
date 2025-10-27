package com.nickfallico.financialriskmanagement.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

public class RiskAssessmentException extends RuntimeException {
    @Getter
    private final HttpStatus status;
    @Getter
    private final String errorCode;

    public RiskAssessmentException(String message) {
        super(message);
        this.status = HttpStatus.UNPROCESSABLE_ENTITY;
        this.errorCode = "RISK_ASSESSMENT_ERROR";
    }
}