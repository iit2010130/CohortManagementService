package com.cohortmgmt.repository;

import com.cohortmgmt.model.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for storing and retrieving customer data.
 */
public interface CustomerRepository {
    
    /**
     * Saves a customer to the repository.
     *
     * @param customer The customer to save
     * @return The saved customer
     */
    Customer save(Customer customer);
    
    /**
     * Finds a customer by ID.
     *
     * @param customerId The ID of the customer to find
     * @return An Optional containing the customer if found, or empty if not found
     */
    Optional<Customer> findById(String customerId);
    
    /**
     * Finds all customers in the repository.
     *
     * @return The list of all customers
     */
    List<Customer> findAll();
    
    /**
     * Deletes a customer from the repository.
     *
     * @param customerId The ID of the customer to delete
     */
    void deleteById(String customerId);
    
    /**
     * Checks if a customer exists in the repository.
     *
     * @param customerId The ID of the customer to check
     * @return true if the customer exists, false otherwise
     */
    boolean existsById(String customerId);
}
