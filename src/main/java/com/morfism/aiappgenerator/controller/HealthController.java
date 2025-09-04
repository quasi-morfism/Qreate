package com.morfism.aiappgenerator.controller;

import com.morfism.aiappgenerator.common.BaseResponse;
import com.morfism.aiappgenerator.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class HealthController {
    @GetMapping("/hello")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("hello world");
    }
}
