package com.cohortmgmt.model;

import java.util.Objects;

/**
 * Represents a customer in the system.
 */
public class Customer {
    private String customerId;
    private Double dailySpend;
    private UserType userType;

    /**
     * Default constructor for serialization/deserialization.
     */
    public Customer() {
    }

    /**
     * Creates a new customer with the specified attributes.
     *
     * @param customerId The unique identifier for the customer
     * @param dailySpend The daily spend amount of the customer
     * @param userType The type of the customer (PAID or FREE)
     */
    public Customer(String customerId, Double dailySpend, UserType userType) {
        this.customerId = customerId;
        this.dailySpend = dailySpend;
        this.userType = userType;
    }

    /**
     * Gets the customer ID.
     *
     * @return The customer ID
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer ID.
     *
     * @param customerId The customer ID to set
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the daily spend amount.
     *
     * @return The daily spend amount
     */
    public Double getDailySpend() {
        return dailySpend;
    }

    /**
     * Sets the daily spend amount.
     *
     * @param dailySpend The daily spend amount to set
     */
    public void setDailySpend(Double dailySpend) {
        this.dailySpend = dailySpend;
    }

    /**
     * Gets the user type.
     *
     * @return The user type
     */
    public UserType getUserType() {
        return userType;
    }

    /**
     * Sets the user type.
     *
     * @param userType The user type to set
     */
    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId='" + customerId + '\'' +
                ", dailySpend=" + dailySpend +
                ", userType=" + userType +
                '}';
    }
}
