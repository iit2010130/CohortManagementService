package com.cohortmgmt.model;

/**
 * Enum representing the types of cohorts in the system.
 */
public enum CohortType {
    /**
     * Represents customers identified as fraudulent.
     */
    FRAUD,
    
    /**
     * Represents premium customers.
     */
    PREMIUM,
    
    /**
     * Represents normal customers.
     */
    NORMAL,
    
    /**
     * Represents VIP customers.
     */
    VIP
}
