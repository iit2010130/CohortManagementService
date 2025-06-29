package com.cohortmgmt.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Service for processing DynamoDB streams to classify customers into cohorts.
 * This service consumes events from the DynamoDB stream, processes them,
 * and stores the classification results in the second DynamoDB table.
 */
@Service
public class DynamoDBStreamProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBStreamProcessingService.class);
    
    private final AmazonDynamoDB amazonDynamoDB;
    private final DynamoDB dynamoDB;
    private final CohortService cohortService;
    private final String customerTableName;
    
    @Autowired
    public DynamoDBStreamProcessingService(
            AmazonDynamoDB amazonDynamoDB,
            DynamoDB dynamoDB,
            CohortService cohortService,
            @Value("${aws.dynamodb.customer-table}") String customerTableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.dynamoDB = dynamoDB;
        this.cohortService = cohortService;
        this.customerTableName = customerTableName;
    }
    
    /**
     * Processes a DynamoDB stream record.
     * This method is called by the DynamoDB stream consumer.
     *
     * @param record The record to process
     */
    public void processRecord(Record record) {
        try {
            StreamRecord streamRecord = record.getDynamodb();
            
            if (streamRecord == null) {
                logger.warn("Stream record is null");
                return;
            }
            
            // Process the record based on the event type
            switch (record.getEventName()) {
                case "INSERT":
                case "MODIFY":
                    processInsertOrModify(streamRecord.getNewImage());
                    break;
                default:
                    logger.info("Ignoring event type: {}", record.getEventName());
            }
        } catch (Exception e) {
            logger.error("Error processing DynamoDB stream record: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Processes an INSERT or MODIFY event.
     *
     * @param newImage The new image of the record
     */
    private void processInsertOrModify(Map<String, AttributeValue> newImage) {
        if (newImage == null) {
            logger.warn("New image is null");
            return;
        }
        
        try {
            // Extract customer data from the record
            String customerId = newImage.get("customerId").getS();
            Double dailySpend = Double.parseDouble(newImage.get("dailySpend").getN());
            UserType userType = UserType.valueOf(newImage.get("userType").getS());
            
            // Create a customer object
            Customer customer = new Customer(customerId, dailySpend, userType);
            
            // Classify the customer into cohort types
            Set<CohortType> cohortTypes = cohortService.classifyCustomer(customer);
            
            logger.info("Classified customer {} into cohort types: {}", customerId, cohortTypes);
        } catch (Exception e) {
            logger.error("Error processing INSERT or MODIFY event: {}", e.getMessage(), e);
        }
    }
    
}
