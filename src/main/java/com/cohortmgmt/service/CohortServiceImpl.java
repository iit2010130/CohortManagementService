package com.cohortmgmt.service;

import com.cohortmgmt.model.Cohort;
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
import java.util.stream.Collectors;

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
        // Removed initializeCohorts() from constructor to avoid startup issues
    }
    
    /**
     * We no longer need this method since we're using CommandLineRunner for initialization
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // No longer needed - initialization is done by DataInitializer
    }
    
    /**
     * Initializes the cohorts based on the configured rules.
     * Made public so it can be called from DataInitializer.
     */
    public void initializeCohorts() {
        for (CohortRule rule : rules) {
            String cohortId = rule.getName() + "_" + rule.getCohortType().name();
            if (!cohortRepository.existsById(cohortId)) {
                Cohort cohort = new Cohort(cohortId, rule.getCohortType(), 
                        "Cohort for " + rule.getName() + " rule with type " + rule.getCohortType().name());
                cohortRepository.save(cohort);
            }
        }
    }
    
    /**
     * Adds a rule to the service.
     *
     * @param rule The rule to add
     */
    public void addRule(CohortRule rule) {
        rules.add(rule);
        String cohortId = rule.getName() + "_" + rule.getCohortType().name();
        if (!cohortRepository.existsById(cohortId)) {
            Cohort cohort = new Cohort(cohortId, rule.getCohortType(), 
                    "Cohort for " + rule.getName() + " rule with type " + rule.getCohortType().name());
            cohortRepository.save(cohort);
        }
    }
    
    @Override
    public Set<String> classifyCustomer(Customer customer) {
        if (customer == null) {
            logger.warn("Cannot classify null customer");
            return Collections.emptySet();
        }
        
        Set<String> cohortIds = new HashSet<>();
        
        for (CohortRule rule : rules) {
            try {
                if (rule.evaluate(customer)) {
                    String cohortId = rule.getName() + "_" + rule.getCohortType().name();
                    Optional<Cohort> cohortOpt = cohortRepository.findById(cohortId);
                    if (cohortOpt.isPresent()) {
                        cohortRepository.addCustomerToCohort(cohortId, customer.getCustomerId());
                        cohortIds.add(cohortId);
                    }
                }
            } catch (Exception e) {
                logger.error("Error evaluating rule {} for customer {}: {}", 
                        rule.getName(), customer.getCustomerId(), e.getMessage(), e);
            }
        }
        
        return cohortIds;
    }
    
    @Override
    public boolean isCustomerInCohortType(String customerId, CohortType cohortType) {
        if (customerId == null || cohortType == null) {
            return false;
        }
        
        // Get all cohorts for the customer
        List<Cohort> cohorts = cohortRepository.findByCustomerId(customerId);
        
        // Check if any of the cohorts match the specified type
        for (Cohort cohort : cohorts) {
            if (cohort.getType() == cohortType) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public List<Cohort> getCustomerCohorts(String customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        
        return cohortRepository.findByCustomerId(customerId);
    }
    
    @Override
    public Set<String> getCohortCustomerIdsByType(CohortType cohortType) {
        if (cohortType == null) {
            return Collections.emptySet();
        }
        
        Set<String> customerIds = new HashSet<>();
        
        // Get all cohorts
        List<Cohort> allCohorts = new ArrayList<>();
        
        // Since we don't have findAll() or findByType() anymore, we need to use a different approach
        // We'll use the cohort IDs from the rules to find all cohorts
        for (CohortRule rule : rules) {
            if (rule.getCohortType() == cohortType) {
                String cohortId = rule.getName() + "_" + rule.getCohortType().name();
                Optional<Cohort> cohortOpt = cohortRepository.findById(cohortId);
                if (cohortOpt.isPresent()) {
                    allCohorts.add(cohortOpt.get());
                }
            }
        }
        
        // Collect all customer IDs from the cohorts
        for (Cohort cohort : allCohorts) {
            customerIds.addAll(cohort.getCustomerIds());
        }
        
        return customerIds;
    }
}
