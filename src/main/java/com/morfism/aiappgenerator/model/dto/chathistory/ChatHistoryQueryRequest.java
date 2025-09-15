package com.morfism.aiappgenerator.model.dto.chathistory;

import com.morfism.aiappgenerator.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Chat History query request
 *
 * @author Morfism
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private String message;

    /**
     * message type: user/ai/error
     */
    private String messageType;

    /**
     * application id
     */
    private Long appId;

    /**
     * user id
     */
    private Long userId;

    private LocalDateTime lastCreateTime;

    private static final long serialVersionUID = 1L;
}