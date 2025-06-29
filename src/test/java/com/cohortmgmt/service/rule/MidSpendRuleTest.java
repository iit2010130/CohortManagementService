package com.cohortmgmt.service.rule;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MidSpendRule class.
 */
public class MidSpendRuleTest {
    
    @Test
    public void testGetName() {
        MidSpendRule rule = new MidSpendRule();
        assertEquals("MidSpend", rule.getName());
    }
    
    @Test
    public void testGetCohortType_Normal() {
        MidSpendRule rule = new MidSpendRule();
        assertEquals(CohortType.NORMAL, rule.getCohortType());
    }
    
    @Test
    public void testGetCohortType_Premium() {
        MidSpendRule rule = new MidSpendRule(CohortType.PREMIUM);
        assertEquals(CohortType.PREMIUM, rule.getCohortType());
    }
    
    @Test
    public void testEvaluateWithNullCustomer() {
        MidSpendRule rule = new MidSpendRule();
        assertFalse(rule.evaluate(null));
    }
    
    @Test
    public void testEvaluateWithNullDailySpend() {
        MidSpendRule rule = new MidSpendRule();
        Customer customer = new Customer("123", null, UserType.PAID);
        assertFalse(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendBelowMinThreshold() {
        MidSpendRule rule = new MidSpendRule();
        Customer customer = new Customer("123", 2999.99, UserType.PAID);
        assertFalse(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendEqualToMinThreshold() {
        MidSpendRule rule = new MidSpendRule();
        Customer customer = new Customer("123", 3000.0, UserType.PAID);
        assertFalse(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendAboveMaxThreshold() {
        MidSpendRule rule = new MidSpendRule();
        Customer customer = new Customer("123", 5000.0, UserType.PAID);
        assertFalse(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendInRange_Normal() {
        MidSpendRule rule = new MidSpendRule();
        Customer customer = new Customer("123", 4000.0, UserType.FREE);
        assertTrue(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendInRange_Premium_Paid() {
        MidSpendRule rule = new MidSpendRule(CohortType.PREMIUM);
        Customer customer = new Customer("123", 4000.0, UserType.PAID);
        assertTrue(rule.evaluate(customer));
    }
    
    @Test
    public void testEvaluateWithDailySpendInRange_Premium_Free() {
        MidSpendRule rule = new MidSpendRule(CohortType.PREMIUM);
        Customer customer = new Customer("123", 4000.0, UserType.FREE);
        assertFalse(rule.evaluate(customer));
    }
    
    @Test
    public void testGetMinThreshold() {
        MidSpendRule rule = new MidSpendRule();
        assertEquals(3000.0, rule.getMinThreshold());
    }
    
    @Test
    public void testGetMaxThreshold() {
        MidSpendRule rule = new MidSpendRule();
        assertEquals(5000.0, rule.getMaxThreshold());
    }
}
