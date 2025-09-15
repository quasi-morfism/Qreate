package com.morfism.aiappgenerator.service;

import com.morfism.aiappgenerator.exception.BusinessException;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.morfism.aiappgenerator.model.dto.chathistory.ChatHistoryQueryRequest;
import com.morfism.aiappgenerator.model.entity.ChatHistory;
import com.morfism.aiappgenerator.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * Chat History service layer interface
 *
 * @author Morfism
 */
public interface ChatHistoryService extends IService<ChatHistory> {





    /**
     * Build MyBatis query wrapper based on query request
     *
     * @param request chat history query request
     * @return QueryWrapper for database query
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest request);


    /**
     * Save user message to chat history
     *
     * @param appId application ID
     * @param message user message content
     * @param loginUser current login user
     * @return saved chat history
     */
    ChatHistory saveUserMessage(Long appId, String message, User loginUser);

    /**
     * Save AI message to chat history
     *
     * @param appId application ID
     * @param message AI message content
     * @param userId user ID who initiated the conversation
     * @return saved chat history
     */
    ChatHistory saveAiMessage(Long appId, String message, Long userId);

    /**
     * Save error message to chat history
     *
     * @param appId application ID
     * @param errorMessage error message content
     * @param userId user ID who initiated the conversation
     * @return saved chat history
     */
    ChatHistory saveErrorMessage(Long appId, String errorMessage, Long userId);

    /**
     * Delete all chat history by application ID (called when app is deleted)
     *
     * @param appId application ID
     * @return number of deleted records
     */
    int deleteChatHistoryByAppId(Long appId);


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
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);


    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
