package com.morfism.aiappgenerator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新自己的应用（仅支持名称）
 */
@Data
public class AppUpdateMyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 应用名称（可修改字段）
     */
    private String appName;
}


