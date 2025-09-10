package com.morfism.aiappgenerator.service;

import com.morfism.aiappgenerator.model.dto.user.UserQueryRequest;
import com.morfism.aiappgenerator.model.vo.LoginUserVO;
import com.morfism.aiappgenerator.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.morfism.aiappgenerator.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * user 服务层。
 *
 * @author Morfism
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @param user 用户实体对象
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      HTTP请求对象，用于存储登录状态
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request HTTP请求对象，用于获取登录状态
     * @return 当前登录的用户实体对象
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request HTTP请求对象，用于清除登录状态
     * @return 退出登录是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 将用户实体对象转换为视图对象
     *
     * @param user 用户实体对象
     * @return 用户视图对象，隐藏敏感信息
     */
    UserVO getUserVO(User user);

    /**
     * 将用户实体列表转换为视图对象列表
     *
     * @param userList 用户实体列表
     * @return 用户视图对象列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 根据查询请求构建查询条件包装器
     *
     * @param userQueryRequest 用户查询请求对象
     * @return 查询条件包装器，用于数据库查询
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取加密后的密码
     *
     * @param userPassword 原始密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 检查密码是否匹配
     *
     * @param plainPassword 明文密码
     * @param hashedPassword 加密后的密码
     * @return 密码是否匹配
     */
    boolean checkPassword(String plainPassword, String hashedPassword);

}
