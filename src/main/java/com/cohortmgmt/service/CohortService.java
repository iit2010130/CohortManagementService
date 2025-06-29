package com.cohortmgmt.service;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;

import java.util.List;
import java.util.Set;

/**
 * Service interface for managing cohorts and classifying customers.
 * 
 * This service provides the core functionality for the Cohort Management Service:
 * 1. Classifying customers into cohort types based on rules
 * 2. Checking if a customer is in a specific cohort type
 * 3. Getting all cohort types for a customer
 * 4. Getting all customers in a cohort type
 */
public interface CohortService {
    
    /**
     * Classifies a customer into cohort types based on the configured rules.
     * This is an internal method used by the data processing services.
     *
     * @param customer The customer to classify
     * @return The set of cohort types the customer was classified into
     */
    Set<CohortType> classifyCustomer(Customer customer);
    
    /**
     * Checks if a customer is part of a specific cohort type.
     * This is one of the three required APIs mentioned in the README.md.
     *
     * @param customerId The ID of the customer to check
     * @param cohortType The type of cohort to check
     * @return true if the customer is in the cohort type, false otherwise
     */
    boolean isCustomerInCohortType(String customerId, CohortType cohortType);
    
    /**
     * Gets all cohort types associated with a customer.
     * This is one of the three required APIs mentioned in the README.md.
     *
     * @param customerId The ID of the customer
     * @return The list of cohort types the customer is in
     */
    List<CohortType> getCustomerCohortTypes(String customerId);
    
    /**
     * Gets all customer IDs for a specific cohort type.
     * This is one of the three required APIs mentioned in the README.md.
     *
     * @param cohortType The type of cohort
     * @return The set of customer IDs in the cohort type
     */
    Set<String> getCustomerIdsByCohortType(CohortType cohortType);
    
    /**
     * Initializes the cohort types based on the configured rules.
     * This is called during application startup.
     */
    void initializeCohortTypes();
}
