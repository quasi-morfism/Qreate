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
 * user 实体类。
 *
 * @author Morfism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * account
     */
    @Column("userAccount")
    private String userAccount;

    /**
     * password
     */
    @Column("userPassword")
    private String userPassword;

    /**
     * username
     */
    @Column("userName")
    private String userName;

    /**
     * user avatar
     */
    @Column("userAvatar")
    private String userAvatar;

    /**
     * user profile
     */
    @Column("userProfile")
    private String userProfile;

    /**
     * user role: user/admin
     */
    @Column("userRole")
    private String userRole;

    /**
     * edit time
     */
    @Column("editTime")
    private LocalDateTime editTime;

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
