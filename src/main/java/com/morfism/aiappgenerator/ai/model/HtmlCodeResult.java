package com.morfism.aiappgenerator.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Description("Result of generating HTML code file")
@Data
public class HtmlCodeResult {

    @Description("Complete HTML code including CSS and JavaScript")
    private String htmlCode;

    @Description("Description of the generated code")
    private String description;
}
