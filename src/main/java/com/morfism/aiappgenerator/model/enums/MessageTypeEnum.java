package com.morfism.aiappgenerator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * Message type enumeration for chat history
 *
 * @author Morfism
 */
@Getter
public enum MessageTypeEnum {

    USER("User", "user"),
    AI("AI", "ai"),
    ERROR("Error", "error");

    private final String text;
    private final String value;

    MessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * Get enumeration by value
     *
     * @param value enum value
     * @return enumeration
     */
    public static MessageTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (MessageTypeEnum anEnum : MessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}