package com.morfism.aiappgenerator.ai.tools;

import com.morfism.aiappgenerator.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * File writing tool
 * Supports AI writing files through tool calls
 */
@Slf4j
public class FileWriteTool {

    @Tool("Write file to specified path")
    public String writeFile(@P("Relative file path") String relativeFilePath, @P("Content to write to file") String content, @ToolMemoryId Long appId) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                // Handle relative path, create project directory based on appId
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            // Create parent directory (if it doesn't exist)
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // Write file content
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Successfully wrote file: {}", path.toAbsolutePath());
            // Note: return relative path, don't let AI return absolute file path to user
            return "File written successfully: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "File write failed: " + relativeFilePath + ", error: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
