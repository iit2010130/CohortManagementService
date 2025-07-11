package com.cohortmgmt.config;

import com.cohortmgmt.service.CohortServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Configuration class for initializing application data.
 */
@Configuration
public class DataInitializer {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    /**
     * CommandLineRunner to initialize cohort types after AWS resources are created.
     * This runs after the AWS resources initialization.
     *
     * @param cohortService The cohort service
     * @return A CommandLineRunner that initializes cohort types
     */
    @Bean
    @Order(2) // Run after AWS resources initialization (which is Order(1))
    public CommandLineRunner initCohortTypes(CohortServiceImpl cohortService) {
        return args -> {
            try {
                // Add a longer delay to ensure tables are fully created
                logger.info("Waiting for tables to be fully created...");
                Thread.sleep(5000); // 5 seconds delay
                
                logger.info("Initializing cohort types...");
                cohortService.initializeCohortTypes();
                logger.info("Cohort types initialized successfully");
            } catch (Exception e) {
                logger.error("Error initializing cohort types: {}", e.getMessage(), e);
            }
        };
    }
}
