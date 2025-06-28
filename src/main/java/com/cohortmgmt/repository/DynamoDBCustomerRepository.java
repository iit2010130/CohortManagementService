package com.cohortmgmt.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DynamoDB implementation of the CustomerRepository interface.
 * This implementation uses the AWS SDK for Java to interact with DynamoDB.
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
    
    @Autowired
    public DynamoDBCustomerRepository(
            AmazonDynamoDB amazonDynamoDB,
            DynamoDB dynamoDB,
            @Value("${aws.dynamodb.customer-table}") String tableName) {
        this.amazonDynamoDB = amazonDynamoDB;
        this.dynamoDB = dynamoDB;
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
            return customer;
        } catch (Exception e) {
            logger.error("Error saving customer with ID {}: {}", customer.getCustomerId(), e.getMessage(), e);
            throw new RuntimeException("Error saving customer", e);
        }
    }
    
    @Override
    public Optional<Customer> findById(String customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey(CUSTOMER_ID_ATTR, customerId);
            
            Item item = table.getItem(spec);
            
            if (item == null) {
                return Optional.empty();
            }
            
            Customer customer = new Customer();
            customer.setCustomerId(item.getString(CUSTOMER_ID_ATTR));
            customer.setDailySpend(item.getDouble(DAILY_SPEND_ATTR));
            customer.setUserType(UserType.valueOf(item.getString(USER_TYPE_ATTR)));
            
            return Optional.of(customer);
        } catch (Exception e) {
            logger.error("Error finding customer with ID {}: {}", customerId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<Customer> findAll() {
        try {
            List<Customer> customers = new ArrayList<>();
            Table table = dynamoDB.getTable(tableName);
            
            table.scan().forEach(item -> {
                Customer customer = new Customer();
                customer.setCustomerId(item.getString(CUSTOMER_ID_ATTR));
                customer.setDailySpend(item.getDouble(DAILY_SPEND_ATTR));
                customer.setUserType(UserType.valueOf(item.getString(USER_TYPE_ATTR)));
                customers.add(customer);
            });
            
            return customers;
        } catch (Exception e) {
            logger.error("Error finding all customers: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void deleteById(String customerId) {
        if (customerId == null) {
            return;
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            table.deleteItem(CUSTOMER_ID_ATTR, customerId);
            logger.info("Deleted customer with ID: {}", customerId);
        } catch (Exception e) {
            logger.error("Error deleting customer with ID {}: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Error deleting customer", e);
        }
    }
    
    @Override
    public boolean existsById(String customerId) {
        if (customerId == null) {
            return false;
        }
        
        try {
            Table table = dynamoDB.getTable(tableName);
            
            GetItemSpec spec = new GetItemSpec()
                    .withPrimaryKey(CUSTOMER_ID_ATTR, customerId);
            
            Item item = table.getItem(spec);
            
            return item != null;
        } catch (Exception e) {
            logger.error("Error checking if customer exists with ID {}: {}", customerId, e.getMessage(), e);
            return false;
        }
    }
}
