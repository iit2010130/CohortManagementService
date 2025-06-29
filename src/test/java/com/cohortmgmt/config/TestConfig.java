package com.cohortmgmt.config;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.repository.CohortRepository;
import com.cohortmgmt.repository.CustomerRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Test configuration for the Cohort Management Service.
 * This class provides mock implementations of the repositories for testing.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
    /**
     * Creates a mock implementation of the CustomerRepository for testing.
     *
     * @return The mock CustomerRepository
     */
    @Bean
    @Primary
    public CustomerRepository customerRepository() {
        return new MockCustomerRepository();
    }
    
    /**
     * Creates a mock implementation of the CohortRepository for testing.
     *
     * @return The mock CohortRepository
     */
    @Bean
    @Primary
    public CohortRepository cohortRepository() {
        return new MockCohortRepository();
    }
    
    /**
     * Mock implementation of the CustomerRepository for testing.
     */
    private static class MockCustomerRepository implements CustomerRepository {
        
        private final Map<String, Customer> customers = new ConcurrentHashMap<>();
        
        @Override
        public Customer save(Customer customer) {
            customers.put(customer.getCustomerId(), customer);
            return customer;
        }
        
        // Additional methods for testing purposes
        public Optional<Customer> findById(String customerId) {
            return Optional.ofNullable(customers.get(customerId));
        }
        
        public List<Customer> findAll() {
            return new ArrayList<>(customers.values());
        }
        
        public void deleteById(String customerId) {
            customers.remove(customerId);
        }
        
        public boolean existsById(String customerId) {
            return customers.containsKey(customerId);
        }
    }
    
    /**
     * Mock implementation of the CohortRepository for testing.
     */
    private static class MockCohortRepository implements CohortRepository {
        
        private final Map<CohortType, Set<String>> cohortTypeToCustomerIds = new ConcurrentHashMap<>();
        private final Map<String, Set<CohortType>> customerIdToCohortTypes = new ConcurrentHashMap<>();
        
        @Override
        public boolean addCustomerToCohortType(CohortType cohortType, String customerId) {
            // Add to cohortTypeToCustomerIds
            cohortTypeToCustomerIds.computeIfAbsent(cohortType, k -> new HashSet<>()).add(customerId);
            
            // Add to customerIdToCohortTypes
            customerIdToCohortTypes.computeIfAbsent(customerId, k -> new HashSet<>()).add(cohortType);
            
            return true;
        }
        
        @Override
        public Set<String> getCustomerIdsByCohortType(CohortType cohortType) {
            return cohortTypeToCustomerIds.getOrDefault(cohortType, Collections.emptySet());
        }
        
        @Override
        public List<CohortType> findCohortTypesByCustomerId(String customerId) {
            Set<CohortType> cohortTypes = customerIdToCohortTypes.getOrDefault(customerId, Collections.emptySet());
            return new ArrayList<>(cohortTypes);
        }
        
        @Override
        public boolean isCustomerInCohortType(String customerId, CohortType cohortType) {
            Set<CohortType> cohortTypes = customerIdToCohortTypes.getOrDefault(customerId, Collections.emptySet());
            return cohortTypes.contains(cohortType);
        }
    }
}
