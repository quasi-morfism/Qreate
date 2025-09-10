# 数据库初始化

-- 创建库
create database if not exists ai_code_generator;

-- 切换库
use ai_code_generator;

-- 用户表
-- 以下是建表语句


-- User table
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment 'account',
    userPassword varchar(512)                           not null comment 'password',
    userName     varchar(256)                           null comment 'username',
    userAvatar   varchar(1024)                          null comment 'user avatar',
    userProfile  varchar(512)                           null comment 'user profile',
    userRole     varchar(256) default 'user'            not null comment 'user role: user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment 'edit time',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment 'create time',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'update time',
    isDelete     tinyint      default 0                 not null comment 'is deleted',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
    ) comment 'user' collate = utf8mb4_unicode_ci;

-- Application table
create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment 'Application name',
    cover        varchar(512)                       null comment 'Application cover image',
    initPrompt   text                               null comment 'Application initialization prompt',
    codeGenType  varchar(64)                        null comment 'Code generation type (enum)',
    deployKey    varchar(64)                        null comment 'Deployment identifier',
    deployedTime datetime                           null comment 'Deployment time',
    priority     int      default 0                 not null comment 'Priority',
    userId       bigint                             not null comment 'Creator user ID',
    editTime     datetime default CURRENT_TIMESTAMP not null comment 'Edit time',
    createTime   datetime default CURRENT_TIMESTAMP not null comment 'Creation time',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment 'Update time',
    isDelete     tinyint  default 0                 not null comment 'Is deleted',
    UNIQUE KEY uk_deployKey (deployKey), -- Ensure deployment identifier uniqueness
    INDEX idx_appName (appName),         -- Improve query performance based on application name
    INDEX idx_userId (userId)            -- Improve query performance based on user ID
) comment 'Application' collate = utf8mb4_unicode_ci;
