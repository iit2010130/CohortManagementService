package com.cohortmgmt.config;

import com.cohortmgmt.model.Cohort;
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
        
        @Override
        public Optional<Customer> findById(String customerId) {
            return Optional.ofNullable(customers.get(customerId));
        }
        
        @Override
        public List<Customer> findAll() {
            return new ArrayList<>(customers.values());
        }
        
        @Override
        public void deleteById(String customerId) {
            customers.remove(customerId);
        }
        
        @Override
        public boolean existsById(String customerId) {
            return customers.containsKey(customerId);
        }
    }
    
    /**
     * Mock implementation of the CohortRepository for testing.
     */
    private static class MockCohortRepository implements CohortRepository {
        
        private final Map<String, Cohort> cohorts = new ConcurrentHashMap<>();
        
        @Override
        public Cohort save(Cohort cohort) {
            cohorts.put(cohort.getId(), cohort);
            return cohort;
        }
        
        @Override
        public Optional<Cohort> findById(String cohortId) {
            return Optional.ofNullable(cohorts.get(cohortId));
        }
        
        @Override
        public List<Cohort> findAll() {
            return new ArrayList<>(cohorts.values());
        }
        
        @Override
        public List<Cohort> findByType(CohortType cohortType) {
            return cohorts.values().stream()
                    .filter(cohort -> cohort.getType() == cohortType)
                    .collect(Collectors.toList());
        }
        
        @Override
        public boolean addCustomerToCohort(String cohortId, String customerId) {
            Cohort cohort = cohorts.get(cohortId);
            if (cohort == null) {
                return false;
            }
            cohort.addCustomer(customerId);
            return true;
        }
        
        @Override
        public boolean removeCustomerFromCohort(String cohortId, String customerId) {
            Cohort cohort = cohorts.get(cohortId);
            if (cohort == null) {
                return false;
            }
            return cohort.removeCustomer(customerId);
        }
        
        @Override
        public Set<String> getCustomerIds(String cohortId) {
            Cohort cohort = cohorts.get(cohortId);
            return cohort != null ? cohort.getCustomerIds() : Collections.emptySet();
        }
        
        @Override
        public List<Cohort> findByCustomerId(String customerId) {
            return cohorts.values().stream()
                    .filter(cohort -> cohort.getCustomerIds().contains(customerId))
                    .collect(Collectors.toList());
        }
        
        @Override
        public void deleteById(String cohortId) {
            cohorts.remove(cohortId);
        }
        
        @Override
        public boolean existsById(String cohortId) {
            return cohorts.containsKey(cohortId);
        }
    }
}
