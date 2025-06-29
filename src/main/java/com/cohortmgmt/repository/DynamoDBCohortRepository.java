package com.cohortmgmt.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
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
 * 1. Determine if a given CustomerId is part of a specific cohort type
 * 2. List all cohort types associated with a given CustomerId
 * 3. Retrieve all CustomerIds for a specific cohort type
 */
@Repository
public class DynamoDBCohortRepository implements CohortRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBCohortRepository.class);
    
    private static final String CUSTOMER_ID_ATTR = "customerId";
    private static final String UUID_ATTR = "uuid";
    private static final String COHORT_TYPE_ATTR = "cohortType";
    private static final String COHORT_TYPE_INDEX = "CohortTypeIndex";
    
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
    public boolean addCustomerToCohortType(CohortType cohortType, String customerId) {
        if (cohortType == null || customerId == null) {
            return false;
        }
        
        try {
            // First check if this customer is already in this cohort type
            if (isCustomerInCohortType(customerId, cohortType)) {
                logger.info("Customer {} is already in cohort type {}, skipping addition", customerId, cohortType);
                return true; // Already in the cohort type, consider it a success
            }
            
            // Add a new entry for this customer with the cohort type
            Table table = dynamoDB.getTable(tableName);
            
            String uuid = java.util.UUID.randomUUID().toString();
            Item item = new Item()
                    .withPrimaryKey(CUSTOMER_ID_ATTR, customerId, UUID_ATTR, uuid)
                    .withString(COHORT_TYPE_ATTR, cohortType.name());
            
            // Explicitly log the item to see what's being saved
            logger.info("Saving item to Cohorts table: {}", item.toJSON());
            
            table.putItem(item);
            
            logger.info("Added customer {} to cohort type {}", customerId, cohortType);
            return true;
        } catch (Exception e) {
            logger.error("Error adding customer {} to cohort type {}: {}", customerId, cohortType, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Set<String> getCustomerIdsByCohortType(CohortType cohortType) {
        if (cohortType == null) {
            return Collections.emptySet();
        }
        
        try {
            // Query the GSI for items with this cohort type
            Table table = dynamoDB.getTable(tableName);
            Index index = table.getIndex(COHORT_TYPE_INDEX);
            
            QuerySpec querySpec = new QuerySpec()
                    .withKeyConditionExpression(COHORT_TYPE_ATTR + " = :cohortType")
                    .withValueMap(new ValueMap().withString(":cohortType", cohortType.name()));
            
            ItemCollection<QueryOutcome> items = index.query(querySpec);
            
            // Collect all customer IDs
            Set<String> customerIds = new HashSet<>();
            items.forEach(item -> {
                String customerId = item.getString(CUSTOMER_ID_ATTR);
                customerIds.add(customerId);
            });
            
            logger.info("Found {} customers for cohort type {}", customerIds.size(), cohortType);
            return customerIds;
        } catch (Exception e) {
            logger.error("Error getting customer IDs for cohort type {}: {}", cohortType, e.getMessage(), e);
            return Collections.emptySet();
        }
    }
    
    @Override
    public List<CohortType> findCohortTypesByCustomerId(String customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        
        try {
            List<CohortType> cohortTypes = new ArrayList<>();
            Table table = dynamoDB.getTable(tableName);
            
            // Query for items with this customer ID
            QuerySpec querySpec = new QuerySpec()
                    .withKeyConditionExpression(CUSTOMER_ID_ATTR + " = :customerId")
                    .withValueMap(new ValueMap().withString(":customerId", customerId));
            
            ItemCollection<QueryOutcome> items = table.query(querySpec);
            
            // Collect all cohort types for this customer
            Set<String> cohortTypeNames = new HashSet<>();
            items.forEach(item -> {
                String cohortTypeName = item.getString(COHORT_TYPE_ATTR);
                cohortTypeNames.add(cohortTypeName);
            });
            
            // Convert cohort type names to CohortType enum values
            for (String cohortTypeName : cohortTypeNames) {
                try {
                    CohortType cohortType = CohortType.valueOf(cohortTypeName);
                    cohortTypes.add(cohortType);
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid cohort type name: {}", cohortTypeName);
                }
            }
            
            logger.info("Found {} cohort types for customer {}", cohortTypes.size(), customerId);
            return cohortTypes;
        } catch (Exception e) {
            logger.error("Error finding cohort types for customer {}: {}", customerId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean isCustomerInCohortType(String customerId, CohortType cohortType) {
        if (customerId == null || cohortType == null) {
            return false;
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            // Query for items with this customer ID
            QuerySpec querySpec = new QuerySpec()
                    .withKeyConditionExpression(CUSTOMER_ID_ATTR + " = :customerId")
                    .withFilterExpression(COHORT_TYPE_ATTR + " = :cohortType")
                    .withValueMap(new ValueMap()
                            .withString(":customerId", customerId)
                            .withString(":cohortType", cohortType.name()));
            
            ItemCollection<QueryOutcome> items = table.query(querySpec);
            
            // If there's at least one item, the customer is in the cohort type
            boolean result = items.iterator().hasNext();
            logger.debug("Customer {} is {} cohort type {}", customerId, result ? "in" : "not in", cohortType);
            return result;
        } catch (Exception e) {
            logger.error("Error checking if customer {} is in cohort type {}: {}", customerId, cohortType, e.getMessage(), e);
            return false;
        }
    }
}
