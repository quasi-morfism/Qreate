package com.morfism.aiappgenerator.ai.model;


import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Description("Result of generating multiple code files")
@Data
public class MultiFileCodeResult {

    @Description("HTML content and structure")
    private String htmlCode;

    @Description("CSS styling code")
    private String cssCode;

    @Description("JavaScript functionality code")
    private String jsCode;

    @Description("Description of the generated code files")
    private String description;
}
