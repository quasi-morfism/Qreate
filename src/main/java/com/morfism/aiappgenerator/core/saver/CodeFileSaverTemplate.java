package com.morfism.aiappgenerator.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;


/**
 * Template class for saving code files to disk
 * Uses template method pattern to define the common save workflow
 *
 * @param <T> the type of code result to save
 */
public abstract class CodeFileSaverTemplate<T> {
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * Main template method for saving code files
     * Defines the workflow: validate -> create directory -> save files -> return directory
     *
     * @param result the code result to save
     * @return the directory where files were saved
     * @throws BusinessException if result is null or save operation fails
     */
    public final File saveCode(T result){
        // Validate input parameters
        validateInput(result);
        // Build unique directory for this save operation
        String baseDirPath = buildUniqueDir();
        // Save files (implemented by child classes)
        saveFiles(result, baseDirPath);
        // Return the directory object
        return new File(baseDirPath);
    }

    /**
     * Build a unique directory path for saving files
     * Directory format: {root}/{codeType}_{snowflakeId}
     *
     * @return the created directory path
     */
    protected String buildUniqueDir() {
        String codeType = getCodeType().getValue();
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * Validate the input result object
     * Can be overridden by subclasses for specific validation logic
     *
     * @param result the code result to validate
     * @throws BusinessException if validation fails
     */
    protected void validateInput(T result){
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"save code result cannot be null");
        }
    }

    /**
     * Get the code generation type for this saver
     * Used for directory naming and identification
     *
     * @return the code generation type enum
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * Save the actual files to the specified directory
     * Must be implemented by concrete subclasses
     *
     * @param result the code result containing file content
     * @param baseDirPath the base directory path to save files
     */
    protected abstract void saveFiles(T result, String baseDirPath);

    public final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(dirPath) && StrUtil.isNotBlank(filename) && StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }

    }

}
