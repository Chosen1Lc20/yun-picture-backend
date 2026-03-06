package com.lc.yunpicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.lc.yunpicturebackend.annotation.AuthCheck;
import com.lc.yunpicturebackend.common.BaseResponse;
import com.lc.yunpicturebackend.common.ResultUtils;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.dto.user.UserLoginRequest;
import com.lc.yunpicturebackend.model.dto.user.UserRegisterRequest;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.LoginUserVo;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("/user")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 注册用户
     * @param registerRequest 注册请求封装
     * @return 注册用户id
     */
    @PostMapping("register")
    public long userRegister(@RequestBody UserRegisterRequest registerRequest) {
        if(ObjUtil.isEmpty(registerRequest)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"注册参数为空");
        }
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();

        return userService.userRegister(userAccount, userPassword, checkPassword);
    }

    /**
     * 用户登录
     * @param loginRequest 登录请求封装
     * @param request httpServletRequest
     * @return 通用结果
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(loginRequest), new BusinessException(ErrorCode.PARAMS_ERROR,"登录参数为空"));

        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        LoginUserVo loginUserVo = userService.userLogin(userAccount, userPassword, request);

        if(loginUserVo == null){
            return ResultUtils.failure();
        }
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 获取当前登录用户
     * @param request httpServletRequest
     * @return 通用结果<LoginUserVo>
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVo> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null){
            return ResultUtils.failure();
        }
        return ResultUtils.success(userService.getLoginUserVo(loginUser));
    }

    /**
     * 注销当前用户
     * @param request httpServletRequest
     * @return 通用结果<Boolean>
     */
    @GetMapping("/get/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null , new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
}

