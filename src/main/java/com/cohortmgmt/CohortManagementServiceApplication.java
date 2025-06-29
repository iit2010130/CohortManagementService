package com.cohortmgmt;

import com.cohortmgmt.config.CohortRuleFactory;
import com.cohortmgmt.config.CohortRuleProperties;
import com.cohortmgmt.service.rule.CohortRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * Main application class for the Cohort Management Service.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CohortRuleProperties.class)
public class CohortManagementServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CohortManagementServiceApplication.class, args);
    }
    
    /**
     * Creates the cohort rules for the service.
     * Rules can be configured via application properties or will use defaults if not configured.
     *
     * @param ruleFactory The factory for creating rules from configuration
     * @param properties The rule configuration properties
     * @return The list of cohort rules
     */
    @Bean
    public List<CohortRule> cohortRules(CohortRuleFactory ruleFactory, CohortRuleProperties properties) {
        return ruleFactory.createRules(properties);
    }
}
