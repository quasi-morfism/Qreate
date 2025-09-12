package com.morfism.aiappgenerator.service;

import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.model.dto.app.AppQueryRequest;
import com.morfism.aiappgenerator.model.entity.App;
import com.morfism.aiappgenerator.model.entity.User;
import com.morfism.aiappgenerator.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Application 服务层接口
 * Application service layer interface
 * 
 * 提供应用管理的核心业务逻辑：
 * 1. 应用数据的转换和封装 (VO转换)
 * 2. 查询条件构建和数据过滤
 * 3. AI代码生成集成服务
 * 4. 用户权限验证和业务规则校验
 *
 * @author Morfism
 */
public interface AppService extends IService<App> {

    /**
     * 将App实体转换为AppVO视图对象
     * Convert App entity to AppVO view object
     *
     * @param app 应用实体/App entity
     * @return 应用视图对象，包含用户信息/AppVO with user information
     */
    AppVO getAppVO(App app);

    /**
     * 批量将App实体列表转换为AppVO列表
     * Convert App entity list to AppVO list in batch
     *
     * @param appList 应用实体列表/App entity list
     * @return 应用视图对象列表，优化了N+1查询问题/AppVO list with optimized N+1 query
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据查询请求构建MyBatis查询条件
     * Build MyBatis query wrapper based on query request
     *
     * @param request 应用查询请求/App query request
     * @return MyBatis查询条件包装器/QueryWrapper for database query
     * @throws BusinessException 当请求参数为空时抛出异常/when request is null
     */
    QueryWrapper getQueryWrapper(AppQueryRequest request);

    /**
     * 聊天生成代码服务（流式响应）
     * Chat-to-code generation service with streaming response
     *
     * @param appId     应用ID/Application ID
     * @param message   用户消息/User message for code generation
     * @param loginUser 登录用户/Current login user
     * @return 代码生成的响应式流/Reactive stream of generated code
     * @throws BusinessException 当应用不存在、用户无权限或代码生成类型不支持时抛出异常
     *                          /when app not found, user has no permission, or unsupported code generation type
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);


    /**
     * Deploys an application based on the provided application ID and user information.
     *
     * @param appId     the ID of the application to be deployed
     * @param loginUser the current logged-in user initiating the deployment
     * @return a string indicating the status or result of the deployment process
     */
    String deployApp(Long appId, User loginUser);

}
