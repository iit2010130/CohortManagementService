package com.cohortmgmt;

import com.cohortmgmt.service.rule.CohortRule;
import com.cohortmgmt.service.rule.DailySpendRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for the Cohort Management Service.
 */
@SpringBootApplication
@EnableScheduling
public class CohortManagementServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CohortManagementServiceApplication.class, args);
    }
    
    /**
     * Creates the cohort rules for the service.
     *
     * @return The list of cohort rules
     */
    @Bean
    public List<CohortRule> cohortRules() {
        List<CohortRule> rules = new ArrayList<>();
        rules.add(new DailySpendRule()); // Add the DailySpend rule with default threshold (5000)
        return rules;
    }
}
