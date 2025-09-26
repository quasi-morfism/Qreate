package com.morfism.aiappgenerator.ai.tools;

import cn.hutool.json.JSONObject;
import com.morfism.aiappgenerator.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File Delete Tool
 * Supports AI deleting files through tool calls
 */
@Slf4j
@Component
public class FileDeleteTool extends BaseTool {

    @Tool("File Delete Tool")
    public String deleteFile(
            @P("Relative file path")
            String relativeFilePath,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path)) {
                return "Warning: File does not exist, no need to delete - " + relativeFilePath;
            }
            if (!Files.isRegularFile(path)) {
                return "Error: Specified path is not a file, cannot delete - " + relativeFilePath;
            }
            // Security check: Avoid deleting important files
            String fileName = path.getFileName().toString();
            if (isImportantFile(fileName)) {
                return "Error: Deletion of important files not allowed - " + fileName;
            }
            Files.delete(path);
            log.info("Successfully deleted file: {}", path.toAbsolutePath());
            return "File deleted successfully: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "File deletion failed: " + relativeFilePath + ", error: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * Determine if it's an important file that should not be deleted
     */
    private boolean isImportantFile(String fileName) {
        String[] importantFiles = {
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "vite.config.js", "vite.config.ts", "vue.config.js",
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md"
        };
        for (String important : importantFiles) {
            if (important.equalsIgnoreCase(fileName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getDisplayName() {
        return "Delete File";
    }

    @Override
    public String getSuccessIcon() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z\"/></svg>";
    }

    @Override
    public String getFailedIcon() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z\"/></svg>";
    }

    @Override
    public String getOperationType() {
        return "FILE_DELETE";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[Tool Call] %s %s", getDisplayName(), relativeFilePath);
    }
}
