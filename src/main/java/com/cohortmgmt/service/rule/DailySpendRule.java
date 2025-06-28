package com.cohortmgmt.service.rule;

import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.CohortType;

/**
 * Rule that classifies customers as PREMIUM if their daily spend is greater than a threshold.
 */
public class DailySpendRule implements CohortRule {
    
    private static final String RULE_NAME = "DailySpend";
    private static final CohortType COHORT_TYPE = CohortType.PREMIUM;
    
    private final double threshold;
    
    /**
     * Creates a new DailySpendRule with the default threshold of 5000.
     */
    public DailySpendRule() {
        this(5000.0);
    }
    
    /**
     * Creates a new DailySpendRule with the specified threshold.
     *
     * @param threshold The threshold for daily spend
     */
    public DailySpendRule(double threshold) {
        this.threshold = threshold;
    }
    
    @Override
    public String getName() {
        return RULE_NAME;
    }
    
    @Override
    public CohortType getCohortType() {
        return COHORT_TYPE;
    }
    
    @Override
    public boolean evaluate(Customer customer) {
        if (customer == null || customer.getDailySpend() == null) {
            return false;
        }
        return customer.getDailySpend() > threshold;
    }
    
    /**
     * Gets the threshold for daily spend.
     *
     * @return The threshold
     */
    public double getThreshold() {
        return threshold;
    }
}
