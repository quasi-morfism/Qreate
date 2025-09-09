package com.morfism.aiappgenerator.ai;

import com.morfism.aiappgenerator.ai.model.HtmlCodeResult;
import com.morfism.aiappgenerator.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
@ActiveProfiles("local")
@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Autowired
    AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult res = aiCodeGeneratorService.generateHtmlCode("make a blog about algebriac geometry less than 20 lines");
        Assertions.assertNotNull(res);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult res = aiCodeGeneratorService.generateMultiFileCode("create a simple calculator with HTML, CSS, and JavaScript");
        Assertions.assertNotNull(res);
        Assertions.assertNotNull(res.getHtmlCode());
        Assertions.assertNotNull(res.getCssCode());
        Assertions.assertNotNull(res.getJsCode());
    }
}