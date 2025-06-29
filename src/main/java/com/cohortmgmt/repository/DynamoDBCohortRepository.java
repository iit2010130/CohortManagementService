package com.cohortmgmt.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.cohortmgmt.model.Cohort;
import com.cohortmgmt.model.CohortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * DynamoDB implementation of the CohortRepository interface.
 * Minimized to support only the required operations:
 * 1. Determine if a given CustomerId is part of a specific cohort
 * 2. List all cohorts associated with a given CustomerId
 * 3. Retrieve all CustomerIds for a specific cohort
 */
@Repository
public class DynamoDBCohortRepository implements CohortRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBCohortRepository.class);
    
    private static final String CUSTOMER_ID_ATTR = "customerId";
    private static final String UUID_ATTR = "uuid";
    private static final String COHORT_TYPE_ATTR = "cohortType";
    private static final String DESCRIPTION_ATTR = "description";
    
    private final AmazonDynamoDB amazonDynamoDB;
    private final DynamoDB dynamoDB;
    private final String tableName;
    
    @Autowired
    public DynamoDBCohortRepository(
            AmazonDynamoDB amazonDynamoDB,
            DynamoDB dynamoDB,
            @Value("${aws.dynamodb.cohort-table}") String tableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.dynamoDB = dynamoDB;
        this.tableName = tableName;
    }
    
    @Override
    public Cohort save(Cohort cohort) {
        if (cohort == null || cohort.getId() == null) {
            throw new IllegalArgumentException("Cohort and cohort ID cannot be null");
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            // Get the customer ID from the cohort's customer IDs
            Set<String> customerIds = cohort.getCustomerIds();
            if (customerIds != null && !customerIds.isEmpty()) {
                // Save an entry for each customer ID
                for (String customerId : customerIds) {
                    String uuid = java.util.UUID.randomUUID().toString();
                    Item item = new Item()
                            .withPrimaryKey(CUSTOMER_ID_ATTR, customerId, UUID_ATTR, uuid)
                            .withString(COHORT_TYPE_ATTR, cohort.getType().name())
                            .withString(DESCRIPTION_ATTR, cohort.getDescription());
                    
                    table.putItem(item);
                    logger.info("Saved cohort with ID: {} for customerId: {}", cohort.getId(), customerId);
                }
            } else {
                // No customer IDs to save, just log it
                logger.info("No customer IDs to save for cohort with ID: {}", cohort.getId());
            }
            
            return cohort;
        } catch (Exception e) {
            logger.error("Error saving cohort with ID {}: {}", cohort.getId(), e.getMessage(), e);
            throw new RuntimeException("Error saving cohort", e);
        }
    }
    
    @Override
    public Optional<Cohort> findById(String cohortId) {
        if (cohortId == null) {
            return Optional.empty();
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            // Extract cohort type from cohort ID (assuming format like "DailySpend_PREMIUM")
            String[] parts = cohortId.split("_");
            if (parts.length != 2) {
                logger.error("Invalid cohort ID format: {}", cohortId);
                return Optional.empty();
            }
            
            String cohortType = parts[1];
            
            // Scan for items with this cohort type
            Map<String, Object> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":cohortType", cohortType);
            
            ItemCollection<ScanOutcome> items = table.scan(
                    COHORT_TYPE_ATTR + " = :cohortType", 
                    null, 
                    expressionAttributeValues);
            
            // Create a cohort object with the matching items
            Cohort cohort = new Cohort();
            cohort.setId(cohortId);
            cohort.setType(CohortType.valueOf(cohortType));
            cohort.setDescription("Cohort for " + parts[0] + " rule with type " + cohortType);
            
            Set<String> customerIds = new HashSet<>();
            items.forEach(item -> {
                String customerId = item.getString(CUSTOMER_ID_ATTR);
                // Skip the initialization entry (where customerId equals cohortId)
                if (!customerId.equals(cohortId)) {
                    customerIds.add(customerId);
                }
            });
            
            cohort.setCustomerIds(customerIds);
            
            // Even if no real customers found, return the cohort if it exists
            // This allows the cohort to be found during initialization
            
            return Optional.of(cohort);
        } catch (Exception e) {
            logger.error("Error finding cohort with ID {}: {}", cohortId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    
    @Override
    public boolean addCustomerToCohort(String cohortId, String customerId) {
        if (cohortId == null || customerId == null) {
            return false;
        }
        
        try {
            // Extract cohort type from cohort ID (assuming format like "DailySpend_PREMIUM")
            String[] parts = cohortId.split("_");
            if (parts.length != 2) {
                logger.error("Invalid cohort ID format: {}", cohortId);
                return false;
            }
            
            String cohortType = parts[1];
            
            // Add a new entry for this customer with the cohort type
            Table table = dynamoDB.getTable(tableName);
            
            String uuid = java.util.UUID.randomUUID().toString();
            Item item = new Item()
                    .withPrimaryKey(CUSTOMER_ID_ATTR, customerId, UUID_ATTR, uuid)
                    .withString(COHORT_TYPE_ATTR, cohortType)
                    .withString(DESCRIPTION_ATTR, "Cohort for " + parts[0] + " rule with type " + cohortType);
            
            table.putItem(item);
            
            logger.info("Added customer {} to cohort {}", customerId, cohortId);
            return true;
        } catch (Exception e) {
            logger.error("Error adding customer {} to cohort {}: {}", customerId, cohortId, e.getMessage(), e);
            return false;
        }
    }
    
    
    @Override
    public Set<String> getCustomerIds(String cohortId) {
        if (cohortId == null) {
            return Collections.emptySet();
        }
        
        try {
            // Extract cohort type from cohort ID (assuming format like "DailySpend_PREMIUM")
            String[] parts = cohortId.split("_");
            if (parts.length != 2) {
                logger.error("Invalid cohort ID format: {}", cohortId);
                return Collections.emptySet();
            }
            
            String cohortType = parts[1];
            
            // Scan for items with this cohort type
            Table table = dynamoDB.getTable(tableName);
            Map<String, Object> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":cohortType", cohortType);
            
            ItemCollection<ScanOutcome> items = table.scan(
                    COHORT_TYPE_ATTR + " = :cohortType", 
                    null, 
                    expressionAttributeValues);
            
            // Collect all customer IDs, excluding the initialization entry
            Set<String> customerIds = new HashSet<>();
            items.forEach(item -> {
                String customerId = item.getString(CUSTOMER_ID_ATTR);
                // Skip the initialization entry (where customerId equals cohortId)
                if (!customerId.equals(cohortId)) {
                    customerIds.add(customerId);
                }
            });
            
            return customerIds;
        } catch (Exception e) {
            logger.error("Error getting customer IDs for cohort {}: {}", cohortId, e.getMessage(), e);
            return Collections.emptySet();
        }
    }
    
    @Override
    public List<Cohort> findByCustomerId(String customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        
        try {
            List<Cohort> cohorts = new ArrayList<>();
            Table table = dynamoDB.getTable(tableName);
            
            // Check if this is a cohort ID (used for initialization)
            if (customerId.contains("_")) {
                // This is likely a cohort ID, not a real customer ID
                // Skip it to avoid showing initialization entries
                return cohorts;
            }
            
            // Query for items with this customer ID
            Map<String, Object> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":customerId", customerId);
            
            ItemCollection<ScanOutcome> items = table.scan(
                    CUSTOMER_ID_ATTR + " = :customerId", 
                    null, 
                    expressionAttributeValues);
            
            Iterator<Item> iterator = items.iterator();
            if (iterator.hasNext()) {
                Item item = iterator.next();
                String cohortType = item.getString(COHORT_TYPE_ATTR);
                String description = item.getString(DESCRIPTION_ATTR);
                
                // Create a cohort object
                Cohort cohort = new Cohort();
                cohort.setId("Rule_" + cohortType); // Reconstruct a cohort ID
                cohort.setType(CohortType.valueOf(cohortType));
                cohort.setDescription(description);
                cohort.setCustomerIds(Collections.singleton(customerId));
                
                cohorts.add(cohort);
            }
            
            return cohorts;
        } catch (Exception e) {
            logger.error("Error finding cohorts for customer {}: {}", customerId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    
    @Override
    public boolean existsById(String cohortId) {
        if (cohortId == null) {
            return false;
        }
        
        try {
            // Extract cohort type from cohort ID (assuming format like "DailySpend_PREMIUM")
            String[] parts = cohortId.split("_");
            if (parts.length != 2) {
                logger.error("Invalid cohort ID format: {}", cohortId);
                return false;
            }
            
            // For initialization, we'll consider a cohort to exist if its ID is valid
            // This avoids having to create initialization entries
            try {
                CohortType.valueOf(parts[1]);
                return true;
            } catch (IllegalArgumentException e) {
                logger.error("Invalid cohort type in cohort ID {}: {}", cohortId, e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error checking if cohort exists with ID {}: {}", cohortId, e.getMessage(), e);
            return false;
        }
    }
}
