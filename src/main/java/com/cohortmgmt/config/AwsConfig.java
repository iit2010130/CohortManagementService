package com.cohortmgmt.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for AWS services.
 */
@Configuration
public class AwsConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsConfig.class);
    
    @Value("${aws.region}")
    private String region;
    
    @Value("${aws.endpoint}")
    private String endpoint;
    
    @Value("${aws.dynamodb.customer-table}")
    private String customerTableName;
    
    @Value("${aws.dynamodb.cohort-table}")
    private String cohortTableName;
    
    @Value("${aws.sqs.queue-name}")
    private String queueName;
    
    /**
     * Creates a DynamoDB client for LocalStack.
     *
     * @return The DynamoDB client
     */
    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy")))
                .build();
    }
    
    /**
     * Creates a DynamoDB document client.
     *
     * @param amazonDynamoDB The DynamoDB client
     * @return The DynamoDB document client
     */
    @Bean
    public DynamoDB dynamoDB(AmazonDynamoDB amazonDynamoDB) {
        return new DynamoDB(amazonDynamoDB);
    }
    
    /**
     * Creates an SQS client for LocalStack.
     *
     * @return The SQS client
     */
    @Bean
    public AmazonSQS amazonSQS() {
        return AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy")))
                .build();
    }
    
    /**
     * CommandLineRunner to initialize AWS resources after all beans are created.
     * This runs after the application context is fully loaded.
     *
     * @param amazonDynamoDB The DynamoDB client
     * @param amazonSQS The SQS client
     * @return A CommandLineRunner that initializes AWS resources
     */
    @Bean
    @Order(1)
    public CommandLineRunner initAwsResources(AmazonDynamoDB amazonDynamoDB, AmazonSQS amazonSQS) {
        return args -> {
            try {
                // Create tables
                createCustomerTable(amazonDynamoDB);
                createCohortTable(amazonDynamoDB);
                createSqsQueue(amazonSQS);
            } catch (Exception e) {
                logger.error("Error initializing AWS resources: {}", e.getMessage(), e);
                // Log error but don't throw exception to allow application to start
            }
        };
    }
    
    /**
     * Creates the customer table in DynamoDB.
     *
     * @param client The DynamoDB client
     */
    private void createCustomerTable(AmazonDynamoDB client) {
        try {
            // Check if table already exists
            client.describeTable(customerTableName);
            logger.info("Customer table already exists: {}", customerTableName);
        } catch (ResourceNotFoundException e) {
            // Create table if it doesn't exist
            logger.info("Creating customer table: {}", customerTableName);
            
            try {
                CreateTableRequest request = new CreateTableRequest()
                        .withTableName(customerTableName)
                        .withKeySchema(new KeySchemaElement("customerId", KeyType.HASH))
                        .withAttributeDefinitions(new AttributeDefinition("customerId", ScalarAttributeType.S))
                        .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                        .withStreamSpecification(new StreamSpecification()
                                .withStreamEnabled(true)
                                .withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES));
                
                logger.info("Sending create table request for {}: {}", customerTableName, request);
                try {
                    client.createTable(request);
                    logger.info("Customer table created: {}", customerTableName);
                    
                    // Wait for table to become active
                    waitForTableActive(client, customerTableName);
                } catch (ResourceInUseException riue) {
                    // Table already exists (race condition), just log and continue
                    logger.info("Customer table already exists (race condition): {}", customerTableName);
                }
            } catch (Exception ex) {
                if (ex instanceof ResourceInUseException) {
                    // Table already exists (race condition), just log and continue
                    logger.info("Customer table already exists (race condition): {}", customerTableName);
                } else {
                    logger.error("Failed to create customer table: {}", ex.getMessage(), ex);
                    // Log error but don't throw exception to allow application to start
                }
            }
        } catch (Exception e) {
            logger.error("Error checking if customer table exists: {}", e.getMessage(), e);
            // Log error but don't throw exception to allow application to start
        }
    }
    
    /**
     * Creates the cohort table in DynamoDB.
     *
     * @param client The DynamoDB client
     */
    private void createCohortTable(AmazonDynamoDB client) {
        try {
            // Check if table already exists
            client.describeTable(cohortTableName);
            logger.info("Cohort table already exists: {}", cohortTableName);
        } catch (ResourceNotFoundException e) {
            // Create table if it doesn't exist
            logger.info("Creating cohort table: {}", cohortTableName);
            
            try {
                // Define attribute definitions - only include attributes used in key schemas
                List<AttributeDefinition> attributeDefinitions = Arrays.asList(
                    new AttributeDefinition("customerId", ScalarAttributeType.S),
                    new AttributeDefinition("uuid", ScalarAttributeType.S),
                    new AttributeDefinition("cohortType", ScalarAttributeType.S)
                );
                
                // Define key schema (primary key = customerId + uuid)
                List<KeySchemaElement> keySchema = Arrays.asList(
                    new KeySchemaElement("customerId", KeyType.HASH),  // Partition key
                    new KeySchemaElement("uuid", KeyType.RANGE)        // Sort key
                );
                
                // Define GSI for cohortType
                GlobalSecondaryIndex cohortTypeIndex = new GlobalSecondaryIndex()
                    .withIndexName("CohortTypeIndex")
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                    .withKeySchema(new KeySchemaElement("cohortType", KeyType.HASH))
                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL));
                
                // Create table request
                CreateTableRequest request = new CreateTableRequest()
                    .withTableName(cohortTableName)
                    .withKeySchema(keySchema)
                    .withAttributeDefinitions(attributeDefinitions)
                    .withGlobalSecondaryIndexes(cohortTypeIndex)
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
                
                logger.info("Sending create table request for {}: {}", cohortTableName, request);
                try {
                    client.createTable(request);
                    logger.info("Cohort table created: {}", cohortTableName);
                    
                    // Wait for table to become active
                    waitForTableActive(client, cohortTableName);
                } catch (ResourceInUseException riue) {
                    // Table already exists (race condition), just log and continue
                    logger.info("Cohort table already exists (race condition): {}", cohortTableName);
                }
            } catch (Exception ex) {
                if (ex instanceof ResourceInUseException) {
                    // Table already exists (race condition), just log and continue
                    logger.info("Cohort table already exists (race condition): {}", cohortTableName);
                } else {
                    logger.error("Failed to create cohort table: {}", ex.getMessage(), ex);
                    // Log error but don't throw exception to allow application to start
                }
            }
        } catch (Exception e) {
            logger.error("Error checking if cohort table exists: {}", e.getMessage(), e);
            // Log error but don't throw exception to allow application to start
        }
    }
    
    /**
     * Creates the SQS queue.
     *
     * @param client The SQS client
     */
    private void createSqsQueue(AmazonSQS client) {
        try {
            // First check if the queue already exists
            try {
                String queueUrl = client.getQueueUrl(queueName).getQueueUrl();
                logger.info("SQS queue already exists: {} with URL: {}", queueName, queueUrl);
                return;
            } catch (Exception e) {
                // Queue doesn't exist, continue to creation
                logger.debug("Queue doesn't exist, will create: {}", queueName);
            }
            
            // Create the queue and get its URL
            logger.info("Creating SQS queue: {}", queueName);
            String queueUrl = client.createQueue(queueName).getQueueUrl();
            logger.info("SQS queue created: {} with URL: {}", queueName, queueUrl);
            
            // Wait a moment to ensure the queue is fully created
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            logger.error("Error creating SQS queue {}: {}", queueName, e.getMessage(), e);
            // Log error but don't throw exception to allow application to start
        }
    }
    
    /**
     * Waits for a DynamoDB table to become active.
     *
     * @param client The DynamoDB client
     * @param tableName The name of the table to wait for
     */
    private void waitForTableActive(AmazonDynamoDB client, String tableName) {
        logger.info("Waiting for table {} to become active...", tableName);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 60000; // 60 seconds timeout
        
        while (System.currentTimeMillis() < endTime) {
            try {
                TableDescription tableDescription = client.describeTable(tableName).getTable();
                if ("ACTIVE".equals(tableDescription.getTableStatus())) {
                    logger.info("Table {} is now active", tableName);
                    return;
                }
                
                logger.info("Table {} status: {}, waiting...", tableName, tableDescription.getTableStatus());
                Thread.sleep(1000); // Wait 1 second before checking again
            } catch (Exception e) {
                logger.warn("Error checking table status: {}", e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // Log error but don't throw exception to allow application to start
        logger.error("Timeout waiting for table {} to become active", tableName);
    }
}
