package com.cohortmgmt.config;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.service.rule.CohortRule;
import com.cohortmgmt.service.rule.DailySpendRule;
import com.cohortmgmt.service.rule.MidSpendRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating cohort rules from configuration.
 */
@Component
public class CohortRuleFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(CohortRuleFactory.class);
    
    /**
     * Creates cohort rules from the provided configuration.
     *
     * @param properties The rule configuration properties
     * @return A list of cohort rules
     */
    public List<CohortRule> createRules(CohortRuleProperties properties) {
        List<CohortRule> rules = new ArrayList<>();
        
        if (!properties.isEnabled()) {
            logger.info("Custom rule configuration is disabled. Using default rules.");
            return getDefaultRules();
        }
        
        for (CohortRuleProperties.RuleConfig config : properties.getConfigurations()) {
            try {
                CohortRule rule = createRule(config);
                if (rule != null) {
                    rules.add(rule);
                    logger.info("Created rule: {} for cohort type: {}", rule.getName(), rule.getCohortType());
                }
            } catch (Exception e) {
                logger.error("Error creating rule from configuration: {}", e.getMessage(), e);
            }
        }
        
        // If no valid rules were created, use defaults
        if (rules.isEmpty()) {
            logger.warn("No valid rules were created from configuration. Using default rules.");
            return getDefaultRules();
        }
        
        return rules;
    }
    
    /**
     * Creates a single rule from the provided configuration.
     *
     * @param config The rule configuration
     * @return The created rule, or null if the configuration is invalid
     */
    private CohortRule createRule(CohortRuleProperties.RuleConfig config) {
        if (config.getType() == null) {
            logger.warn("Rule type is required");
            return null;
        }
        
        CohortType cohortType = config.getCohortType();
        
        switch (config.getType().toLowerCase()) {
            case "daily-spend":
                Double threshold = config.getMaxThreshold();
                if (threshold == null) {
                    // Use default threshold
                    return new DailySpendRule();
                } else {
                    return new DailySpendRule(threshold);
                }
                
            case "mid-spend":
                if (cohortType == null) {
                    // Use default cohort type (NORMAL)
                    return new MidSpendRule();
                } else {
                    return new MidSpendRule(cohortType);
                }
                
            case "custom-rule":
                // Custom rule implementation
                // This allows users to define custom rules in the application.yml file
                // without having to modify the code
                if (cohortType == null) {
                    logger.warn("Cohort type is required for custom rules");
                    return null;
                }
                
                // Create a custom rule based on the configuration
                return createCustomRule(config);
                
            default:
                logger.warn("Unknown rule type: {}", config.getType());
                return null;
        }
    }
    
    /**
     * Creates a custom rule from the provided configuration.
     *
     * @param config The rule configuration
     * @return The created custom rule
     */
    private CohortRule createCustomRule(CohortRuleProperties.RuleConfig config) {
        final CohortType cohortType = config.getCohortType();
        final Double minThreshold = config.getMinThreshold();
        final Double maxThreshold = config.getMaxThreshold();
        final Boolean requirePaidUser = config.getRequirePaidUser();
        
        // Create a custom rule implementation
        return new CohortRule() {
            @Override
            public String getName() {
                return "CustomRule-" + cohortType;
            }
            
            @Override
            public CohortType getCohortType() {
                return cohortType;
            }
            
            @Override
            public boolean evaluate(com.cohortmgmt.model.Customer customer) {
                if (customer == null) {
                    return false;
                }
                
                // Check if the customer meets the minimum threshold
                if (minThreshold != null && customer.getDailySpend() < minThreshold) {
                    return false;
                }
                
                // Check if the customer meets the maximum threshold
                if (maxThreshold != null && customer.getDailySpend() > maxThreshold) {
                    return false;
                }
                
                // Check if the customer is a paid user if required
                if (requirePaidUser != null && requirePaidUser && 
                        customer.getUserType() != com.cohortmgmt.model.UserType.PAID) {
                    return false;
                }
                
                // All conditions met
                return true;
            }
        };
    }
    
    /**
     * Gets the default rules.
     *
     * @return A list of default cohort rules
     */
    private List<CohortRule> getDefaultRules() {
        List<CohortRule> rules = new ArrayList<>();
        rules.add(new DailySpendRule());
        rules.add(new MidSpendRule());
        rules.add(new MidSpendRule(CohortType.PREMIUM));
        return rules;
    }
}
