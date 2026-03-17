package com.lc.yunpicturebackend.service.impl;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.dto.user.UserAddRequest;
import com.lc.yunpicturebackend.model.dto.user.UserQueryRequest;
import com.lc.yunpicturebackend.model.dto.user.UserUpdateRequest;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.UserRoleEnum;
import com.lc.yunpicturebackend.model.vo.LoginUserVo;
import com.lc.yunpicturebackend.model.vo.UserVo;
import com.lc.yunpicturebackend.service.UserService;
import com.lc.yunpicturebackend.mapper.UserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import static com.lc.yunpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author lianchao0921
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2026-03-06 14:48:32
*/
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper;

    /**
     * 注册
     * @param userAccount 用户提交的账户
     * @param userPassword 用户提交的密码
     * @param checkPassword 用户提交的二次密码
     * @return 注册用户的id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {

        //首先校验参数是否为空
        if(StrUtil.hasBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        //对参数进行校验
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名过短");
        }
        if(userAccount.length() > 20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名过长");
        }
        if(userPassword.length() < 6){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        if( !userPassword.equals(checkPassword) ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次密码不一致");
        }
        //检查用户是否重复
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);

        User user = userMapper.selectOne(userQueryWrapper);
        if(user != null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该账户已注册");
        }

        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(getEncryptPassword(userPassword));
        newUser.setUserName("");
        newUser.setUserAvatar("");
        newUser.setUserProfile("");
        newUser.setUserRole(UserRoleEnum.USER.getValue());
        newUser.setEditTime(new Date());
        newUser.setCreateTime(new Date());
        newUser.setUpdateTime(new Date());
        newUser.setIsDelete(0);

        int insert = userMapper.insert(newUser);
        if( insert <=0 ) {
            //插入失败
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败,数据库错误");
        }

        return newUser.getId();
    }

    /**
     * 用户登录
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request httpServletRequest
     * @return 登录用户视图
     */
    @Override
    public LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if(StrUtil.hasBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名错误");
        }

        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码错误");
        }
        String encryptPassword = getEncryptPassword(userPassword);

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount).eq("userPassword", encryptPassword);

        User curUser = userMapper.selectOne(userQueryWrapper);
        //当前用户不存在或者密码错误
        if(curUser == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户不存在或密码错误");
        }
        request.getSession().setAttribute(USER_LOGIN_STATE, curUser);
        return getLoginUserVo(curUser);
    }

    /**
     * 获取登录用户
     * @param request httpServletRequest
     * @return 用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(loginUser == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到当前登录用户");
        }
        Long loginUserId = loginUser.getId();
        return userMapper.selectById(loginUserId);
    }

    /**
     * 用户注销
     * @param request httpServletRequest
     * @return true 或 false
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(user == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前用户未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 对密码进行加密
     * @param userPassword 用户提交的密码
     * @return 加密过后的密码
     */
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "nanfeng";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 用户脱敏
     * @param originalUser 初始用户
     * @return 脱敏后的用户
     */
    public User getSaftyUser(User originalUser) {
        
        User newUser = new User();
        newUser.setId(originalUser.getId());
        newUser.setUserAccount(originalUser.getUserAccount());
        newUser.setUserPassword("");
        newUser.setUserName(originalUser.getUserName());
        newUser.setUserAvatar(originalUser.getUserAvatar());
        newUser.setUserProfile(originalUser.getUserProfile());
        newUser.setUserRole(originalUser.getUserRole());
        
        return newUser;
    }

    /**
     * 获取登录用户视图
     * @param user 当前用户
     * @return 登录用户视图
     */
    @Override
    public LoginUserVo getLoginUserVo(User user) {
        if(user == null){
            return null;
        }
        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtils.copyProperties(user, loginUserVo);
        return loginUserVo;
    }

    /**
     * 获取用户视图
     * @param user 要查询的用户
     * @return 用户视图
     */
    @Override
    public UserVo getUserVo(User user) {
        if(user == null){
            return null;
        }
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }

    /**
     * 获取用户视图列表
     * @param userList 要查询的用户列表
     * @return 用户视图列表
     */
    @Override
    public List<UserVo> getUserVoList(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return null;
        }

        return userList.stream().map(this::getUserVo).collect(Collectors.toList());
    }

    /**
     * 根据 userQueryRequest 获取对应的queryWrapper
     * @param userQueryRequest 用户查询请求
     * @return QueryWrapper<User>
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 根据 userUpdateRequest 获取对应的queryWrapper
     * @param userUpdateRequest 用户更新请求
     * @return QueryWrapper<User>
     */
    public UpdateWrapper<User> getUpdateWrapper(UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userUpdateRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        Long id = userUpdateRequest.getId();
        String userName = userUpdateRequest.getUserName();
        String userAvatar = userUpdateRequest.getUserAvatar();
        String userProfile = userUpdateRequest.getUserProfile();
        String userRole = userUpdateRequest.getUserRole();
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        userUpdateWrapper.set(StrUtil.isNotBlank(userRole), "userRole", userRole);
        userUpdateWrapper.set(StrUtil.isNotBlank(userAvatar), "userAccount", userAvatar);
        userUpdateWrapper.set(StrUtil.isNotBlank(userName), "userName", userName);
        userUpdateWrapper.set(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        return userUpdateWrapper;
    }

    /**
     * 判断用户是否为管理员
     * @param user 用户
     * @return true or false
     */
    public boolean isAdmin(User user) {
        return user !=null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}




