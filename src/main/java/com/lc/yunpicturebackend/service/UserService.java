package com.lc.yunpicturebackend.service;

import com.lc.yunpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lc.yunpicturebackend.model.vo.LoginUserVo;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author lianchao0921
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-03-06 14:48:32
*/
public interface UserService extends IService<User> {
    long userRegister(String userAccount, String userPassword, String checkPassword);

    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    LoginUserVo getLoginUserVo(User user);

    boolean userLogout(HttpServletRequest request);
}
