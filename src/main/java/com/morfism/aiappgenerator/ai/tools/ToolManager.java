package com.morfism.aiappgenerator.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool Manager
 * Unified management of all tools, provides functionality to get tools by name
 */
@Slf4j
@Component
public class ToolManager {

    /**
     * Mapping from tool name to tool instance
     */
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    /**
     * Auto-inject all tools
     */
    @Resource
    private BaseTool[] tools;

    /**
     * Initialize tool mapping
     */
    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
            log.info("Registered tool: {} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("Tool manager initialization completed, registered {} tools", toolMap.size());
    }

    /**
     * Get tool instance by tool name
     *
     * @param toolName tool English name
     * @return tool instance
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * Get registered tool collection
     *
     * @return tool instance collection
     */
    public BaseTool[] getAllTools() {
        return tools;
    }

    /**
     * Check if tool exists
     *
     * @param toolName tool name
     * @return whether exists
     */
    public boolean hasTool(String toolName) {
        return toolMap.containsKey(toolName);
    }

    /**
     * Get tool count
     *
     * @return tool count
     */
    public int getToolCount() {
        return toolMap.size();
    }
}
