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
import java.util.stream.Collectors;

/**
 * DynamoDB implementation of the CohortRepository interface.
 */
@Repository
public class DynamoDBCohortRepository implements CohortRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBCohortRepository.class);
    
    private static final String COHORT_ID_ATTR = "cohortId";
    private static final String COHORT_TYPE_ATTR = "cohortType";
    private static final String DESCRIPTION_ATTR = "description";
    private static final String CUSTOMER_IDS_ATTR = "customerIds";
    
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
            
            Item item = new Item()
                    .withPrimaryKey(COHORT_ID_ATTR, cohort.getId())
                    .withString(COHORT_TYPE_ATTR, cohort.getType().name())
                    .withString(DESCRIPTION_ATTR, cohort.getDescription())
                    .withStringSet(CUSTOMER_IDS_ATTR, cohort.getCustomerIds());
            
            table.putItem(item);
            
            logger.info("Saved cohort with ID: {}", cohort.getId());
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
            
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey(COHORT_ID_ATTR, cohortId);
            
            Item item = table.getItem(spec);
            
            if (item == null) {
                return Optional.empty();
            }
            
            Cohort cohort = new Cohort();
            cohort.setId(item.getString(COHORT_ID_ATTR));
            cohort.setType(CohortType.valueOf(item.getString(COHORT_TYPE_ATTR)));
            cohort.setDescription(item.getString(DESCRIPTION_ATTR));
            
            Set<String> customerIds = item.getStringSet(CUSTOMER_IDS_ATTR);
            if (customerIds != null) {
                cohort.setCustomerIds(customerIds);
            }
            
            return Optional.of(cohort);
        } catch (Exception e) {
            logger.error("Error finding cohort with ID {}: {}", cohortId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<Cohort> findAll() {
        try {
            List<Cohort> cohorts = new ArrayList<>();
            Table table = dynamoDB.getTable(tableName);
            
            table.scan().forEach(item -> {
                Cohort cohort = new Cohort();
                cohort.setId(item.getString(COHORT_ID_ATTR));
                cohort.setType(CohortType.valueOf(item.getString(COHORT_TYPE_ATTR)));
                cohort.setDescription(item.getString(DESCRIPTION_ATTR));
                
                Set<String> customerIds = item.getStringSet(CUSTOMER_IDS_ATTR);
                if (customerIds != null) {
                    cohort.setCustomerIds(customerIds);
                }
                
                cohorts.add(cohort);
            });
            
            return cohorts;
        } catch (Exception e) {
            logger.error("Error finding all cohorts: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Cohort> findByType(CohortType cohortType) {
        if (cohortType == null) {
            return Collections.emptyList();
        }
        
        try {
            List<Cohort> cohorts = new ArrayList<>();
            Table table = dynamoDB.getTable(tableName);
            
            Map<String, Object> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":cohortType", cohortType.name());
            
            table.scan("cohortType = :cohortType", null, expressionAttributeValues)
                    .forEach(item -> {
                        Cohort cohort = new Cohort();
                        cohort.setId(item.getString(COHORT_ID_ATTR));
                        cohort.setType(CohortType.valueOf(item.getString(COHORT_TYPE_ATTR)));
                        cohort.setDescription(item.getString(DESCRIPTION_ATTR));
                        
                        Set<String> customerIds = item.getStringSet(CUSTOMER_IDS_ATTR);
                        if (customerIds != null) {
                            cohort.setCustomerIds(customerIds);
                        }
                        
                        cohorts.add(cohort);
                    });
            
            return cohorts;
        } catch (Exception e) {
            logger.error("Error finding cohorts by type {}: {}", cohortType, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean addCustomerToCohort(String cohortId, String customerId) {
        if (cohortId == null || customerId == null) {
            return false;
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withPrimaryKey(COHORT_ID_ATTR, cohortId)
                    .withUpdateExpression("ADD " + CUSTOMER_IDS_ATTR + " :customerId")
                    .withValueMap(new ValueMap().withStringSet(":customerId", Collections.singleton(customerId)))
                    .withReturnValues(ReturnValue.UPDATED_NEW);
            
            table.updateItem(updateItemSpec);
            
            logger.info("Added customer {} to cohort {}", customerId, cohortId);
            return true;
        } catch (Exception e) {
            logger.error("Error adding customer {} to cohort {}: {}", customerId, cohortId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean removeCustomerFromCohort(String cohortId, String customerId) {
        if (cohortId == null || customerId == null) {
            return false;
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                    .withPrimaryKey(COHORT_ID_ATTR, cohortId)
                    .withUpdateExpression("DELETE " + CUSTOMER_IDS_ATTR + " :customerId")
                    .withValueMap(new ValueMap().withStringSet(":customerId", Collections.singleton(customerId)))
                    .withReturnValues(ReturnValue.UPDATED_NEW);
            
            table.updateItem(updateItemSpec);
            
            logger.info("Removed customer {} from cohort {}", customerId, cohortId);
            return true;
        } catch (Exception e) {
            logger.error("Error removing customer {} from cohort {}: {}", customerId, cohortId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Set<String> getCustomerIds(String cohortId) {
        if (cohortId == null) {
            return Collections.emptySet();
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey(COHORT_ID_ATTR, cohortId)
                    .withAttributesToGet(CUSTOMER_IDS_ATTR);
            
            Item item = table.getItem(spec);
            
            if (item == null) {
                return Collections.emptySet();
            }
            
            Set<String> customerIds = item.getStringSet(CUSTOMER_IDS_ATTR);
            return customerIds != null ? customerIds : Collections.emptySet();
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
            
            // This is not efficient for large datasets, but it's a simple implementation for now
            table.scan().forEach(item -> {
                Set<String> customerIds = item.getStringSet(CUSTOMER_IDS_ATTR);
                if (customerIds != null && customerIds.contains(customerId)) {
                    Cohort cohort = new Cohort();
                    cohort.setId(item.getString(COHORT_ID_ATTR));
                    cohort.setType(CohortType.valueOf(item.getString(COHORT_TYPE_ATTR)));
                    cohort.setDescription(item.getString(DESCRIPTION_ATTR));
                    cohort.setCustomerIds(customerIds);
                    cohorts.add(cohort);
                }
            });
            
            return cohorts;
        } catch (Exception e) {
            logger.error("Error finding cohorts for customer {}: {}", customerId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void deleteById(String cohortId) {
        if (cohortId == null) {
            return;
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            table.deleteItem(COHORT_ID_ATTR, cohortId);
            logger.info("Deleted cohort with ID: {}", cohortId);
        } catch (Exception e) {
            logger.error("Error deleting cohort with ID {}: {}", cohortId, e.getMessage(), e);
            throw new RuntimeException("Error deleting cohort", e);
        }
    }
    
    @Override
    public boolean existsById(String cohortId) {
        if (cohortId == null) {
            return false;
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey(COHORT_ID_ATTR, cohortId);
            
            Item item = table.getItem(spec);
            
            return item != null;
        } catch (Exception e) {
            logger.error("Error checking if cohort exists with ID {}: {}", cohortId, e.getMessage(), e);
            return false;
        }
    }
}
