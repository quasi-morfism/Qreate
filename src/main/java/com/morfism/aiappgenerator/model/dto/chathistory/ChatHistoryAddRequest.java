package com.morfism.aiappgenerator.model.dto.chathistory;

import lombok.Data;

import java.io.Serializable;

/**
 * Chat History add request
 *
 * @author Morfism
 */
@Data
public class ChatHistoryAddRequest implements Serializable {

    /**
     * message content
     */
    private String message;

    /**
     * message type: user/ai/error
     */
    private String messageType;

    /**
     * application id
     */
    private Long appId;

    private static final long serialVersionUID = 1L;
}