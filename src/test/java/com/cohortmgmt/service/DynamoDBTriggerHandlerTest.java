package com.cohortmgmt.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DynamoDBTriggerHandlerTest {

    @Mock
    private AmazonDynamoDB amazonDynamoDB;

    @Mock
    private CohortService cohortService;

    private DynamoDBTriggerHandler triggerHandler;

    private final String customerTableName = "Customers";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        triggerHandler = new DynamoDBTriggerHandler(amazonDynamoDB, cohortService, customerTableName);
    }

    @Test
    void testPollCustomersTable_ProcessesItems() {
        // Setup mock data
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("customerId", new AttributeValue().withS("customer1"));
        item1.put("dailySpend", new AttributeValue().withN("1000.0"));
        item1.put("userType", new AttributeValue().withS("PAID"));

        Map<String, AttributeValue> item2 = new HashMap<>();
        item2.put("customerId", new AttributeValue().withS("customer2"));
        item2.put("dailySpend", new AttributeValue().withN("2000.0"));
        item2.put("userType", new AttributeValue().withS("FREE"));

        List<Map<String, AttributeValue>> items = Arrays.asList(item1, item2);

        ScanResult scanResult = new ScanResult().withItems(items);
        when(amazonDynamoDB.scan(any(ScanRequest.class))).thenReturn(scanResult);

        // Setup cohort service mock
        Set<CohortType> cohortTypes = new HashSet<>(Arrays.asList(CohortType.NORMAL, CohortType.PREMIUM));
        when(cohortService.classifyCustomer(any(Customer.class))).thenReturn(cohortTypes);

        // Execute
        triggerHandler.pollCustomersTable();

        // Verify
        verify(amazonDynamoDB).scan(any(ScanRequest.class));
        
        // Capture the Customer objects passed to classifyCustomer
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(cohortService, times(2)).classifyCustomer(customerCaptor.capture());
        
        List<Customer> capturedCustomers = customerCaptor.getAllValues();
        assertEquals(2, capturedCustomers.size());
        
        // Verify first customer
        Customer customer1 = capturedCustomers.get(0);
        assertEquals("customer1", customer1.getCustomerId());
        assertEquals(1000.0, customer1.getDailySpend());
        assertEquals(UserType.PAID, customer1.getUserType());
        
        // Verify second customer
        Customer customer2 = capturedCustomers.get(1);
        assertEquals("customer2", customer2.getCustomerId());
        assertEquals(2000.0, customer2.getDailySpend());
        assertEquals(UserType.FREE, customer2.getUserType());
    }

    @Test
    void testPollCustomersTable_SkipsRecentlyProcessedItems() {
        // Setup mock data for first call
        Map<String, AttributeValue> item1 = new HashMap<>();
        item1.put("customerId", new AttributeValue().withS("customer1"));
        item1.put("dailySpend", new AttributeValue().withN("1000.0"));
        item1.put("userType", new AttributeValue().withS("PAID"));

        List<Map<String, AttributeValue>> items = Collections.singletonList(item1);

        ScanResult scanResult = new ScanResult().withItems(items);
        when(amazonDynamoDB.scan(any(ScanRequest.class))).thenReturn(scanResult);

        // Setup cohort service mock
        Set<CohortType> cohortTypes = new HashSet<>(Arrays.asList(CohortType.NORMAL));
        when(cohortService.classifyCustomer(any(Customer.class))).thenReturn(cohortTypes);

        // Execute first time
        triggerHandler.pollCustomersTable();

        // Execute second time immediately (should skip processing)
        triggerHandler.pollCustomersTable();

        // Verify classifyCustomer was called only once
        verify(cohortService, times(1)).classifyCustomer(any(Customer.class));
    }

    @Test
    void testPollCustomersTable_HandlesExceptions() {
        // Setup mock to throw exception
        when(amazonDynamoDB.scan(any(ScanRequest.class))).thenThrow(new RuntimeException("Test exception"));

        // Execute - should not throw exception
        assertDoesNotThrow(() -> triggerHandler.pollCustomersTable());

        // Verify scan was called
        verify(amazonDynamoDB).scan(any(ScanRequest.class));
        
        // Verify classifyCustomer was not called
        verify(cohortService, never()).classifyCustomer(any(Customer.class));
    }
}
