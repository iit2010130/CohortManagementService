package com.cohortmgmt.repository;

import com.cohortmgmt.model.Customer;

/**
 * Repository interface for storing customer data.
 * Minimized to support only the required operations.
 */
public interface CustomerRepository {
    
    /**
     * Saves a customer to the repository.
     * This is the only method needed for the minimal requirements.
     *
     * @param customer The customer to save
     * @return The saved customer
     */
    Customer save(Customer customer);
}
