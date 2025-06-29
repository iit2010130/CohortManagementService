package com.cohortmgmt.controller;

import com.cohortmgmt.model.CohortType;
import com.cohortmgmt.service.CohortService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the CohortController class.
 */
public class CohortControllerTest {
    
    @Mock
    private CohortService cohortService;
    
    @InjectMocks
    private CohortController cohortController;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    public void testIsCustomerInCohortType_True() {
        // Arrange
        String customerId = "123";
        CohortType cohortType = CohortType.PREMIUM;
        when(cohortService.isCustomerInCohortType(customerId, cohortType)).thenReturn(true);
        
        // Act
        ResponseEntity<Boolean> response = cohortController.isCustomerInCohortType(customerId, cohortType);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody());
    }
    
    @Test
    public void testIsCustomerInCohortType_False() {
        // Arrange
        String customerId = "123";
        CohortType cohortType = CohortType.PREMIUM;
        when(cohortService.isCustomerInCohortType(customerId, cohortType)).thenReturn(false);
        
        // Act
        ResponseEntity<Boolean> response = cohortController.isCustomerInCohortType(customerId, cohortType);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody());
    }
    
    @Test
    public void testGetCustomerCohortTypes() {
        // Arrange
        String customerId = "123";
        List<CohortType> cohortTypes = Arrays.asList(
                CohortType.PREMIUM,
                CohortType.NORMAL
        );
        when(cohortService.getCustomerCohortTypes(customerId)).thenReturn(cohortTypes);
        
        // Act
        ResponseEntity<List<CohortType>> response = cohortController.getCustomerCohortTypes(customerId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(CohortType.PREMIUM, response.getBody().get(0));
        assertEquals(CohortType.NORMAL, response.getBody().get(1));
    }
    
    @Test
    public void testGetCustomerCohortTypes_Empty() {
        // Arrange
        String customerId = "123";
        List<CohortType> cohortTypes = Arrays.asList();
        when(cohortService.getCustomerCohortTypes(customerId)).thenReturn(cohortTypes);
        
        // Act
        ResponseEntity<List<CohortType>> response = cohortController.getCustomerCohortTypes(customerId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }
    
    @Test
    public void testGetCustomerIdsByCohortType() {
        // Arrange
        CohortType cohortType = CohortType.PREMIUM;
        Set<String> customerIds = new HashSet<>(Arrays.asList("123", "456", "789"));
        when(cohortService.getCustomerIdsByCohortType(cohortType)).thenReturn(customerIds);
        
        // Act
        ResponseEntity<Set<String>> response = cohortController.getCustomerIdsByCohortType(cohortType);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertEquals(customerIds, response.getBody());
    }
    
    @Test
    public void testGetCustomerIdsByCohortType_Empty() {
        // Arrange
        CohortType cohortType = CohortType.PREMIUM;
        Set<String> customerIds = new HashSet<>();
        when(cohortService.getCustomerIdsByCohortType(cohortType)).thenReturn(customerIds);
        
        // Act
        ResponseEntity<Set<String>> response = cohortController.getCustomerIdsByCohortType(cohortType);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
    }
}
