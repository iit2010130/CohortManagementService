package com.cohortmgmt.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.model.DescribeStreamRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeStreamResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.GetRecordsRequest;
import com.amazonaws.services.dynamodbv2.model.GetRecordsResult;
import com.amazonaws.services.dynamodbv2.model.GetShardIteratorRequest;
import com.amazonaws.services.dynamodbv2.model.GetShardIteratorResult;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.Shard;
import com.amazonaws.services.dynamodbv2.model.ShardIteratorType;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for listening to DynamoDB streams and forwarding events to the DynamoDBStreamProcessingService.
 */
@Service
public class DynamoDBStreamListener {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBStreamListener.class);
    
    private final AmazonDynamoDB amazonDynamoDB;
    private final DynamoDBStreamProcessingService streamProcessingService;
    private final String customerTableName;
    private final String region;
    private final String endpoint;
    
    private AmazonDynamoDBStreams streamsClient;
    private String streamArn;
    private final ConcurrentHashMap<String, String> shardIterators = new ConcurrentHashMap<>();
    
    @Autowired
    public DynamoDBStreamListener(
            AmazonDynamoDB amazonDynamoDB,
            DynamoDBStreamProcessingService streamProcessingService,
            @Value("${aws.dynamodb.customer-table}") String customerTableName,
            @Value("${aws.region}") String region,
            @Value("${aws.endpoint}") String endpoint) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.streamProcessingService = streamProcessingService;
        this.customerTableName = customerTableName;
        this.region = region;
        this.endpoint = endpoint;
    }
    
    @PostConstruct
    public void initialize() {
        // Create a DynamoDB Streams client
        streamsClient = AmazonDynamoDBStreamsClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy")))
                .build();
        
        // Schedule a delayed initialization to ensure tables are created first
        scheduleInitialization();
    }
    
    /**
     * Schedule initialization with a delay to ensure tables are created first.
     */
    private void scheduleInitialization() {
        new Thread(() -> {
            try {
                // Wait for tables to be created
                logger.info("Waiting for tables to be created before initializing DynamoDB Stream listener...");
                Thread.sleep(5000); // 5 seconds delay
                
                // Try to initialize
                initializeStreamListener();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting to initialize DynamoDB Stream listener", e);
            }
        }).start();
    }
    
    /**
     * Initialize the stream listener after tables are created.
     */
    private void initializeStreamListener() {
        try {
            // Get the stream ARN for the customer table using list-streams API
            com.amazonaws.services.dynamodbv2.model.ListStreamsRequest listStreamsRequest = new com.amazonaws.services.dynamodbv2.model.ListStreamsRequest()
                    .withTableName(customerTableName);
            
            com.amazonaws.services.dynamodbv2.model.ListStreamsResult listStreamsResult = streamsClient.listStreams(listStreamsRequest);
            
            List<String> streamArns = listStreamsResult.getStreams().stream()
                    .map(com.amazonaws.services.dynamodbv2.model.Stream::getStreamArn)
                    .collect(java.util.stream.Collectors.toList());
            
            if (streamArns.isEmpty()) {
                logger.warn("No streams found for table: {}", customerTableName);
                // Schedule a retry
                scheduleRetry();
                return;
            }
            
            // Use the first stream ARN (there should only be one for the table)
            streamArn = streamArns.get(0);
            
            logger.info("Initialized DynamoDB Stream listener for table: {} with stream ARN: {}", 
                    customerTableName, streamArn);
            
            // Initialize shard iterators
            initializeShardIterators();
        } catch (ResourceNotFoundException e) {
            logger.warn("Table {} not found yet, will retry initialization later", customerTableName);
            // Schedule a retry
            scheduleRetry();
        } catch (Exception e) {
            logger.error("Error initializing DynamoDB Stream listener: {}", e.getMessage(), e);
            // Schedule a retry
            scheduleRetry();
        }
    }
    
    /**
     * Schedule a retry for initialization.
     */
    private void scheduleRetry() {
        new Thread(() -> {
            try {
                // Wait before retrying
                Thread.sleep(10000); // 10 seconds delay
                
                // Try to initialize again
                logger.info("Retrying DynamoDB Stream listener initialization...");
                initializeStreamListener();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting to retry DynamoDB Stream listener initialization", e);
            }
        }).start();
    }
    
    private void initializeShardIterators() {
        try {
            // Describe the stream to get the shards
            DescribeStreamResult describeStreamResult = streamsClient.describeStream(
                    new DescribeStreamRequest().withStreamArn(streamArn));
            
            List<Shard> shards = describeStreamResult.getStreamDescription().getShards();
            
            for (Shard shard : shards) {
                // Get a shard iterator for each shard
                GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest()
                        .withStreamArn(streamArn)
                        .withShardId(shard.getShardId())
                        .withShardIteratorType(ShardIteratorType.TRIM_HORIZON);
                
                GetShardIteratorResult getShardIteratorResult = streamsClient.getShardIterator(getShardIteratorRequest);
                String shardIterator = getShardIteratorResult.getShardIterator();
                
                // Store the shard iterator
                shardIterators.put(shard.getShardId(), shardIterator);
                
                logger.info("Initialized shard iterator for shard: {}", shard.getShardId());
            }
        } catch (Exception e) {
            logger.error("Error initializing shard iterators: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Polls the DynamoDB stream for new records.
     * This method is scheduled to run every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    public void pollStream() {
        if (streamArn == null || shardIterators.isEmpty()) {
            logger.debug("Stream ARN or shard iterators not initialized yet, skipping poll");
            return;
        }
        
        try {
            logger.debug("Polling DynamoDB stream with ARN: {}", streamArn);
            
            // Process each shard
            for (String shardId : shardIterators.keySet()) {
                String shardIterator = shardIterators.get(shardId);
                
                if (shardIterator == null) {
                    logger.debug("Shard iterator is null for shard: {}", shardId);
                    continue;
                }
                
                // Get records from the shard
                GetRecordsRequest getRecordsRequest = new GetRecordsRequest()
                        .withShardIterator(shardIterator)
                        .withLimit(100);
                
                logger.debug("Getting records from shard: {}", shardId);
                GetRecordsResult getRecordsResult = streamsClient.getRecords(getRecordsRequest);
                List<Record> records = getRecordsResult.getRecords();
                
                logger.debug("Got {} records from shard: {}", records.size(), shardId);
                
                // Process each record
                for (Record record : records) {
                    logger.debug("Processing record: {}", record);
                    streamProcessingService.processRecord(record);
                }
                
                // Update the shard iterator for the next poll
                shardIterators.put(shardId, getRecordsResult.getNextShardIterator());
            }
        } catch (Exception e) {
            logger.error("Error polling DynamoDB stream: {}", e.getMessage(), e);
        }
    }
}
