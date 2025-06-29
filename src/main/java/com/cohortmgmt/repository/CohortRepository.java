package com.cohortmgmt.repository;

import com.cohortmgmt.model.Cohort;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for storing and retrieving cohort data.
 * Minimized to support only the required operations:
 * 1. Determine if a given CustomerId is part of a specific cohort
 * 2. List all cohorts associated with a given CustomerId
 * 3. Retrieve all CustomerIds for a specific cohort
 */
public interface CohortRepository {
    
    /**
     * Saves a cohort to the repository (needed for initialization).
     *
     * @param cohort The cohort to save
     * @return The saved cohort
     */
    Cohort save(Cohort cohort);
    
    /**
     * Finds a cohort by ID (needed for operations).
     *
     * @param cohortId The ID of the cohort to find
     * @return An Optional containing the cohort if found, or empty if not found
     */
    Optional<Cohort> findById(String cohortId);
    
    /**
     * Adds a customer to a cohort (needed for classification).
     *
     * @param cohortId The ID of the cohort
     * @param customerId The ID of the customer to add
     * @return true if the customer was added, false otherwise
     */
    boolean addCustomerToCohort(String cohortId, String customerId);
    
    /**
     * Gets all customer IDs in a cohort (for query #3).
     *
     * @param cohortId The ID of the cohort
     * @return The set of customer IDs in the cohort
     */
    Set<String> getCustomerIds(String cohortId);
    
    /**
     * Gets all cohorts that contain a specific customer (for query #2).
     *
     * @param customerId The ID of the customer
     * @return The list of cohorts containing the customer
     */
    List<Cohort> findByCustomerId(String customerId);
    
    /**
     * Checks if a cohort exists in the repository (needed for initialization).
     *
     * @param cohortId The ID of the cohort to check
     * @return true if the cohort exists, false otherwise
     */
    boolean existsById(String cohortId);
}
