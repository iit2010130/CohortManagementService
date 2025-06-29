package com.cohortmgmt.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Initializes AWS resources when the application starts.
 * This ensures that required resources like SQS queues exist before they are used.
 */
@Component
public class AwsResourceInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsResourceInitializer.class);
    
    private final AmazonSQS amazonSQS;
    private final AmazonDynamoDB amazonDynamoDB;
    private final String queueName;
    private final String customerTableName;
    private final String cohortTableName;
    
    @Autowired
    public AwsResourceInitializer(
            AmazonSQS amazonSQS,
            AmazonDynamoDB amazonDynamoDB,
            @Value("${aws.sqs.queue-name}") String queueName,
            @Value("${aws.dynamodb.customer-table}") String customerTableName,
            @Value("${aws.dynamodb.cohort-table}") String cohortTableName) {
        this.amazonSQS = amazonSQS;
        this.amazonDynamoDB = amazonDynamoDB;
        this.queueName = queueName;
        this.customerTableName = customerTableName;
        this.cohortTableName = cohortTableName;
    }
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        createSqsQueueIfNotExists();
        createCustomerTableIfNotExists();
        createCohortTableIfNotExists();
    }
    
    /**
     * Creates the Customers DynamoDB table if it doesn't already exist.
     * This table will have a DynamoDB Stream enabled.
     */
    private void createCustomerTableIfNotExists() {
        try {
            // First check if the table already exists
            try {
                amazonDynamoDB.describeTable(customerTableName);
                logger.info("DynamoDB table already exists: {}", customerTableName);
                return;
            } catch (ResourceNotFoundException e) {
                // Table doesn't exist, continue to creation
                logger.debug("Table doesn't exist, will create: {}", customerTableName);
            }
            
            // Create the table with a stream enabled
            CreateTableRequest createTableRequest = new CreateTableRequest()
                    .withTableName(customerTableName)
                    .withKeySchema(new KeySchemaElement("customerId", KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition("customerId", "S"))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                    .withStreamSpecification(new StreamSpecification()
                            .withStreamEnabled(true)
                            .withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES));
            
            amazonDynamoDB.createTable(createTableRequest);
            logger.info("Successfully created DynamoDB table with stream enabled: {}", customerTableName);
            
            // Wait a moment to ensure the table is fully created and available
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            logger.warn("Error creating DynamoDB table {}: {}", customerTableName, e.getMessage());
        }
    }
    
    /**
     * Creates the Cohorts DynamoDB table if it doesn't already exist.
     */
    private void createCohortTableIfNotExists() {
        try {
            // First check if the table already exists
            try {
                amazonDynamoDB.describeTable(cohortTableName);
                logger.info("DynamoDB table already exists: {}", cohortTableName);
                return;
            } catch (ResourceNotFoundException e) {
                // Table doesn't exist, continue to creation
                logger.debug("Table doesn't exist, will create: {}", cohortTableName);
            }
            
            // Create the table with customerId as hash key, uuid as range key, and cohortType as GSI
            CreateTableRequest createTableRequest = new CreateTableRequest()
                    .withTableName(cohortTableName)
                    .withKeySchema(
                            new KeySchemaElement("customerId", KeyType.HASH),
                            new KeySchemaElement("uuid", KeyType.RANGE))
                    .withAttributeDefinitions(
                            new AttributeDefinition("customerId", "S"),
                            new AttributeDefinition("uuid", "S"),
                            new AttributeDefinition("cohortType", "S"))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                    .withGlobalSecondaryIndexes(
                            new com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex()
                                    .withIndexName("CohortTypeIndex")
                                    .withKeySchema(new KeySchemaElement("cohortType", KeyType.HASH))
                                    .withProjection(new com.amazonaws.services.dynamodbv2.model.Projection().withProjectionType("ALL"))
                                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L)));
            
            amazonDynamoDB.createTable(createTableRequest);
            logger.info("Successfully created DynamoDB table with GSI: {}", cohortTableName);
            
            // Wait a moment to ensure the table is fully created and available
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            logger.warn("Error creating DynamoDB table {}: {}", cohortTableName, e.getMessage());
        }
    }
    
    /**
     * Creates the SQS queue if it doesn't already exist.
     */
    private void createSqsQueueIfNotExists() {
        try {
            // First check if the queue already exists
            try {
                String queueUrl = amazonSQS.getQueueUrl(queueName).getQueueUrl();
                logger.info("SQS queue already exists: {} with URL: {}", queueName, queueUrl);
                return;
            } catch (Exception e) {
                // Queue doesn't exist, continue to creation
                logger.debug("Queue doesn't exist, will create: {}", queueName);
            }
            
            // Create the queue
            String queueUrl = amazonSQS.createQueue(new CreateQueueRequest(queueName)).getQueueUrl();
            logger.info("Successfully created SQS queue: {} with URL: {}", queueName, queueUrl);
            
            // Wait a moment to ensure the queue is fully created and available
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            logger.warn("Error creating SQS queue {}: {}", queueName, e.getMessage());
        }
    }
}
