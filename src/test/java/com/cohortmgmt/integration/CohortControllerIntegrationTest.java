package com.cohortmgmt.integration;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import com.cohortmgmt.repository.CohortRepository;
import com.cohortmgmt.repository.CustomerRepository;
import com.cohortmgmt.service.CohortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the CohortController.
 * These tests use a mock MVC to test the controller endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CohortControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private CohortService cohortService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private CohortRepository cohortRepository;
    
    private Customer premiumCustomer;
    private Customer normalCustomer;
    
    @BeforeEach
    public void setup() {
        // Create test customers
        premiumCustomer = new Customer("premium-customer", 6000.0, UserType.PAID);
        normalCustomer = new Customer("normal-customer", 3000.0, UserType.FREE);
        
        // Save customers to repository
        customerRepository.save(premiumCustomer);
        customerRepository.save(normalCustomer);
        
        // Add premium customer to PREMIUM cohort type
        cohortRepository.addCustomerToCohortType(CohortType.PREMIUM, premiumCustomer.getCustomerId());
    }
    
    @Test
    public void testIsCustomerInCohortType_True() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cohorts/check")
                .param("customerId", premiumCustomer.getCustomerId())
                .param("cohortType", CohortType.PREMIUM.name())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
    
    @Test
    public void testIsCustomerInCohortType_False() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cohorts/check")
                .param("customerId", normalCustomer.getCustomerId())
                .param("cohortType", CohortType.PREMIUM.name())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
    
    @Test
    public void testGetCustomerCohortTypes() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cohorts/customer/{customerId}", 
                premiumCustomer.getCustomerId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is(CohortType.PREMIUM.name())));
    }
    
    @Test
    public void testGetCustomerCohortTypes_Empty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cohorts/customer/{customerId}", 
                normalCustomer.getCustomerId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
    
    @Test
    public void testGetCustomerIdsByCohortType() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cohorts/type/{cohortType}/customers", 
                CohortType.PREMIUM.name())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$", hasItem(premiumCustomer.getCustomerId())));
    }
    
    @Test
    public void testGetCustomerIdsByCohortType_Empty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cohorts/type/{cohortType}/customers", 
                CohortType.FRAUD.name())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
    
    @Test
    public void testIsCustomerInCohortType_InvalidCustomerId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cohorts/check")
                .param("customerId", "")
                .param("cohortType", CohortType.PREMIUM.name())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid argument")));
    }
    
    @Test
    public void testIsCustomerInCohortType_MissingParameter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/cohorts/check")
                .param("customerId", premiumCustomer.getCustomerId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Missing parameter")));
    }
}
