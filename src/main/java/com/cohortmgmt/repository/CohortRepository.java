package com.cohortmgmt.repository;

import com.cohortmgmt.model.CohortType;

import java.util.List;
import java.util.Set;

/**
 * Repository interface for storing and retrieving cohort data.
 * Minimized to support only the required operations:
 * 1. Determine if a given CustomerId is part of a specific cohort type
 * 2. List all cohort types associated with a given CustomerId
 * 3. Retrieve all CustomerIds for a specific cohort type
 */
public interface CohortRepository {
    
    /**
     * Adds a customer to a cohort type (needed for classification).
     *
     * @param cohortType The type of the cohort
     * @param customerId The ID of the customer to add
     * @return true if the customer was added, false otherwise
     */
    boolean addCustomerToCohortType(CohortType cohortType, String customerId);
    
    /**
     * Gets all customer IDs in a cohort type (for query #3).
     *
     * @param cohortType The type of the cohort
     * @return The set of customer IDs in the cohort type
     */
    Set<String> getCustomerIdsByCohortType(CohortType cohortType);
    
    /**
     * Gets all cohort types that contain a specific customer (for query #2).
     *
     * @param customerId The ID of the customer
     * @return The list of cohort types containing the customer
     */
    List<CohortType> findCohortTypesByCustomerId(String customerId);
    
    /**
     * Checks if a customer is in a specific cohort type.
     *
     * @param customerId The ID of the customer
     * @param cohortType The type of cohort
     * @return true if the customer is in the cohort type, false otherwise
     */
    boolean isCustomerInCohortType(String customerId, CohortType cohortType);
}
