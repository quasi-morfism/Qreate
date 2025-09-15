package com.morfism.aiappgenerator.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.morfism.aiappgenerator.constant.UserConstant;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.service.AppService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.exception.ThrowUtils;
import com.morfism.aiappgenerator.model.dto.chathistory.ChatHistoryQueryRequest;
import com.morfism.aiappgenerator.model.entity.App;
import com.morfism.aiappgenerator.model.entity.ChatHistory;
import com.morfism.aiappgenerator.mapper.ChatHistoryMapper;
import com.morfism.aiappgenerator.model.entity.User;
import com.morfism.aiappgenerator.model.enums.MessageTypeEnum;
import com.morfism.aiappgenerator.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat History service layer implementation
 *
 * @author Morfism
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {


    @Autowired
    @Lazy
    private AppService appService;





    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }




    @Override
    public ChatHistory saveUserMessage(Long appId, String message, User loginUser) {
        return saveMessage(appId, message, MessageTypeEnum.USER.getValue(), loginUser.getId());
    }

    @Override
    public ChatHistory saveAiMessage(Long appId, String message, Long userId) {
        return saveMessage(appId, message, MessageTypeEnum.AI.getValue(), userId);
    }

    @Override
    public ChatHistory saveErrorMessage(Long appId, String errorMessage, Long userId) {
        return saveMessage(appId, errorMessage, MessageTypeEnum.ERROR.getValue(), userId);
    }

    /**
     * Common method to save message to chat history
     */
    private ChatHistory saveMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Application ID cannot be null or empty");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "Message content cannot be empty");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "Message type cannot be empty");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "User ID cannot be null or empty");

        // Validate message type is in enum
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "Invalid message type: " + messageType);

        ChatHistory chatHistory = ChatHistory.builder()
                .message(message)
                .messageType(messageType)
                .appId(appId)
                .userId(userId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();

        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "Failed to save chat history");
        
        log.info("Chat history saved successfully - appId: {}, messageType: {}, userId: {}", 
                appId, messageType, userId);
        return chatHistory;
    }

    @Override
    public int deleteChatHistoryByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Application ID cannot be null or empty");
        
        int deletedCount = this.getMapper().deleteByAppId(appId);
        log.info("Deleted {} chat history records for appId: {}", deletedCount, appId);
        return deletedCount;
    }

    /**
     * 分页查询某个应用的对话历史（游标查询）
     * 使用创建时间作为游标进行分页查询，提高查询性能
     * 
     * @param appId          应用ID，不能为空且必须大于0
     * @param pageSize       页面大小，范围必须在1-50之间
     * @param lastCreateTime 最后一条记录的创建时间，用作游标查询的起点，为空则从最新记录开始
     * @param loginUser      登录用户信息，用于权限验证
     * @return 对话历史分页结果，按创建时间降序排列
     * @throws BusinessException 当参数无效、用户未登录、应用不存在或用户无权限时抛出
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (MessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (MessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("Successfully loaded {} chat history records for appId: {}", loadedCount, appId);
            return loadedCount;
        } catch (Exception e) {
            log.error("Failed to load chat history for appId: {}, error: {}", appId, e.getMessage(), e);
            // Loading failure doesn't affect system operation, just no historical context
            return 0;
        }
    }



}
