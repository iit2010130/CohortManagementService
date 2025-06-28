package com.cohortmgmt.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a cohort in the system.
 */
public class Cohort {
    private String id;
    private CohortType type;
    private String description;
    private Set<String> customerIds;

    /**
     * Default constructor for serialization/deserialization.
     */
    public Cohort() {
        this.customerIds = new HashSet<>();
    }

    /**
     * Creates a new cohort with the specified attributes.
     *
     * @param id The unique identifier for the cohort
     * @param type The type of the cohort
     * @param description The description of the cohort
     */
    public Cohort(String id, CohortType type, String description) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.customerIds = new HashSet<>();
    }

    /**
     * Adds a customer to the cohort.
     *
     * @param customerId The ID of the customer to add
     * @return true if the customer was added, false if the customer was already in the cohort
     */
    public boolean addCustomer(String customerId) {
        return customerIds.add(customerId);
    }

    /**
     * Removes a customer from the cohort.
     *
     * @param customerId The ID of the customer to remove
     * @return true if the customer was removed, false if the customer wasn't in the cohort
     */
    public boolean removeCustomer(String customerId) {
        return customerIds.remove(customerId);
    }

    /**
     * Checks if a customer is in the cohort.
     *
     * @param customerId The ID of the customer to check
     * @return true if the customer is in the cohort, false otherwise
     */
    public boolean containsCustomer(String customerId) {
        return customerIds.contains(customerId);
    }

    /**
     * Gets the number of customers in the cohort.
     *
     * @return The number of customers
     */
    public int getCustomerCount() {
        return customerIds.size();
    }

    /**
     * Gets the cohort ID.
     *
     * @return The cohort ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the cohort ID.
     *
     * @param id The cohort ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the cohort type.
     *
     * @return The cohort type
     */
    public CohortType getType() {
        return type;
    }

    /**
     * Sets the cohort type.
     *
     * @param type The cohort type to set
     */
    public void setType(CohortType type) {
        this.type = type;
    }

    /**
     * Gets the cohort description.
     *
     * @return The cohort description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the cohort description.
     *
     * @param description The cohort description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the set of customer IDs in the cohort.
     *
     * @return The set of customer IDs
     */
    public Set<String> getCustomerIds() {
        return new HashSet<>(customerIds);
    }

    /**
     * Sets the set of customer IDs in the cohort.
     *
     * @param customerIds The set of customer IDs to set
     */
    public void setCustomerIds(Set<String> customerIds) {
        this.customerIds = new HashSet<>(customerIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cohort cohort = (Cohort) o;
        return Objects.equals(id, cohort.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Cohort{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", customerCount=" + customerIds.size() +
                '}';
    }
}
