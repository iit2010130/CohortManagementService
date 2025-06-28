package com.cohortmgmt.controller;

import com.cohortmgmt.exception.ResourceNotFoundException;
import com.cohortmgmt.model.Cohort;
import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.service.CohortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST controller for cohort management operations.
 */
@RestController
@RequestMapping("/api/cohorts")
public class CohortController {
    
    private static final Logger logger = LoggerFactory.getLogger(CohortController.class);
    
    private final CohortService cohortService;
    
    @Autowired
    public CohortController(CohortService cohortService) {
        this.cohortService = cohortService;
    }
    
    /**
     * Checks if a customer is part of a specific cohort type.
     *
     * @param customerId The ID of the customer to check
     * @param cohortType The type of cohort to check
     * @return true if the customer is in any cohort of the specified type, false otherwise
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> isCustomerInCohortType(
            @RequestParam("customerId") String customerId,
            @RequestParam("cohortType") CohortType cohortType) {
        logger.info("Checking if customer {} is in cohort type {}", customerId, cohortType);
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        if (cohortType == null) {
            throw new IllegalArgumentException("Cohort type cannot be null");
        }
        
        boolean isInCohort = cohortService.isCustomerInCohortType(customerId, cohortType);
        
        logger.info("Customer {} is {} cohort type {}", 
                customerId, isInCohort ? "in" : "not in", cohortType);
        
        return ResponseEntity.ok(isInCohort);
    }
    
    /**
     * Gets all cohorts associated with a customer.
     *
     * @param customerId The ID of the customer
     * @return The list of cohorts the customer is in
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Cohort>> getCustomerCohorts(@PathVariable("customerId") String customerId) {
        logger.info("Getting cohorts for customer: {}", customerId);
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        List<Cohort> cohorts = cohortService.getCustomerCohorts(customerId);
        
        if (cohorts.isEmpty()) {
            logger.warn("No cohorts found for customer: {}", customerId);
        } else {
            logger.info("Found {} cohorts for customer: {}", cohorts.size(), customerId);
        }
        
        return ResponseEntity.ok(cohorts);
    }
    
    /**
     * Gets all customer IDs for a specific cohort type.
     *
     * @param cohortType The type of cohort
     * @return The set of customer IDs in cohorts of the specified type
     */
    @GetMapping("/type/{cohortType}/customers")
    public ResponseEntity<Set<String>> getCohortCustomerIdsByType(@PathVariable("cohortType") CohortType cohortType) {
        logger.info("Getting customer IDs for cohort type: {}", cohortType);
        
        if (cohortType == null) {
            throw new IllegalArgumentException("Cohort type cannot be null");
        }
        
        Set<String> customerIds = cohortService.getCohortCustomerIdsByType(cohortType);
        
        if (customerIds.isEmpty()) {
            logger.warn("No customers found for cohort type: {}", cohortType);
        } else {
            logger.info("Found {} customers for cohort type: {}", customerIds.size(), cohortType);
        }
        
        return ResponseEntity.ok(customerIds);
    }
}
