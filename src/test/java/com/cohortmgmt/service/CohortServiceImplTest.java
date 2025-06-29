package com.cohortmgmt.service;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.model.Customer;
import com.cohortmgmt.model.UserType;
import com.cohortmgmt.repository.CohortRepository;
import com.cohortmgmt.service.rule.CohortRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CohortServiceImpl class.
 */
public class CohortServiceImplTest {
    
    @Mock
    private CohortRepository cohortRepository;
    
    @Mock
    private CohortRule mockRule;
    
    private CohortServiceImpl cohortService;
    
    private Customer premiumCustomer;
    private Customer normalCustomer;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Set up the mock rule
        when(mockRule.getName()).thenReturn("TestRule");
        when(mockRule.getCohortType()).thenReturn(CohortType.PREMIUM);
        
        // Create the service with the mock rule
        List<CohortRule> rules = Collections.singletonList(mockRule);
        cohortService = new CohortServiceImpl(rules, cohortRepository);
        
        // Create test customers
        premiumCustomer = new Customer("premium-customer", 6000.0, UserType.PAID);
        normalCustomer = new Customer("normal-customer", 3000.0, UserType.FREE);
        
        // Set up the mock repository
        when(cohortRepository.isCustomerInCohortType(premiumCustomer.getCustomerId(), CohortType.PREMIUM)).thenReturn(true);
        when(cohortRepository.isCustomerInCohortType(normalCustomer.getCustomerId(), CohortType.PREMIUM)).thenReturn(false);
        when(cohortRepository.findCohortTypesByCustomerId(premiumCustomer.getCustomerId())).thenReturn(Collections.singletonList(CohortType.PREMIUM));
        when(cohortRepository.findCohortTypesByCustomerId(normalCustomer.getCustomerId())).thenReturn(Collections.emptyList());
        when(cohortRepository.getCustomerIdsByCohortType(CohortType.PREMIUM)).thenReturn(Collections.singleton(premiumCustomer.getCustomerId()));
        when(cohortRepository.getCustomerIdsByCohortType(CohortType.FRAUD)).thenReturn(Collections.emptySet());
    }
    
    @Test
    public void testClassifyCustomer_Success() {
        // Arrange
        when(mockRule.evaluate(premiumCustomer)).thenReturn(true);
        
        // Act
        Set<CohortType> cohortTypes = cohortService.classifyCustomer(premiumCustomer);
        
        // Assert
        assertNotNull(cohortTypes);
        assertEquals(1, cohortTypes.size());
        assertTrue(cohortTypes.contains(CohortType.PREMIUM));
        verify(cohortRepository).addCustomerToCohortType(CohortType.PREMIUM, premiumCustomer.getCustomerId());
    }
    
    @Test
    public void testClassifyCustomer_NoMatch() {
        // Arrange
        when(mockRule.evaluate(normalCustomer)).thenReturn(false);
        
        // Act
        Set<CohortType> cohortTypes = cohortService.classifyCustomer(normalCustomer);
        
        // Assert
        assertNotNull(cohortTypes);
        assertTrue(cohortTypes.isEmpty());
        verify(cohortRepository, never()).addCustomerToCohortType(any(CohortType.class), anyString());
    }
    
    @Test
    public void testClassifyCustomer_NullCustomer() {
        // Act
        Set<CohortType> cohortTypes = cohortService.classifyCustomer(null);
        
        // Assert
        assertNotNull(cohortTypes);
        assertTrue(cohortTypes.isEmpty());
        verify(cohortRepository, never()).addCustomerToCohortType(any(CohortType.class), anyString());
    }
    
    @Test
    public void testClassifyCustomer_RuleException() {
        // Arrange
        when(mockRule.evaluate(premiumCustomer)).thenThrow(new RuntimeException("Test exception"));
        
        // Act
        Set<CohortType> cohortTypes = cohortService.classifyCustomer(premiumCustomer);
        
        // Assert
        assertNotNull(cohortTypes);
        assertTrue(cohortTypes.isEmpty());
        verify(cohortRepository, never()).addCustomerToCohortType(any(CohortType.class), anyString());
    }
    
    @Test
    public void testIsCustomerInCohortType_True() {
        // Act
        boolean result = cohortService.isCustomerInCohortType(premiumCustomer.getCustomerId(), CohortType.PREMIUM);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    public void testIsCustomerInCohortType_False() {
        // Act
        boolean result = cohortService.isCustomerInCohortType(normalCustomer.getCustomerId(), CohortType.PREMIUM);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    public void testIsCustomerInCohortType_NullCustomerId() {
        // Act
        boolean result = cohortService.isCustomerInCohortType(null, CohortType.PREMIUM);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    public void testIsCustomerInCohortType_NullCohortType() {
        // Act
        boolean result = cohortService.isCustomerInCohortType(premiumCustomer.getCustomerId(), null);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    public void testGetCustomerCohortTypes() {
        // Act
        List<CohortType> cohortTypes = cohortService.getCustomerCohortTypes(premiumCustomer.getCustomerId());
        
        // Assert
        assertNotNull(cohortTypes);
        assertEquals(1, cohortTypes.size());
        assertEquals(CohortType.PREMIUM, cohortTypes.get(0));
    }
    
    @Test
    public void testGetCustomerCohortTypes_NoCohorts() {
        // Act
        List<CohortType> cohortTypes = cohortService.getCustomerCohortTypes(normalCustomer.getCustomerId());
        
        // Assert
        assertNotNull(cohortTypes);
        assertTrue(cohortTypes.isEmpty());
    }
    
    @Test
    public void testGetCustomerCohortTypes_NullCustomerId() {
        // Act
        List<CohortType> cohortTypes = cohortService.getCustomerCohortTypes(null);
        
        // Assert
        assertNotNull(cohortTypes);
        assertTrue(cohortTypes.isEmpty());
    }
    
    @Test
    public void testGetCustomerIdsByCohortType() {
        // Act
        Set<String> customerIds = cohortService.getCustomerIdsByCohortType(CohortType.PREMIUM);
        
        // Assert
        assertNotNull(customerIds);
        assertEquals(1, customerIds.size());
        assertTrue(customerIds.contains(premiumCustomer.getCustomerId()));
    }
    
    @Test
    public void testGetCustomerIdsByCohortType_NoCohorts() {
        // Act
        Set<String> customerIds = cohortService.getCustomerIdsByCohortType(CohortType.FRAUD);
        
        // Assert
        assertNotNull(customerIds);
        assertTrue(customerIds.isEmpty());
    }
    
    @Test
    public void testGetCustomerIdsByCohortType_NullCohortType() {
        // Act
        Set<String> customerIds = cohortService.getCustomerIdsByCohortType(null);
        
        // Assert
        assertNotNull(customerIds);
        assertTrue(customerIds.isEmpty());
    }
    
    @Test
    public void testAddRule() {
        // Arrange
        CohortRule newRule = mock(CohortRule.class);
        when(newRule.getName()).thenReturn("NewRule");
        when(newRule.getCohortType()).thenReturn(CohortType.NORMAL);
        
        // Act
        cohortService.addRule(newRule);
        
        // Assert
        verify(cohortRepository, never()).addCustomerToCohortType(any(CohortType.class), anyString());
    }
}
