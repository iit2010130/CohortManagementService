package com.cohortmgmt.service.rule;

import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.CohortType;

/**
 * Interface for cohort classification rules.
 */
public interface CohortRule {
    
    /**
     * Gets the name of the rule.
     *
     * @return The rule name
     */
    String getName();
    
    /**
     * Gets the type of cohort this rule classifies customers into.
     *
     * @return The cohort type
     */
    CohortType getCohortType();
    
    /**
     * Evaluates whether a customer should be classified into the cohort based on this rule.
     *
     * @param customer The customer to evaluate
     * @return true if the customer should be in the cohort, false otherwise
     */
    boolean evaluate(Customer customer);
}
