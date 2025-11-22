package com.nickfallico.financialriskmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReviewRequestDTO {

    @NotNull(message = "Review decision is required")
    private ReviewDecision decision;

    @NotBlank(message = "Reviewer ID is required")
    private String reviewerId;

    private String notes;

    public enum ReviewDecision {
        APPROVE,    // Clear the transaction, mark as legitimate
        REJECT,     // Confirm as fraud, block permanently
        ESCALATE    // Needs further investigation
    }
}