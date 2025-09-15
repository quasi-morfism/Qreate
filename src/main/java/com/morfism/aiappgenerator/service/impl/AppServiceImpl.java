package com.morfism.aiappgenerator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.morfism.aiappgenerator.constant.AppConstant;
import com.morfism.aiappgenerator.core.AiCodeGeneratorFacade;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.exception.ThrowUtils;
import com.morfism.aiappgenerator.model.dto.app.AppQueryRequest;
import com.morfism.aiappgenerator.model.entity.App;
import com.morfism.aiappgenerator.model.entity.User;
import com.morfism.aiappgenerator.mapper.AppMapper;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;
import com.morfism.aiappgenerator.model.vo.AppVO;
import com.morfism.aiappgenerator.model.vo.UserVO;
import com.morfism.aiappgenerator.service.AppService;
import com.morfism.aiappgenerator.service.ChatHistoryService;
import com.morfism.aiappgenerator.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application 服务层实现。
 *
 * @author Morfism
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService {

    @Autowired
    private UserService userService;

    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);

        // Populate user information
        if (app.getUserId() != null) {
            User user = userService.getById(app.getUserId());
            if (user != null) {
                UserVO userVO = userService.getUserVO(user);
                appVO.setUser(userVO);
            }
        }

        return appVO;
    }



    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. Parameter validation
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Application ID cannot be null or empty");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "User message cannot be blank");
        // 2. Query application information
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "Application not found");
        // 3. Verify user permission to access the application, only owner can generate code
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "No permission to access this application");
        }
        // 4. Get application code generation type
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsupported code generation type");
        }

        // 5. Save user message to chat history
        try {
            chatHistoryService.saveUserMessage(appId, message, loginUser);
        } catch (Exception e) {
            log.warn("Failed to save user message to chat history: {}", e.getMessage());
            // Continue with code generation even if chat history saving fails
        }

        // 6. Get shared stream from facade (all backend processing including code saving and chat history is handled independently)
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId, loginUser.getId());
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. Parameter validation
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Application ID cannot be null or empty");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "User not logged in");
        // 2. Query application information
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "Application not found");
        // 3. Verify user permission to deploy the application, only owner can deploy
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "No permission to deploy this application");
        }
        // 4. Check if deployKey already exists
        String deployKey = app.getDeployKey();
        // Generate 6-character deployKey (letters + numbers) if not exists
        if (StrUtil.isBlank(deployKey)) {
            deployKey = generateUniqueDeployKey();
        }
        // 5. Get code generation type and build source directory path
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. Check if source directory exists
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Application code does not exist, please generate code first");
        }
        // 7. Copy files to deployment directory
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Deployment failed: " + e.getMessage());
        }
        // 8. Update application deployKey and deployment time
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "Failed to update application deployment information");
        // 9. Return accessible URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

    @Override
    @Transactional
    public boolean deleteAppWithChatHistory(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Application ID cannot be null or empty");
        
        try {
            // First delete related chat history
            int deletedChatHistoryCount = chatHistoryService.deleteChatHistoryByAppId(appId);
            log.info("Deleted {} chat history records for appId: {}", deletedChatHistoryCount, appId);
            
            // Then delete the application
            boolean result = super.removeById(appId);
            if (result) {
                log.info("Successfully deleted application with id: {}", appId);
            } else {
                log.error("Failed to delete application with id: {}", appId);
            }
            return result;
        } catch (Exception e) {
            log.error("Error deleting application with chat history for appId: {}, error: {}", appId, e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to delete application: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        if(id == null){
            return false;
        }
        // First delete related chat history, then delete the app
        Long appId = Long.valueOf(id.toString());
        if(appId <=0){return false;}

        try {
            // Delete related chat history
            int deletedChatHistoryCount = chatHistoryService.deleteChatHistoryByAppId(appId);
            log.info("Deleted {} chat history records for appId: {}", deletedChatHistoryCount, appId);
            
            // Then delete the application using parent's removeById
            boolean result = super.removeById(appId);
            if (result) {
                log.info("Successfully deleted application with id: {}", appId);
            } else {
                log.error("Failed to delete application with id: {}", appId);
            }
            return result;
        } catch (Exception e) {
            log.error("Error deleting application with chat history for appId: {}, error: {}", id, e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to delete application: " + e.getMessage());
        }
    }


    /**
     * Generate unique deploy key with duplication check
     * Generates a 6-character random string and ensures uniqueness in database
     *
     * @return unique deploy key
     */
    private String generateUniqueDeployKey() {
        String deployKey;
        int maxAttempts = 10;
        int attempts = 0;
        
        do {
            deployKey = RandomUtil.randomString(6);
            attempts++;
            
            // Check if key already exists in database
            QueryWrapper queryWrapper = QueryWrapper.create().eq("deployKey", deployKey);
            long count = this.count(queryWrapper);
            
            if (count == 0) {
                return deployKey; // Found unique key
            }
            
        } while (attempts < maxAttempts);
        
        // If we can't find a unique key after maxAttempts, throw an exception
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to generate unique deploy key after " + maxAttempts + " attempts");
    }

}


