package com.morfism.aiappgenerator.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * chat history 实体类。
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
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * message
     */
    private String message;

    /**
     * user/ai
     */
    @Column("messageType")
    private String messageType;

    /**
     * application id
     */
    @Column("appId")
    private Long appId;

    /**
     * creator user id
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
