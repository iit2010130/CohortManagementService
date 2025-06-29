package com.cohortmgmt.service;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.repository.CohortRepository;
import com.cohortmgmt.service.rule.CohortRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Implementation of the CohortService interface.
 */
@Service
public class CohortServiceImpl implements CohortService, ApplicationListener<ContextRefreshedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(CohortServiceImpl.class);
    
    private final CohortRepository cohortRepository;
    private final List<CohortRule> rules = new ArrayList<>();
    
    /**
     * Creates a new CohortServiceImpl with the specified rules and repository.
     *
     * @param rules The rules to use for classification
     * @param cohortRepository The repository for storing cohort data
     */
    @Autowired
    public CohortServiceImpl(List<CohortRule> rules, CohortRepository cohortRepository) {
        this.cohortRepository = cohortRepository;
        if (rules != null) {
            this.rules.addAll(rules);
        }
        // Removed initialization from constructor to avoid startup issues
    }
    
    /**
     * We no longer need this method since we're using CommandLineRunner for initialization
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // No longer needed - initialization is done by DataInitializer
    }
    
    /**
     * Initializes the cohort types based on the configured rules.
     * Made public so it can be called from DataInitializer.
     */
    @Override
    public void initializeCohortTypes() {
        // No initialization needed for cohort types since they are enum values
        logger.info("Cohort types initialized");
    }
    
    /**
     * Adds a rule to the service.
     *
     * @param rule The rule to add
     */
    public void addRule(CohortRule rule) {
        rules.add(rule);
        logger.info("Added rule: {} for cohort type: {}", rule.getName(), rule.getCohortType());
    }
    
    @Override
    public Set<CohortType> classifyCustomer(Customer customer) {
        if (customer == null) {
            logger.warn("Cannot classify null customer");
            return Collections.emptySet();
        }
        
        Set<CohortType> cohortTypes = new HashSet<>();
        
        for (CohortRule rule : rules) {
            try {
                if (rule.evaluate(customer)) {
                    CohortType cohortType = rule.getCohortType();
                    cohortRepository.addCustomerToCohortType(cohortType, customer.getCustomerId());
                    cohortTypes.add(cohortType);
                    logger.info("Customer {} classified into cohort type {}", customer.getCustomerId(), cohortType);
                }
            } catch (Exception e) {
                logger.error("Error evaluating rule {} for customer {}: {}", 
                        rule.getName(), customer.getCustomerId(), e.getMessage(), e);
            }
        }
        
        return cohortTypes;
    }
    
    @Override
    public boolean isCustomerInCohortType(String customerId, CohortType cohortType) {
        if (customerId == null || cohortType == null) {
            return false;
        }
        
        return cohortRepository.isCustomerInCohortType(customerId, cohortType);
    }
    
    @Override
    public List<CohortType> getCustomerCohortTypes(String customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        
        return cohortRepository.findCohortTypesByCustomerId(customerId);
    }
    
    @Override
    public Set<String> getCustomerIdsByCohortType(CohortType cohortType) {
        if (cohortType == null) {
            return Collections.emptySet();
        }
        
        return cohortRepository.getCustomerIdsByCohortType(cohortType);
    }
}
