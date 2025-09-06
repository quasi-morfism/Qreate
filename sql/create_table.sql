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
