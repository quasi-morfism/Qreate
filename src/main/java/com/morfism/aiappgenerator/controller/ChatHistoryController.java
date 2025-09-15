package com.morfism.aiappgenerator.controller;

import com.morfism.aiappgenerator.model.dto.chathistory.ChatHistoryQueryRequest;
import com.morfism.aiappgenerator.model.entity.ChatHistory;
import com.mybatisflex.core.paginate.Page;
import com.morfism.aiappgenerator.annotation.AuthCheck;
import com.morfism.aiappgenerator.common.BaseResponse;
import com.morfism.aiappgenerator.common.ResultUtils;
import com.morfism.aiappgenerator.constant.UserConstant;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.exception.ThrowUtils;
import com.morfism.aiappgenerator.model.dto.chathistory.ChatHistoryAddRequest;
import com.morfism.aiappgenerator.model.entity.User;
import com.morfism.aiappgenerator.service.AppService;
import com.morfism.aiappgenerator.service.ChatHistoryService;
import com.morfism.aiappgenerator.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Chat History controller
 *
 * @author Morfism
 */
@RestController
@RequestMapping("/chat-history")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppService appService;

    /**
     * Add chat history manually (mainly for testing, real chat history is saved during conversation)
     *
     * @param addRequest add request
     * @param request    HTTP request
     * @return chat history id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChatHistory(@RequestBody ChatHistoryAddRequest addRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        
        // Save user message (for testing purposes)
        var chatHistory = chatHistoryService.saveUserMessage(
                addRequest.getAppId(), 
                addRequest.getMessage(), 
                loginUser
        );
        
        return ResultUtils.success(chatHistory.getId());
    }







    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }


    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }


    /**
     * Internal API: Delete all chat history by application ID (called when app is deleted)
     * This endpoint is for internal use and requires admin permission
     *
     * @param appId application ID
     * @return number of deleted records
     */
    @DeleteMapping("/internal/app/{appId}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> deleteChatHistoryByAppId(@PathVariable Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "Invalid application ID");
        
        int deletedCount = chatHistoryService.deleteChatHistoryByAppId(appId);
        return ResultUtils.success(deletedCount);
    }

}
