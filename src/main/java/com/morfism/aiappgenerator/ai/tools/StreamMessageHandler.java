package com.morfism.aiappgenerator.ai.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Set;

/**
 * Stream Message Handler
 * Handles TOOL_REQUEST and TOOL_EXECUTED messages in streaming transmission
 * Based on user-provided stream processing logic patterns
 */
@Slf4j
@Component
public class StreamMessageHandler {

    @Resource
    private ToolManager toolManager;

    /**
     * Handle tool request messages
     * Corresponds to user-provided TOOL_REQUEST case logic
     *
     * @param chunk message chunk
     * @param seenToolIds set of processed tool IDs
     * @return formatted tool request response
     */
    public String handleToolRequest(String chunk, Set<String> seenToolIds) {
        try {
            // Parse tool request message
            ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
            String toolId = toolRequestMessage.getId();
            String toolName = toolRequestMessage.getName();
            
            // Check if this is the first time seeing this tool ID
            if (toolId != null && !seenToolIds.contains(toolId)) {
                // First time calling this tool, record ID and return tool information
                seenToolIds.add(toolId);
                
                // Get tool instance by tool name
                BaseTool tool = toolManager.getTool(toolName);
                if (tool != null) {
                    // Return formatted tool call information
                    return tool.generateToolRequestResponse();
                } else {
                    log.warn("Unknown tool requested: {}", toolName);
                    return String.format("\n\n🛠️ Calling tool: %s...\n\n", toolName);
                }
            } else {
                // Not the first time calling this tool, return empty
                return "";
            }
        } catch (Exception e) {
            log.warn("Failed to handle tool request: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Handle tool execution completed messages
     * Corresponds to user-provided TOOL_EXECUTED case logic
     *
     * @param chunk message chunk
     * @param chatHistoryStringBuilder chat history builder
     * @return formatted tool execution result
     */
    public String handleToolExecuted(String chunk, StringBuilder chatHistoryStringBuilder) {
        try {
            // Parse tool execution message
            ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
            String toolName = toolExecutedMessage.getName();
            JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
            
            // Get tool instance by tool name and generate corresponding result format
            BaseTool tool = toolManager.getTool(toolName);
            if (tool != null) {
                String result = tool.generateToolExecutedResult(jsonObject);
                
                // Output content for frontend and persistence
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            } else {
                log.warn("Unknown tool executed: {}", toolName);
                String fallbackOutput = String.format("\n\n[Tool Call] Unknown tool: %s\n\n", toolName);
                chatHistoryStringBuilder.append(fallbackOutput);
                return fallbackOutput;
            }
        } catch (Exception e) {
            log.warn("Failed to handle tool executed: {}", e.getMessage());
            String errorOutput = "\n\n[Tool Call] Processing failed\n\n";
            chatHistoryStringBuilder.append(errorOutput);
            return errorOutput;
        }
    }

    /**
     * Tool request message model
     */
    public static class ToolRequestMessage {
        private String id;
        private String name;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * Tool execution message model
     */
    public static class ToolExecutedMessage {
        private String name;
        private String arguments;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }
}
