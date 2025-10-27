package com.nickfallico.financialriskmanagement.service;

import com.nickfallico.financialriskmanagement.exception.RiskManagementException;
import com.nickfallico.financialriskmanagement.ml.FraudDetectionModel;
import com.nickfallico.financialriskmanagement.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TransactionRiskWorkflow {
    private final RiskAssessmentService riskAssessmentService;
    private final FraudDetectionModel fraudDetectionModel;

    public Mono<Transaction> processTransaction(Transaction transaction) {
        return riskAssessmentService.assessTransactionRisk(transaction)
            .flatMap(riskScore -> {
                if (riskScore.getRiskLevel() == RiskAssessmentService.RiskLevel.VERY_HIGH) {
                    return Mono.error(new RiskManagementException.TransactionRiskException("High-risk transaction detected"));
                }
                
                double fraudProbability = fraudDetectionModel.predictFraudProbability(transaction);
                
                if (fraudProbability > 0.8) {
                    return Mono.error(new RiskManagementException.FraudDetectedException("Potential fraud detected"));
                }
                
                return Mono.just(transaction);
            });
    }
}