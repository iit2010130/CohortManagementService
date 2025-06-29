package com.cohortmgmt.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Initializes AWS resources when the application starts.
 * This ensures that required resources like SQS queues exist before they are used.
 */
@Component
public class AwsResourceInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsResourceInitializer.class);
    
    private final AmazonSQS amazonSQS;
    private final String queueName;
    
    @Autowired
    public AwsResourceInitializer(AmazonSQS amazonSQS, 
                                @Value("${aws.sqs.queue-name}") String queueName) {
        this.amazonSQS = amazonSQS;
        this.queueName = queueName;
    }
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        createSqsQueueIfNotExists();
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
