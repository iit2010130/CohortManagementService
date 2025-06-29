package com.cohortmgmt.service.rule;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;

/**
 * Rule that classifies customers based on mid-range spending and user type.
 * - If SPEND > 3000 and < 5000, then cohort type is NORMAL
 * - If SPEND > 3000 and < 5000 AND customer is PAID, then cohort type is PREMIUM
 */
public class MidSpendRule implements CohortRule {
    
    private static final String RULE_NAME = "MidSpend";
    private static final double MIN_THRESHOLD = 3000.0;
    private static final double MAX_THRESHOLD = 5000.0;
    
    private final CohortType cohortType;
    
    /**
     * Creates a new MidSpendRule for NORMAL customers.
     */
    public MidSpendRule() {
        this(CohortType.NORMAL);
    }
    
    /**
     * Creates a new MidSpendRule with the specified cohort type.
     *
     * @param cohortType The cohort type to assign
     */
    public MidSpendRule(CohortType cohortType) {
        this.cohortType = cohortType;
    }
    
    @Override
    public String getName() {
        return RULE_NAME;
    }
    
    @Override
    public CohortType getCohortType() {
        return cohortType;
    }
    
    @Override
    public boolean evaluate(Customer customer) {
        if (customer == null || customer.getDailySpend() == null) {
            return false;
        }
        
        double dailySpend = customer.getDailySpend();
        
        // Check if spend is in the mid range (> 3000 and < 5000)
        boolean isInMidRange = dailySpend > MIN_THRESHOLD && dailySpend < MAX_THRESHOLD;
        
        if (!isInMidRange) {
            return false;
        }
        
        // For PREMIUM cohort type, also check if the customer is PAID
        if (cohortType == CohortType.PREMIUM) {
            return customer.getUserType() == UserType.PAID;
        }
        
        // For NORMAL cohort type, we don't care about the user type
        return true;
    }
    
    /**
     * Gets the minimum threshold for daily spend.
     *
     * @return The minimum threshold
     */
    public double getMinThreshold() {
        return MIN_THRESHOLD;
    }
    
    /**
     * Gets the maximum threshold for daily spend.
     *
     * @return The maximum threshold
     */
    public double getMaxThreshold() {
        return MAX_THRESHOLD;
    }
}
