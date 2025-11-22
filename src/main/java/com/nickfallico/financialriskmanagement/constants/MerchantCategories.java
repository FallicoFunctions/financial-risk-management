package com.nickfallico.financialriskmanagement.constants;

import java.util.Arrays;
import java.util.List;

public final class MerchantCategories {
    
    // Single source of truth for all merchant categories
    public static final List<String> VALID_CATEGORIES = Arrays.asList(
        "GROCERIES", 
        "ELECTRONICS", 
        "TRAVEL", 
        "DINING",
        "ENTERTAINMENT", 
        "UTILITIES", 
        "TRANSPORTATION",
        "GAMBLING", 
        "CRYPTO", 
        "ONLINE_SHOPPING",
        "ADULT_ENTERTAINMENT",
        "RETAIL"
    );
    
    public static final List<String> HIGH_RISK_CATEGORIES = Arrays.asList(
        "GAMBLING", 
        "CRYPTO", 
        "ADULT_ENTERTAINMENT"
    );
    
    private MerchantCategories() {
        // Prevent instantiation
    }
}