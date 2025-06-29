package com.cohortmgmt.config;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import com.cohortmgmt.service.rule.CohortRule;
import com.cohortmgmt.service.rule.DailySpendRule;
import com.cohortmgmt.service.rule.MidSpendRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CohortRuleFactoryTest {

    private CohortRuleFactory ruleFactory;
    private CohortRuleProperties properties;

    @BeforeEach
    void setUp() {
        ruleFactory = new CohortRuleFactory();
        properties = new CohortRuleProperties();
        properties.setEnabled(true);
    }

    @Test
    void testCreateRules_DefaultRules() {
        // Setup - empty configurations
        properties.setConfigurations(new ArrayList<>());

        // Execute
        List<CohortRule> rules = ruleFactory.createRules(properties);

        // Verify
        assertNotNull(rules);
        assertEquals(3, rules.size());
        
        // Verify default rules
        boolean foundDailySpendRule = false;
        boolean foundMidSpendRuleNormal = false;
        boolean foundMidSpendRulePremium = false;
        
        for (CohortRule rule : rules) {
            if (rule instanceof DailySpendRule) {
                foundDailySpendRule = true;
            } else if (rule instanceof MidSpendRule) {
                if (rule.getCohortType() == CohortType.NORMAL) {
                    foundMidSpendRuleNormal = true;
                } else if (rule.getCohortType() == CohortType.PREMIUM) {
                    foundMidSpendRulePremium = true;
                }
            }
        }
        
        assertTrue(foundDailySpendRule, "Default DailySpendRule not found");
        assertTrue(foundMidSpendRuleNormal, "Default MidSpendRule for NORMAL not found");
        assertTrue(foundMidSpendRulePremium, "Default MidSpendRule for PREMIUM not found");
    }

    @Test
    void testCreateRules_CustomRule() {
        // Setup - add a custom rule configuration
        List<CohortRuleProperties.RuleConfig> configs = new ArrayList<>();
        
        CohortRuleProperties.RuleConfig customRuleConfig = new CohortRuleProperties.RuleConfig();
        customRuleConfig.setType("custom-rule");
        customRuleConfig.setCohortType(CohortType.VIP);
        customRuleConfig.setMinThreshold(1000.0);
        customRuleConfig.setMaxThreshold(3000.0);
        customRuleConfig.setRequirePaidUser(true);
        
        configs.add(customRuleConfig);
        properties.setConfigurations(configs);

        // Execute
        List<CohortRule> rules = ruleFactory.createRules(properties);

        // Verify
        assertNotNull(rules);
        assertEquals(1, rules.size());
        
        CohortRule customRule = rules.get(0);
        assertEquals(CohortType.VIP, customRule.getCohortType());
        assertEquals("CustomRule-VIP", customRule.getName());
        
        // Test rule evaluation
        Customer paidCustomerInRange = new Customer("customer1", 2000.0, UserType.PAID);
        Customer paidCustomerBelowRange = new Customer("customer2", 500.0, UserType.PAID);
        Customer paidCustomerAboveRange = new Customer("customer3", 4000.0, UserType.PAID);
        Customer freeCustomerInRange = new Customer("customer4", 2000.0, UserType.FREE);
        
        assertTrue(customRule.evaluate(paidCustomerInRange), "Paid customer in range should match");
        assertFalse(customRule.evaluate(paidCustomerBelowRange), "Paid customer below range should not match");
        assertFalse(customRule.evaluate(paidCustomerAboveRange), "Paid customer above range should not match");
        assertFalse(customRule.evaluate(freeCustomerInRange), "Free customer in range should not match");
    }

    @Test
    void testCreateRules_DisabledConfiguration() {
        // Setup - disabled configuration
        properties.setEnabled(false);
        
        // Add a custom rule that should be ignored
        List<CohortRuleProperties.RuleConfig> configs = new ArrayList<>();
        
        CohortRuleProperties.RuleConfig customRuleConfig = new CohortRuleProperties.RuleConfig();
        customRuleConfig.setType("custom-rule");
        customRuleConfig.setCohortType(CohortType.VIP);
        
        configs.add(customRuleConfig);
        properties.setConfigurations(configs);

        // Execute
        List<CohortRule> rules = ruleFactory.createRules(properties);

        // Verify - should return default rules
        assertNotNull(rules);
        assertEquals(3, rules.size());
    }

    @Test
    void testCreateRules_InvalidRuleType() {
        // Setup - add an invalid rule configuration
        List<CohortRuleProperties.RuleConfig> configs = new ArrayList<>();
        
        CohortRuleProperties.RuleConfig invalidRuleConfig = new CohortRuleProperties.RuleConfig();
        invalidRuleConfig.setType("invalid-rule-type");
        invalidRuleConfig.setCohortType(CohortType.NORMAL);
        
        configs.add(invalidRuleConfig);
        properties.setConfigurations(configs);

        // Execute
        List<CohortRule> rules = ruleFactory.createRules(properties);

        // Verify - should return default rules since no valid rules were created
        assertNotNull(rules);
        assertEquals(3, rules.size());
    }

    @Test
    void testCreateRules_CustomRuleWithoutCohortType() {
        // Setup - add a custom rule without cohort type
        List<CohortRuleProperties.RuleConfig> configs = new ArrayList<>();
        
        CohortRuleProperties.RuleConfig invalidCustomRuleConfig = new CohortRuleProperties.RuleConfig();
        invalidCustomRuleConfig.setType("custom-rule");
        // No cohort type set
        
        configs.add(invalidCustomRuleConfig);
        properties.setConfigurations(configs);

        // Execute
        List<CohortRule> rules = ruleFactory.createRules(properties);

        // Verify - should return default rules since no valid rules were created
        assertNotNull(rules);
        assertEquals(3, rules.size());
    }
}
