package com.lc.yunpicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lc.yunpicturebackend.annotation.AuthCheck;
import com.lc.yunpicturebackend.common.BaseResponse;
import com.lc.yunpicturebackend.common.ResultUtils;
import com.lc.yunpicturebackend.constant.UserConstant;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.dto.user.*;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.LoginUserVo;
import com.lc.yunpicturebackend.model.vo.UserVo;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController("/user")
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 注册用户
     * @param registerRequest 注册请求封装
     * @return 注册用户id
     */
    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest registerRequest) {
        if(ObjUtil.isEmpty(registerRequest)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"注册参数为空");
        }
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();

        long uid = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(uid);
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

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userAddRequest), new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        //加密过后的密码
        String encryptPassword = userService.getEncryptPassword(userAddRequest.getUserPassword());
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        user.setUserPassword(encryptPassword);
        boolean save = userService.save(user);
        if(!save){
            return ResultUtils.failure();
        }
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id获取用户 仅管理员
     * @param id 用户id
     * @return BaseResponse<User>
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id<=0), new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数错误"));
        User user = userService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(user), new BusinessException(ErrorCode.OPERATION_ERROR,"查询不到该用户"));

        return ResultUtils.success(user);
    }

    /**
     * 根据id获取包装类
     * @param id 用户id
     * @return BaseResponse<UserVo>
     */
    @PostMapping("/get/vo")
    public BaseResponse<UserVo> getUserVoById(long id) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(id<=0), new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数错误"));
        User originalUser = userService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(originalUser), new BusinessException(ErrorCode.OPERATION_ERROR,"查询不到该用户"));
        UserVo userVo = userService.getUserVo(originalUser);

        return ResultUtils.success(userVo);
    }

    /**
     * 更新用户
     * @param userUpdateRequest 用户更新请求
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userUpdateRequest) || userUpdateRequest.getId()<=0,
                new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        //只会更新那些非空的属性
        UpdateWrapper<User> updateWrapper = userService.getUpdateWrapper(userUpdateRequest);

        boolean update = userService.update(updateWrapper);
        ThrowUtils.throwIf(!update, new BusinessException(ErrorCode.OPERATION_ERROR,"更新失败"));

        return ResultUtils.success(update);
    }

    /**
     * 删除用户
     * @param userDeleteRequest 用户删除请求
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUserById(@RequestBody UserDeleteRequest userDeleteRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userDeleteRequest) || userDeleteRequest.getId()<=0,
                new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数错误"));
        long uid = userDeleteRequest.getId();
        boolean result = userService.removeById(uid);
        ThrowUtils.throwIf(!result, new BusinessException(ErrorCode.OPERATION_ERROR,"删除失败"));

        return ResultUtils.success(result);
    }

    /**
     * 分页获取用户封装列表
     * @return BaseResponse<IPage<UserVo>>
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<IPage<UserVo>> listUserVoByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userQueryRequest), new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        IPage<User> page = new Page<>(userQueryRequest.getCurrent(), userQueryRequest.getPageSize());
        QueryWrapper<User> queryWrapper = userService.getQueryWrapper(userQueryRequest);
        IPage<User> userPage = userService.page(page, queryWrapper);
        IPage<UserVo> userVoPage = new Page<>(userQueryRequest.getCurrent(), userQueryRequest.getPageSize(),userPage.getTotal());

        List<UserVo> userVoList = userService.getUserVoList(userPage.getRecords());
        userVoPage.setRecords(userVoList);

        return ResultUtils.success(userVoPage);
    }
}

