package com.morfism.aiappgenerator.controller;

import com.morfism.aiappgenerator.ai.tools.BaseTool;
import com.morfism.aiappgenerator.ai.tools.ToolManager;
import com.morfism.aiappgenerator.common.BaseResponse;
import com.morfism.aiappgenerator.common.ResultUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool Management Controller
 * Provides tool information and configuration for frontend
 */
@Slf4j
@RestController
@RequestMapping("/tool")
public class ToolController {

    @Resource
    private ToolManager toolManager;

    /**
     * Get all registered tool information
     * Frontend can use this information to dynamically configure tool processing logic
     */
    @GetMapping("/list")
    public BaseResponse<List<ToolInfo>> getAllTools() {
        BaseTool[] tools = toolManager.getAllTools();
        List<ToolInfo> toolInfos = new ArrayList<>();
        
        for (BaseTool tool : tools) {
            ToolInfo info = new ToolInfo();
            info.setToolName(tool.getToolName());
            info.setDisplayName(tool.getDisplayName());
            info.setOperationType(tool.getOperationType());
            info.setSuccessIcon(tool.getSuccessIcon());
            info.setFailedIcon(tool.getFailedIcon());
            toolInfos.add(info);
        }
        
        return ResultUtils.success(toolInfos);
    }


    /**
     * Tool Information DTO
     */
    public static class ToolInfo {
        private String toolName;        // English name, e.g., "writeFile"
        private String displayName;     // Display name, e.g., "Write File"
        private String operationType;   // Operation type, e.g., "FILE_WRITE"
        private String successIcon;     // Success icon SVG
        private String failedIcon;      // Failed icon SVG

        // Getters and Setters
        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        
        public String getSuccessIcon() { return successIcon; }
        public void setSuccessIcon(String successIcon) { this.successIcon = successIcon; }
        
        public String getFailedIcon() { return failedIcon; }
        public void setFailedIcon(String failedIcon) { this.failedIcon = failedIcon; }
    }
}
