package com.cohortmgmt.controller;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import com.cohortmgmt.repository.CustomerRepository;
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
    private final CustomerRepository customerRepository;
    
    @Autowired
    public CohortController(CohortService cohortService, CustomerRepository customerRepository) {
        this.cohortService = cohortService;
        this.customerRepository = customerRepository;
    }
    
    /**
     * Checks if a customer is part of a specific cohort type.
     *
     * @param customerId The ID of the customer to check
     * @param cohortType The type of cohort to check
     * @return true if the customer is in the cohort type, false otherwise
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
     * Gets all cohort types associated with a customer.
     *
     * @param customerId The ID of the customer
     * @return The list of cohort types the customer is in
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<CohortType>> getCustomerCohortTypes(@PathVariable("customerId") String customerId) {
        logger.info("Getting cohort types for customer: {}", customerId);
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        List<CohortType> cohortTypes = cohortService.getCustomerCohortTypes(customerId);
        
        if (cohortTypes.isEmpty()) {
            logger.warn("No cohort types found for customer: {}", customerId);
        } else {
            logger.info("Found {} cohort types for customer: {}", cohortTypes.size(), customerId);
        }
        
        return ResponseEntity.ok(cohortTypes);
    }
    
    /**
     * Gets all customer IDs for a specific cohort type.
     *
     * @param cohortType The type of cohort
     * @return The set of customer IDs in the cohort type
     */
    @GetMapping("/type/{cohortType}/customers")
    public ResponseEntity<Set<String>> getCustomerIdsByCohortType(@PathVariable("cohortType") CohortType cohortType) {
        logger.info("Getting customer IDs for cohort type: {}", cohortType);
        
        if (cohortType == null) {
            throw new IllegalArgumentException("Cohort type cannot be null");
        }
        
        Set<String> customerIds = cohortService.getCustomerIdsByCohortType(cohortType);
        
        if (customerIds.isEmpty()) {
            logger.warn("No customers found for cohort type: {}", cohortType);
        } else {
            logger.info("Found {} customers for cohort type: {}", customerIds.size(), cohortType);
        }
        
        return ResponseEntity.ok(customerIds);
    }
    
    /**
     * Manually triggers the classification of a customer.
     * This is useful when a customer is added directly to the Customers table
     * without going through the application.
     *
     * @param customerId The ID of the customer to classify
     * @param dailySpend The daily spend of the customer
     * @param userType The user type of the customer
     * @return The set of cohort types the customer was classified into
     */
    @PostMapping("/classify")
    public ResponseEntity<Set<CohortType>> classifyCustomer(
            @RequestParam("customerId") String customerId,
            @RequestParam("dailySpend") Double dailySpend,
            @RequestParam("userType") UserType userType) {
        logger.info("Manually classifying customer: {}", customerId);
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        if (dailySpend == null) {
            throw new IllegalArgumentException("Daily spend cannot be null");
        }
        
        if (userType == null) {
            throw new IllegalArgumentException("User type cannot be null");
        }
        
        // Create a customer object
        Customer customer = new Customer(customerId, dailySpend, userType);
        
        // Classify the customer
        Set<CohortType> cohortTypes = cohortService.classifyCustomer(customer);
        
        logger.info("Customer {} classified into cohort types: {}", customerId, cohortTypes);
        
        return ResponseEntity.ok(cohortTypes);
    }
    
    /**
     * Manually triggers the classification of a customer by ID.
     * This endpoint retrieves the customer from the database and then classifies it.
     *
     * @param customerId The ID of the customer to classify
     * @return The set of cohort types the customer was classified into
     */
    @PostMapping("/classify/{customerId}")
    public ResponseEntity<?> classifyCustomerById(@PathVariable("customerId") String customerId) {
        logger.info("Manually classifying customer by ID: {}", customerId);
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        try {
            // Retrieve the customer from DynamoDB
            logger.info("Retrieving customer from DynamoDB: {}", customerId);
            
            // Use the AWS CLI to get the customer
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "aws", "dynamodb", "get-item",
                    "--table-name", "Customers",
                    "--key", "{\"customerId\":{\"S\":\"" + customerId + "\"}}",
                    "--endpoint-url", "http://localhost:4566"
            );
            
            Process process = processBuilder.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Error retrieving customer from DynamoDB: {}", exitCode);
                return ResponseEntity.badRequest().body("Error retrieving customer from DynamoDB");
            }
            
            String result = output.toString();
            logger.info("DynamoDB response: {}", result);
            
            // Parse the JSON response
            org.json.JSONObject json = new org.json.JSONObject(result);
            if (!json.has("Item")) {
                logger.error("Customer not found: {}", customerId);
                return ResponseEntity.notFound().build();
            }
            
            org.json.JSONObject item = json.getJSONObject("Item");
            String id = item.getJSONObject("customerId").getString("S");
            double dailySpend = Double.parseDouble(item.getJSONObject("dailySpend").getString("N"));
            UserType userType = UserType.valueOf(item.getJSONObject("userType").getString("S"));
            
            // Create a customer object
            Customer customer = new Customer(id, dailySpend, userType);
            
            // Classify the customer
            Set<CohortType> cohortTypes = cohortService.classifyCustomer(customer);
            
            logger.info("Customer {} classified into cohort types: {}", customerId, cohortTypes);
            
            return ResponseEntity.ok(cohortTypes);
        } catch (Exception e) {
            logger.error("Error classifying customer: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error classifying customer: " + e.getMessage());
        }
    }
}
