package com.morfism.aiappgenerator.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.morfism.aiappgenerator.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * File Directory Read Tool
 * Uses Hutool to simplify file operations
 */
@Slf4j
@Component
public class FileDirReadTool extends BaseTool {

    /**
     * Files and directories to ignore
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * File extensions to ignore
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    @Tool("Directory Reading Tool: to understand the current project structure")
    public String readDir(
            @P("Relative directory path, empty to read entire project structure")
            String relativeDirPath,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeDirPath == null ? "" : relativeDirPath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeDirPath == null ? "" : relativeDirPath);
            }
            File targetDir = path.toFile();
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                return "Error: Directory does not exist or is not a directory - " + relativeDirPath;
            }
            StringBuilder structure = new StringBuilder();
            structure.append("Project directory structure:\n");
            
            // 递归构建目录树结构，限制深度避免性能问题
            buildDirectoryTree(targetDir, structure, "", 0, 3); // 最大深度3层
            return structure.toString();

        } catch (Exception e) {
            String errorMessage = "Failed to read directory structure: " + relativeDirPath + ", error: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 递归构建目录树结构
     * Build directory tree structure recursively with depth limit
     */
    private void buildDirectoryTree(File dir, StringBuilder structure, String indent, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth) {
            return;
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        
        // 分离目录和文件，并排序
        List<File> directories = new ArrayList<>();
        List<File> regularFiles = new ArrayList<>();
        
        for (File file : files) {
            if (shouldIgnore(file.getName())) {
                continue;
            }
            if (file.isDirectory()) {
                directories.add(file);
            } else {
                regularFiles.add(file);
            }
        }
        
        // 排序
        directories.sort(Comparator.comparing(File::getName));
        regularFiles.sort(Comparator.comparing(File::getName));
        
        // 先显示目录
        for (File directory : directories) {
            structure.append(indent).append("📁 ").append(directory.getName()).append("/\n");
            buildDirectoryTree(directory, structure, indent + "  ", currentDepth + 1, maxDepth);
        }
        
        // 再显示文件
        for (File file : regularFiles) {
            structure.append(indent).append("📄 ").append(file.getName()).append("\n");
        }
    }

    /**
     * Determine if this file or directory should be ignored
     */
    private boolean shouldIgnore(String fileName) {
        // Check if in ignore name list
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }

        // Check file extension
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getDisplayName() {
        return "Read Directory";
    }

    @Override
    public String getSuccessIcon() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M10 4H4c-1.11 0-2 .89-2 2v12c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2h-8l-2-2z\"/></svg>";
    }

    @Override
    public String getFailedIcon() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M10 4H4c-1.11 0-2 .89-2 2v12c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2h-8l-2-2z\"/></svg>";
    }

    @Override
    public String getOperationType() {
        return "DIR_READ";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeDirPath = arguments.getStr("relativeDirPath");
        if (StrUtil.isEmpty(relativeDirPath)) {
            relativeDirPath = "Root Directory";
        }
        return String.format("[Tool Call] %s %s", getDisplayName(), relativeDirPath);
    }
}
