package com.cohortmgmt.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service that simulates a Lambda function triggered by DynamoDB events.
 * This service polls the Customers table periodically and processes any new or modified items.
 */
@Service
public class DynamoDBTriggerHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBTriggerHandler.class);
    
    private final AmazonDynamoDB amazonDynamoDB;
    private final CohortService cohortService;
    private final String customerTableName;
    
    // Keep track of the last processed items to avoid processing the same item multiple times
    private final Map<String, Long> processedItems = new HashMap<>();
    
    @Autowired
    public DynamoDBTriggerHandler(
            AmazonDynamoDB amazonDynamoDB,
            CohortService cohortService,
            @Value("${aws.dynamodb.customer-table}") String customerTableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.cohortService = cohortService;
        this.customerTableName = customerTableName;
    }
    
    /**
     * Polls the Customers table periodically and processes any new or modified items.
     * This method is scheduled to run every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    public void pollCustomersTable() {
        try {
            logger.debug("Polling Customers table for new or modified items");
            
            // Scan the Customers table
            com.amazonaws.services.dynamodbv2.model.ScanRequest scanRequest = new com.amazonaws.services.dynamodbv2.model.ScanRequest()
                    .withTableName(customerTableName);
            
            com.amazonaws.services.dynamodbv2.model.ScanResult scanResult = amazonDynamoDB.scan(scanRequest);
            
            // Process each item
            for (Map<String, AttributeValue> item : scanResult.getItems()) {
                String customerId = item.get("customerId").getS();
                
                // Skip if we've already processed this item recently
                if (processedItems.containsKey(customerId)) {
                    long lastProcessedTime = processedItems.get(customerId);
                    if (System.currentTimeMillis() - lastProcessedTime < 60000) { // 1 minute
                        continue;
                    }
                }
                
                // Process the item
                processItem(item);
                
                // Mark as processed
                processedItems.put(customerId, System.currentTimeMillis());
            }
        } catch (Exception e) {
            logger.error("Error polling Customers table: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Processes a single item from the Customers table.
     *
     * @param item The item to process
     */
    private void processItem(Map<String, AttributeValue> item) {
        try {
            // Extract customer data
            String customerId = item.get("customerId").getS();
            Double dailySpend = Double.parseDouble(item.get("dailySpend").getN());
            UserType userType = UserType.valueOf(item.get("userType").getS());
            
            logger.info("Processing customer: {}", customerId);
            
            // Create a customer object
            Customer customer = new Customer(customerId, dailySpend, userType);
            
            // Classify the customer
            Set<CohortType> cohortTypes = cohortService.classifyCustomer(customer);
            
            logger.info("Customer {} classified into cohort types: {}", customerId, cohortTypes);
        } catch (Exception e) {
            logger.error("Error processing item: {}", e.getMessage(), e);
        }
    }
}
