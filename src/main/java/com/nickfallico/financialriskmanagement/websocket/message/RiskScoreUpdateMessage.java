package com.nickfallico.financialriskmanagement.websocket.message;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * WebSocket message for risk score updates.
 * Covers HighRiskUserIdentified and UserProfileUpdated events.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RiskScoreUpdateMessage extends DashboardMessage {

    // User identification
    private String userId;

    // Risk score changes (for USER_PROFILE_UPDATED)
    private Double previousRiskScore;
    private Double newRiskScore;
    private String updateReason;

    // High risk details (for HIGH_RISK_USER_IDENTIFIED)
    private Double overallRiskScore;
    private Double riskThreshold;
    private List<String> riskFactors;
    private String alertSeverity;      // WARNING, URGENT, CRITICAL
    private String recommendedAction;  // MONITOR, REVIEW, SUSPEND
}
