package com.morfism.aiappgenerator.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.morfism.aiappgenerator.ai.AiCodeGeneratorService;
import com.morfism.aiappgenerator.annotation.AuthCheck;
import com.morfism.aiappgenerator.common.BaseResponse;
import com.morfism.aiappgenerator.common.DeleteRequest;
import com.morfism.aiappgenerator.common.ResultUtils;
import com.morfism.aiappgenerator.constant.AppConstant;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.model.dto.app.*;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;
import com.morfism.aiappgenerator.constant.UserConstant;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.exception.ThrowUtils;
import com.morfism.aiappgenerator.model.entity.App;
import com.morfism.aiappgenerator.model.entity.User;
import com.morfism.aiappgenerator.model.vo.AppVO;
import com.morfism.aiappgenerator.service.AppService;
import com.morfism.aiappgenerator.service.ProjectDownloadService;
import com.morfism.aiappgenerator.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;
import java.util.Map;


/**
 * App 控制层。
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    @Autowired
    private UserService userService;

    @Autowired
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Autowired
    private ProjectDownloadService projectDownloadService;

    /**
     * 用户创建应用（需填写 initPrompt）
     * 
     * 支持的代码生成类型 (codeGenType):
     * - "HTML": 单页面HTML应用，适合简单的静态页面
     * - "MULTI_FILE": 多文件项目，支持复杂的项目结构
     * - "VUE_PROJECT": Vue.js项目，支持实时文件写入和组件化开发
     * 
     * 如果不指定 codeGenType，默认使用 "MULTI_FILE"
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest addRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(addRequest.getInitPrompt()), ErrorCode.PARAMS_ERROR, "initPrompt is required");
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        BeanUtil.copyProperties(addRequest, app);

        // 验证并设置代码生成类型
        String codeGenType = app.getCodeGenType();
        if (StrUtil.isBlank(codeGenType)) {
            // 如果没有提供，使用默认值
            app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        } else {
            // 如果提供了，验证是否为有效值
            CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
            if (codeGenTypeEnum == null) {
                // 无效值，使用默认值
                app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
            } else {
                // 如果是有效值，保持原值不变
                app.setCodeGenType(codeGenType);
            }
        }

        // 如果未提供 appName，使用 AI 自动生成
        if (StrUtil.isBlank(addRequest.getAppName())) {
            String generatedAppName = aiCodeGeneratorService.generateAppName(addRequest.getInitPrompt());
            app.setAppName(generatedAppName);
        }

        app.setUserId(loginUser.getId());
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    // 用户根据 id 修改自己的应用（仅名称）
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMyApp(@RequestBody AppUpdateMyRequest updateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        App exist = appService.getById(updateRequest.getId());
        ThrowUtils.throwIf(exist == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!exist.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        App update = new App();
        update.setId(updateRequest.getId());
        update.setAppName(updateRequest.getAppName());
        update.setEditTime(java.time.LocalDateTime.now());
        boolean result = appService.updateById(update);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // 用户根据 id 删除自己的应用
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        App exist = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(exist == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!exist.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        boolean result = appService.deleteAppWithChatHistory(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    // 用户根据 id 查看应用详情
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param request         请求
     * @return 应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "query at most 20 apps per page");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }


    // 管理员删除任意应用
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        boolean result = appService.deleteAppWithChatHistory(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    // 管理员更新任意应用（名称、封面、优先级）
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateAdminRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        App update = new App();
        BeanUtil.copyProperties(updateRequest, update);
        update.setEditTime(java.time.LocalDateTime.now());
        boolean result = appService.updateById(update);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/selected/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "query at most 20 apps per page");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.SELECTED_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员分页获取应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }


    /**
     * 应用聊天生成代码（流式 SSE）
     * 
     * 支持多种代码生成类型：
     * - HTML: 单页面HTML代码生成
     * - MULTI_FILE: 多文件项目代码生成  
     * - VUE_PROJECT: Vue项目代码生成（带实时文件写入）
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     * 
     * SSE数据格式:
     * - 普通响应: {"q": "AI生成的内容"}
     * - 工具执行: {"q": "\n[TOOL_EXECUTED:writeFile:tool-id]"}
     * - 文件写入成功: {"q": "\n[FILE_WRITE_SUCCESS:filename.vue]"}
     * - 文件写入失败: {"q": "\n[FILE_WRITE_FAILED:filename.vue]"}
     * - 生成完成: {"q": "\n[GENERATION_COMPLETE]"}
     * - 流结束事件: event="done", data=""
     * 
     * 前端处理示例:
     * ```javascript
     * const eventSource = new EventSource('/app/chat/gen/code?appId=1&message=hello');
     * eventSource.onmessage = function(event) {
     *   const data = JSON.parse(event.data);
     *   const content = data.q;
     *   
     *   if (content.includes('[FILE_WRITE_SUCCESS:')) {
     *     // 处理文件写入成功
     *     const fileName = content.match(/\[FILE_WRITE_SUCCESS:(.*?)\]/)[1];
     *     console.log('文件写入成功:', fileName);
     *   } else if (content.includes('[GENERATION_COMPLETE]')) {
     *     // 处理生成完成
     *     console.log('代码生成完成');
     *   } else if (content.includes('[TOOL_EXECUTED:')) {
     *     // 处理工具执行
     *     console.log('工具执行:', content);
     *   } else {
     *     // 处理普通AI响应内容
     *     displayMessage(content);
     *   }
     * };
     * 
     * eventSource.addEventListener('done', function(event) {
     *   console.log('流传输完成');
     *   eventSource.close();
     * });
     * ```
     * 
     * 注意事项:
     * - 前端断开连接不会影响后台文件写入和聊天历史保存
     * - Vue项目生成会通过工具调用实时写入文件
     * - 聊天历史会在生成完成后自动保存
     * - 支持多个前端同时订阅同一个流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       @RequestParam(required = false) String adapt,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务生成代码（流式），传递adapt参数
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, adapt, loginUser);
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("q", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder().data(jsonData).build();

                }).concatWith(Mono.just(
                        ServerSentEvent.<String>builder().event("done").data("").build()
                ));

    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "App id cannot be empty");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }



    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String appName = StrUtil.blankToDefault(app.getAppName(), String.valueOf(appId));
        String downloadFileName = appName.replaceAll("[^a-zA-Z0-9]", "_")
                                         .replaceAll("_{2,}", "_")
                                         .replaceAll("^_|_$", "");
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }




}

    
