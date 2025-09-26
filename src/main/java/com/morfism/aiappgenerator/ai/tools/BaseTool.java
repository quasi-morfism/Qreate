package com.morfism.aiappgenerator.ai.tools;

import cn.hutool.json.JSONObject;

/**
 * Base Tool Class
 * Defines common interfaces and behaviors for all AI tools
 */
public abstract class BaseTool {

    /**
     * Get tool English name (for AI service identification)
     * @return tool name
     */
    public abstract String getToolName();

    /**
     * Get tool display name (for frontend display)
     * @return display name
     */
    public abstract String getDisplayName();

    /**
     * Get tool success icon SVG
     * @return success icon SVG string
     */
    public abstract String getSuccessIcon();

    /**
     * Get tool failed icon SVG  
     * @return failed icon SVG string
     */
    public abstract String getFailedIcon();

    /**
     * Get tool operation type (for status markers)
     * @return operation type string
     */
    public abstract String getOperationType();

    /**
     * Generate tool request response information
     * Information displayed when AI requests to call the tool
     * @return formatted tool request information
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n🛠️ Calling %s...\n\n", getDisplayName());
    }

    /**
     * Generate tool execution result information
     * Generate formatted execution result based on tool parameters
     * @param arguments tool call arguments
     * @return formatted execution result information
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);

    /**
     * Check if tool execution was successful
     * Determine success based on execution result
     * @param result tool execution result
     * @return whether successful
     */
    protected boolean isToolExecutionSuccessful(String result) {
        if (result == null) {
            return false;
        }
        String lowerResult = result.toLowerCase();
        return !lowerResult.contains("error:") 
            && !lowerResult.contains("error") 
            && !lowerResult.contains("failed");
    }

}
