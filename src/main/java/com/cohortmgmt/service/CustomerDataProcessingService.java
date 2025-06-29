package com.cohortmgmt.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import com.cohortmgmt.repository.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Service for processing customer data from SQS and classifying them into cohorts.
 */
@Service
public class CustomerDataProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerDataProcessingService.class);
    
    private final AmazonSQS amazonSQS;
    private final CustomerRepository customerRepository;
    private final CohortService cohortService;
    private final ObjectMapper objectMapper;
    private final String queueName;
    private final String endpoint;
    
    @Autowired
    public CustomerDataProcessingService(
            AmazonSQS amazonSQS,
            CustomerRepository customerRepository,
            CohortService cohortService,
            @Value("${aws.sqs.queue-name}") String queueName,
            @Value("${aws.endpoint}") String endpoint) {
        this.amazonSQS = amazonSQS;
        this.customerRepository = customerRepository;
        this.cohortService = cohortService;
        this.objectMapper = new ObjectMapper();
        this.queueName = queueName;
        this.endpoint = endpoint;
    }
    
    /**
     * Processes customer data from SQS.
     * This method is scheduled to run every 10 seconds.
     */
    @Scheduled(fixedDelay = 10000)
    public void processCustomerData() {
        try {
            // First, ensure the queue exists
            String queueUrl = ensureQueueExists();
            if (queueUrl == null) {
                // Queue doesn't exist and couldn't be created, skip processing
                return;
            }
            
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMaxNumberOfMessages(10)
                    .withWaitTimeSeconds(5);
            
            List<Message> messages = amazonSQS.receiveMessage(receiveMessageRequest).getMessages();
            
            for (Message message : messages) {
                try {
                    processMessage(message);
                    amazonSQS.deleteMessage(queueUrl, message.getReceiptHandle());
                } catch (Exception e) {
                    logger.error("Error processing message: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            // Log at debug level to avoid filling logs with expected errors during startup
            logger.debug("Error receiving messages from SQS: {}", e.getMessage());
        }
    }
    
    /**
     * Ensures that the SQS queue exists, creating it if necessary.
     * 
     * @return The queue URL if the queue exists or was created, null otherwise
     */
    private String ensureQueueExists() {
        try {
            // Try to get the queue URL from the service first
            try {
                String queueUrl = amazonSQS.getQueueUrl(queueName).getQueueUrl();
                logger.debug("Queue exists, URL: {}", queueUrl);
                return queueUrl;
            } catch (Exception e) {
                // Queue doesn't exist, try to create it
                logger.debug("Queue doesn't exist, creating: {}", queueName);
                String queueUrl = amazonSQS.createQueue(queueName).getQueueUrl();
                logger.info("Created SQS queue: {} with URL: {}", queueName, queueUrl);
                return queueUrl;
            }
        } catch (Exception e) {
            // If all else fails, use the constructed URL for LocalStack
            String constructedUrl = endpoint + "/000000000000/" + queueName;
            logger.debug("Using constructed queue URL: {}", constructedUrl);
            return constructedUrl;
        }
    }
    
    /**
     * Processes a single message from SQS.
     *
     * @param message The message to process
     * @throws Exception If an error occurs while processing the message
     */
    private void processMessage(Message message) throws Exception {
        String messageBody = message.getBody();
        JsonNode jsonNode = objectMapper.readTree(messageBody);
        
        String customerId = jsonNode.get("customerId").asText();
        Double dailySpend = jsonNode.get("dailySpend").asDouble();
        UserType userType = UserType.valueOf(jsonNode.get("userType").asText());
        
        Customer customer = new Customer(customerId, dailySpend, userType);
        
        // Save the customer to the repository
        customerRepository.save(customer);
        
        // Classify the customer into cohort types
        Set<CohortType> cohortTypes = cohortService.classifyCustomer(customer);
        
        logger.info("Processed customer {} and classified into cohort types: {}", customerId, cohortTypes);
    }
}
