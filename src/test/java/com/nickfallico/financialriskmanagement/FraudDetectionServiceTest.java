package com.nickfallico.financialriskmanagement;

import com.nickfallico.financialriskmanagement.model.Transaction;
import com.nickfallico.financialriskmanagement.model.UserRiskProfile;
import com.nickfallico.financialriskmanagement.service.FraudDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {
    @Mock
    private UserRiskProfile mockProfile;

    @Test
    void testHighAmountTransaction() {
        Transaction highAmountTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(15000))
            .isInternational(false)
            .merchantCategory("ELECTRONICS")
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(20);
        profile.getMerchantCategoryFrequency().put("ELECTRONICS", 5);

        FraudDetectionService service = new FraudDetectionService();
        boolean isPotentialFraud = service.isPotentialFraud(highAmountTransaction, profile);

        assertTrue(isPotentialFraud, "High amount transaction should be flagged as potential fraud");
    }

    @Test
    void testInternationalTransaction() {
        Transaction internationalTransaction = Transaction.builder()
            .amount(BigDecimal.valueOf(5000))
            .isInternational(true)
            .merchantCategory("TRAVEL")
            .build();

        UserRiskProfile profile = new UserRiskProfile();
        profile.setTotalTransactions(5);
        profile.getMerchantCategoryFrequency().put("TRAVEL", 1);

        FraudDetectionService service = new FraudDetectionService();
        boolean isPotentialFraud = service.isPotentialFraud(internationalTransaction, profile);

        assertTrue(isPotentialFraud, "International transaction with low transaction history should be flagged");
    }
}