package com.cohortmgmt.repository;

import com.cohortmgmt.model.Cohort;
import com.cohortmgmt.model.CohortType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for storing and retrieving cohort data.
 */
public interface CohortRepository {
    
    /**
     * Saves a cohort to the repository.
     *
     * @param cohort The cohort to save
     * @return The saved cohort
     */
    Cohort save(Cohort cohort);
    
    /**
     * Finds a cohort by ID.
     *
     * @param cohortId The ID of the cohort to find
     * @return An Optional containing the cohort if found, or empty if not found
     */
    Optional<Cohort> findById(String cohortId);
    
    /**
     * Finds all cohorts in the repository.
     *
     * @return The list of all cohorts
     */
    List<Cohort> findAll();
    
    /**
     * Finds all cohorts of a specific type.
     *
     * @param cohortType The type of cohort to find
     * @return The list of cohorts of the specified type
     */
    List<Cohort> findByType(CohortType cohortType);
    
    /**
     * Adds a customer to a cohort.
     *
     * @param cohortId The ID of the cohort
     * @param customerId The ID of the customer to add
     * @return true if the customer was added, false otherwise
     */
    boolean addCustomerToCohort(String cohortId, String customerId);
    
    /**
     * Removes a customer from a cohort.
     *
     * @param cohortId The ID of the cohort
     * @param customerId The ID of the customer to remove
     * @return true if the customer was removed, false otherwise
     */
    boolean removeCustomerFromCohort(String cohortId, String customerId);
    
    /**
     * Gets all customer IDs in a cohort.
     *
     * @param cohortId The ID of the cohort
     * @return The set of customer IDs in the cohort
     */
    Set<String> getCustomerIds(String cohortId);
    
    /**
     * Gets all cohorts that contain a specific customer.
     *
     * @param customerId The ID of the customer
     * @return The list of cohorts containing the customer
     */
    List<Cohort> findByCustomerId(String customerId);
    
    /**
     * Deletes a cohort from the repository.
     *
     * @param cohortId The ID of the cohort to delete
     */
    void deleteById(String cohortId);
    
    /**
     * Checks if a cohort exists in the repository.
     *
     * @param cohortId The ID of the cohort to check
     * @return true if the cohort exists, false otherwise
     */
    boolean existsById(String cohortId);
}
