package com.morfism.aiappgenerator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员更新任意应用
 */
@Data
public class AppUpdateAdminRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 优先级
     */
    private Integer priority;
}


