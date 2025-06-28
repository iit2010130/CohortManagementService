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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Arrays;

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
     * Initializes the AWS resources (DynamoDB tables and SQS queue).
     */
    @PostConstruct
    public void init() {
        try {
            createCustomerTable();
            createCohortTable();
            createSqsQueue();
        } catch (Exception e) {
            logger.error("Error initializing AWS resources: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Creates the customer table in DynamoDB.
     */
    private void createCustomerTable() {
        AmazonDynamoDB client = amazonDynamoDB();
        
        try {
            // Check if table already exists
            client.describeTable(customerTableName);
            logger.info("Customer table already exists: {}", customerTableName);
        } catch (ResourceNotFoundException e) {
            // Create table if it doesn't exist
            logger.info("Creating customer table: {}", customerTableName);
            
            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(customerTableName)
                    .withKeySchema(new KeySchemaElement("customerId", KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition("customerId", ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                    .withStreamSpecification(new StreamSpecification()
                            .withStreamEnabled(true)
                            .withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES));
            
            client.createTable(request);
            logger.info("Customer table created: {}", customerTableName);
        }
    }
    
    /**
     * Creates the cohort table in DynamoDB.
     */
    private void createCohortTable() {
        AmazonDynamoDB client = amazonDynamoDB();
        
        try {
            // Check if table already exists
            client.describeTable(cohortTableName);
            logger.info("Cohort table already exists: {}", cohortTableName);
        } catch (ResourceNotFoundException e) {
            // Create table if it doesn't exist
            logger.info("Creating cohort table: {}", cohortTableName);
            
            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(cohortTableName)
                    .withKeySchema(
                            new KeySchemaElement("cohortId", KeyType.HASH))
                    .withAttributeDefinitions(
                            new AttributeDefinition("cohortId", ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
            
            client.createTable(request);
            logger.info("Cohort table created: {}", cohortTableName);
        }
    }
    
    /**
     * Creates the SQS queue.
     */
    private void createSqsQueue() {
        AmazonSQS client = amazonSQS();
        
        try {
            client.createQueue(queueName);
            logger.info("SQS queue created: {}", queueName);
        } catch (Exception e) {
            logger.warn("Error creating SQS queue {}: {}", queueName, e.getMessage());
        }
    }
}
