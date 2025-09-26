package com.morfism.aiappgenerator.ai.tools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

import jakarta.annotation.Resource;

/**
 * Tool Execution Handler
 * Unified processing of various tool execution results and status markers
 */
@Slf4j
@Component
public class ToolExecutionHandler {

    @Resource
    private ToolManager toolManager;

    /**
     * Handle tool execution results
     * Generate corresponding status markers and response information based on tool type
     *
     * @param toolExecution tool execution object
     * @param responseBuilder response builder
     * @param sink Flux emitter
     * @param appId application ID
     */
    public void handleToolExecution(Object toolExecution, StringBuilder responseBuilder, FluxSink<String> sink, Long appId) {
        try {
            // Use reflection to get tool execution information
            ToolExecutionRequest request = getToolExecutionRequest(toolExecution);
            String result = getToolExecutionResult(toolExecution);
            
            if (request == null) {
                log.warn("❌ Failed to extract tool execution request for appId: {}", appId);
                return;
            }

            String toolName = request.name();
            String fileName = extractFileNameFromArguments(request.arguments());
            
            log.info("🛠️ Tool executed: {} (id: {}) for appId: {}", toolName, request.id(), appId);
            
            // Get corresponding tool instance
            BaseTool tool = toolManager.getTool(toolName);
            if (tool == null) {
                handleUnknownTool(toolName, result, appId);
                return;
            }

            // Check if execution was successful
            boolean isSuccessful = tool.isToolExecutionSuccessful(result);
            String marker = generateToolMarker(tool.getOperationType(), fileName, isSuccessful);
            
            // Add marker to response builder and send to frontend
            responseBuilder.append(marker);
            log.info("📝 Added {} marker to responseBuilder: {}", 
                isSuccessful ? "SUCCESS" : "FAILED", marker);
            sink.next(marker);
            
        } catch (Exception e) {
            log.warn("❌ Error processing tool execution: {}", e.getMessage());
            String errorMarker = "\n[TOOL_EXECUTION_ERROR]";
            responseBuilder.append(errorMarker);
            sink.next(errorMarker);
        }
    }

    /**
     * Generate tool execution marker
     */
    private String generateToolMarker(String operationType, String fileName, boolean isSuccessful) {
        String status = isSuccessful ? "SUCCESS" : "FAILED";
        return String.format("\n[%s_%s:%s]", operationType.toUpperCase(), status, fileName);
    }


    /**
     * Handle unknown tool
     */
    private void handleUnknownTool(String toolName, String result, Long appId) {
        log.info("🔧 Unknown tool executed: {} for appId: {}, result: {}", 
            toolName, appId, 
            result != null ? result.substring(0, Math.min(100, result.length())) + "..." : "null");
    }

    /**
     * Extract file name from tool arguments
     */
    private String extractFileNameFromArguments(String arguments) {
        try {
            if (arguments != null) {
                // Find various possible file path fields
                String[] pathFields = {"relativeFilePath", "fileName", "path", "relativeDirPath"};
                
                for (String field : pathFields) {
                    String fieldPattern = "\"" + field + "\"";
                    if (arguments.contains(fieldPattern)) {
                        int start = arguments.indexOf(fieldPattern) + fieldPattern.length();
                        int valueStart = arguments.indexOf("\"", start) + 1;
                        int valueEnd = arguments.indexOf("\"", valueStart);
                        if (valueStart > 0 && valueEnd > valueStart) {
                            String fullPath = arguments.substring(valueStart, valueEnd);
                            
                            // Handle special cases
                            if ("relativeDirPath".equals(field) && fullPath.isEmpty()) {
                                return "project root";
                            }
                            
                            // Return only the filename part
                            int lastSlash = fullPath.lastIndexOf('/');
                            return lastSlash != -1 ? fullPath.substring(lastSlash + 1) : fullPath;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract file name from arguments: {}", e.getMessage());
        }
        return "unknown file";
    }

    /**
     * Get tool execution request through reflection
     */
    private ToolExecutionRequest getToolExecutionRequest(Object toolExecution) {
        try {
            return (ToolExecutionRequest) toolExecution.getClass()
                .getMethod("request")
                .invoke(toolExecution);
        } catch (Exception e) {
            log.debug("Failed to get tool execution request: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get tool execution result through reflection
     */
    private String getToolExecutionResult(Object toolExecution) {
        try {
            return (String) toolExecution.getClass()
                .getMethod("result")
                .invoke(toolExecution);
        } catch (Exception e) {
            log.debug("Failed to get tool execution result: {}", e.getMessage());
            return null;
        }
    }
}
