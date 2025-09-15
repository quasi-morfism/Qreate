package com.morfism.aiappgenerator.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat history entity class
 *
 * @author Morfism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_history")
public class ChatHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * message content
     */
    private String message;

    /**
     * message type: user/ai/error
     */
    @Column("messageType")
    private String messageType;

    /**
     * application id
     */
    @Column("appId")
    private Long appId;

    /**
     * user id (who sent the message)
     */
    @Column("userId")
    private Long userId;

    /**
     * create time
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * update time
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * is deleted
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
