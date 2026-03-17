package com.lc.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lc.yunpicturebackend.model.dto.user.UserAddRequest;
import com.lc.yunpicturebackend.model.dto.user.UserQueryRequest;
import com.lc.yunpicturebackend.model.dto.user.UserUpdateRequest;
import com.lc.yunpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lc.yunpicturebackend.model.vo.LoginUserVo;
import com.lc.yunpicturebackend.model.vo.UserVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author lianchao0921
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-03-06 14:48:32
*/
public interface UserService extends IService<User> {
    long userRegister(String userAccount, String userPassword, String checkPassword);

    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    LoginUserVo getLoginUserVo(User user);

    UserVo getUserVo(User user);

    List<UserVo> getUserVoList(List<User> userList);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    String getEncryptPassword(String userPassword);

    User getSaftyUser(User originalUser);

    UpdateWrapper<User> getUpdateWrapper(UserUpdateRequest userUpdateRequest);

    boolean isAdmin(User user);
}
