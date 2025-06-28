package com.cohortmgmt.service;

import com.cohortmgmt.model.Cohort;
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
    private Cohort premiumCohort;
    
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
        
        // Create test cohort
        premiumCohort = new Cohort("TestRule_PREMIUM", CohortType.PREMIUM, "Premium customers");
        Set<String> customerIds = new HashSet<>();
        customerIds.add(premiumCustomer.getCustomerId());
        premiumCohort.setCustomerIds(customerIds);
        
        // Set up the mock repository
        when(cohortRepository.existsById(anyString())).thenReturn(false);
        when(cohortRepository.save(any(Cohort.class))).thenReturn(premiumCohort);
        when(cohortRepository.findById("TestRule_PREMIUM")).thenReturn(Optional.of(premiumCohort));
        when(cohortRepository.findByType(CohortType.PREMIUM)).thenReturn(Collections.singletonList(premiumCohort));
        when(cohortRepository.findByCustomerId(premiumCustomer.getCustomerId())).thenReturn(Collections.singletonList(premiumCohort));
        when(cohortRepository.findByCustomerId(normalCustomer.getCustomerId())).thenReturn(Collections.emptyList());
        when(cohortRepository.getCustomerIds("TestRule_PREMIUM")).thenReturn(premiumCohort.getCustomerIds());
    }
    
    @Test
    public void testClassifyCustomer_Success() {
        // Arrange
        when(mockRule.evaluate(premiumCustomer)).thenReturn(true);
        
        // Act
        Set<String> cohortIds = cohortService.classifyCustomer(premiumCustomer);
        
        // Assert
        assertNotNull(cohortIds);
        assertEquals(1, cohortIds.size());
        assertTrue(cohortIds.contains("TestRule_PREMIUM"));
        verify(cohortRepository).addCustomerToCohort("TestRule_PREMIUM", premiumCustomer.getCustomerId());
    }
    
    @Test
    public void testClassifyCustomer_NoMatch() {
        // Arrange
        when(mockRule.evaluate(normalCustomer)).thenReturn(false);
        
        // Act
        Set<String> cohortIds = cohortService.classifyCustomer(normalCustomer);
        
        // Assert
        assertNotNull(cohortIds);
        assertTrue(cohortIds.isEmpty());
        verify(cohortRepository, never()).addCustomerToCohort(anyString(), anyString());
    }
    
    @Test
    public void testClassifyCustomer_NullCustomer() {
        // Act
        Set<String> cohortIds = cohortService.classifyCustomer(null);
        
        // Assert
        assertNotNull(cohortIds);
        assertTrue(cohortIds.isEmpty());
        verify(cohortRepository, never()).addCustomerToCohort(anyString(), anyString());
    }
    
    @Test
    public void testClassifyCustomer_RuleException() {
        // Arrange
        when(mockRule.evaluate(premiumCustomer)).thenThrow(new RuntimeException("Test exception"));
        
        // Act
        Set<String> cohortIds = cohortService.classifyCustomer(premiumCustomer);
        
        // Assert
        assertNotNull(cohortIds);
        assertTrue(cohortIds.isEmpty());
        verify(cohortRepository, never()).addCustomerToCohort(anyString(), anyString());
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
    public void testGetCustomerCohorts() {
        // Act
        List<Cohort> cohorts = cohortService.getCustomerCohorts(premiumCustomer.getCustomerId());
        
        // Assert
        assertNotNull(cohorts);
        assertEquals(1, cohorts.size());
        assertEquals(premiumCohort.getId(), cohorts.get(0).getId());
    }
    
    @Test
    public void testGetCustomerCohorts_NoCohorts() {
        // Act
        List<Cohort> cohorts = cohortService.getCustomerCohorts(normalCustomer.getCustomerId());
        
        // Assert
        assertNotNull(cohorts);
        assertTrue(cohorts.isEmpty());
    }
    
    @Test
    public void testGetCustomerCohorts_NullCustomerId() {
        // Act
        List<Cohort> cohorts = cohortService.getCustomerCohorts(null);
        
        // Assert
        assertNotNull(cohorts);
        assertTrue(cohorts.isEmpty());
    }
    
    @Test
    public void testGetCohortCustomerIdsByType() {
        // Act
        Set<String> customerIds = cohortService.getCohortCustomerIdsByType(CohortType.PREMIUM);
        
        // Assert
        assertNotNull(customerIds);
        assertEquals(1, customerIds.size());
        assertTrue(customerIds.contains(premiumCustomer.getCustomerId()));
    }
    
    @Test
    public void testGetCohortCustomerIdsByType_NoCohorts() {
        // Arrange
        when(cohortRepository.findByType(CohortType.FRAUD)).thenReturn(Collections.emptyList());
        
        // Act
        Set<String> customerIds = cohortService.getCohortCustomerIdsByType(CohortType.FRAUD);
        
        // Assert
        assertNotNull(customerIds);
        assertTrue(customerIds.isEmpty());
    }
    
    @Test
    public void testGetCohortCustomerIdsByType_NullCohortType() {
        // Act
        Set<String> customerIds = cohortService.getCohortCustomerIdsByType(null);
        
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
        verify(cohortRepository).existsById("NewRule_NORMAL");
        verify(cohortRepository).save(any(Cohort.class));
    }
}
