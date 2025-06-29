package com.cohortmgmt.config;

import com.cohortmgmt.model.CohortType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for cohort rules.
 */
@ConfigurationProperties(prefix = "cohort.rules")
public class CohortRuleProperties {
    
    private boolean enabled = true;
    private List<RuleConfig> configurations = new ArrayList<>();
    
    /**
     * Configuration for a single rule.
     */
    public static class RuleConfig {
        private String type; // "daily-spend" or "mid-spend"
        private CohortType cohortType; // Using ENUM directly as requested
        private Double minThreshold;
        private Double maxThreshold;
        private Boolean requirePaidUser;
        
        // Getters and setters
        public String getType() { 
            return type; 
        }
        
        public void setType(String type) { 
            this.type = type; 
        }
        
        public CohortType getCohortType() { 
            return cohortType; 
        }
        
        public void setCohortType(CohortType cohortType) { 
            this.cohortType = cohortType; 
        }
        
        public Double getMinThreshold() { 
            return minThreshold; 
        }
        
        public void setMinThreshold(Double minThreshold) { 
            this.minThreshold = minThreshold; 
        }
        
        public Double getMaxThreshold() { 
            return maxThreshold; 
        }
        
        public void setMaxThreshold(Double maxThreshold) { 
            this.maxThreshold = maxThreshold; 
        }
        
        public Boolean getRequirePaidUser() { 
            return requirePaidUser; 
        }
        
        public void setRequirePaidUser(Boolean requirePaidUser) { 
            this.requirePaidUser = requirePaidUser; 
        }
    }
    
    // Getters and setters
    public boolean isEnabled() { 
        return enabled; 
    }
    
    public void setEnabled(boolean enabled) { 
        this.enabled = enabled; 
    }
    
    public List<RuleConfig> getConfigurations() { 
        return configurations; 
    }
    
    public void setConfigurations(List<RuleConfig> configurations) { 
        this.configurations = configurations; 
    }
}
