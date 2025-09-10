package com.morfism.aiappgenerator.model.dto.app;

import lombok.Data;


import java.io.Serializable;

/**
 * 创建应用请求（用户）
 */
@Data
public class AppAddRequest implements Serializable {

  
    private static final long serialVersionUID = 1L;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 初始化提示词（必填）
     */
    private String initPrompt;

    /**
     * 代码生成类型（可选）
     */
    private String codeGenType;
}


