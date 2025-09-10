package com.morfism.aiappgenerator.model.vo;

import lombok.Data;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Application View Object
 */
@Data
public class AppVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Application ID
     */
    private Long id;
    
    /**
     * Application name
     */
    private String appName;
    
    /**
     * Application cover image
     */
    private String cover;
    
    /**
     * Priority level
     */
    private Integer priority;
    
    /**
     * Initialization prompt
     */
    private String initPrompt;
    
    /**
     * Code generation type
     */
    private String codeGenType;
    
    /**
     * Deployment key
     */
    private String deployKey;
    
    /**
     * Deployment time
     */
    private LocalDateTime deployedTime;
    
    /**
     * User ID who created this app
     */
    private Long userId;
    
    /**
     * Creation time
     */
    private LocalDateTime createTime;

    /**
     * User information who created this app
     */
    private UserVO user;
}


