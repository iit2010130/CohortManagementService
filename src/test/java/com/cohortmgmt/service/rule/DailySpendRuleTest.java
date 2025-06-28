package com.cohortmgmt.service.rule;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DailySpendRule class.
 */
public class DailySpendRuleTest {
    
    @Test
    public void testGetName() {
        DailySpendRule rule = new DailySpendRule();
        assertEquals("DailySpend", rule.getName());
    }
    
    @Test
    public void testGetCohortType() {
        DailySpendRule rule = new DailySpendRule();
        assertEquals(CohortType.PREMIUM, rule.getCohortType());
    }
    
    @Test
    public void testEvaluateWithNullCustomer() {
        DailySpendRule rule = new DailySpendRule();
        assertFalse(rule.evaluate(null));
    }
    
    @Test
    public void testEvaluateWithNullDailySpend() {
        DailySpendRule rule = new DailySpendRule();
        Customer customer = new Customer("123", null, UserType.PAID);
        assertFalse(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendBelowThreshold() {
        DailySpendRule rule = new DailySpendRule();
        Customer customer = new Customer("123", 4999.99, UserType.PAID);
        assertFalse(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendEqualToThreshold() {
        DailySpendRule rule = new DailySpendRule();
        Customer customer = new Customer("123", 5000.0, UserType.PAID);
        assertFalse(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendAboveThreshold() {
        DailySpendRule rule = new DailySpendRule();
        Customer customer = new Customer("123", 5000.01, UserType.PAID);
        assertTrue(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithCustomThreshold() {
        DailySpendRule rule = new DailySpendRule(1000.0);
        Customer customer = new Customer("123", 1500.0, UserType.PAID);
        assertTrue(rule.evaluate(customer));
    }
    
    @Test
    public void testGetThreshold() {
        DailySpendRule rule = new DailySpendRule();
        assertEquals(5000.0, rule.getThreshold());
        
        DailySpendRule customRule = new DailySpendRule(1000.0);
        assertEquals(1000.0, customRule.getThreshold());
    }
}
