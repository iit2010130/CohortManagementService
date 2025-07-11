package com.cohortmgmt.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.service.CohortService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * DynamoDB implementation of the CustomerRepository interface.
 * Minimized to support only the required operations.
 */
@Repository
public class DynamoDBCustomerRepository implements CustomerRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBCustomerRepository.class);
    
    private static final String CUSTOMER_ID_ATTR = "customerId";
    private static final String DAILY_SPEND_ATTR = "dailySpend";
    private static final String USER_TYPE_ATTR = "userType";
    
    private final AmazonDynamoDB amazonDynamoDB;
    private final DynamoDB dynamoDB;
    private final String tableName;
    private final CohortService cohortService;
    
    @Autowired
    public DynamoDBCustomerRepository(
            AmazonDynamoDB amazonDynamoDB,
            DynamoDB dynamoDB,
            CohortService cohortService,
            @Value("${aws.dynamodb.customer-table}") String tableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.dynamoDB = dynamoDB;
        this.cohortService = cohortService;
        this.tableName = tableName;
    }
    
    @Override
    public Customer save(Customer customer) {
        if (customer == null || customer.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer and customer ID cannot be null");
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            Item item = new Item()
                    .withPrimaryKey(CUSTOMER_ID_ATTR, customer.getCustomerId())
                    .withDouble(DAILY_SPEND_ATTR, customer.getDailySpend())
                    .withString(USER_TYPE_ATTR, customer.getUserType().name());
            
            table.putItem(item);
            
            logger.info("Saved customer with ID: {}", customer.getCustomerId());
            
            // Classify the customer into cohort types
            try {
                logger.info("Classifying customer with ID: {}", customer.getCustomerId());
                Set<CohortType> cohortTypes = cohortService.classifyCustomer(customer);
                logger.info("Customer {} classified into cohort types: {}", customer.getCustomerId(), cohortTypes);
            } catch (Exception e) {
                logger.error("Error classifying customer with ID {}: {}", customer.getCustomerId(), e.getMessage(), e);
                // Don't rethrow the exception, as we don't want to fail the save operation
            }
            
            return customer;
        } catch (Exception e) {
            logger.error("Error saving customer with ID {}: {}", customer.getCustomerId(), e.getMessage(), e);
            throw new RuntimeException("Error saving customer", e);
        }
    }
}
