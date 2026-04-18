package com.lc.yunpicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.lc.yunpicturebackend.annotation.AuthCheck;
import com.lc.yunpicturebackend.common.BaseResponse;
import com.lc.yunpicturebackend.common.ResultUtils;
import com.lc.yunpicturebackend.constant.UserConstant;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.dto.space.analyze.*;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.analyze.*;
import com.lc.yunpicturebackend.service.SpaceAnalyzeService;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/SpaceAnalyze")
@RestController
@Slf4j
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @Resource
    private UserService userService;

    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyzeResponse(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                     HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest==null, ErrorCode.PARAMS_ERROR,"空间用途分析请求为空");
        User loginUser = userService.getLoginUser(httpServletRequest);
        SpaceUsageAnalyzeResponse spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyzeResponse(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                                            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceCategoryAnalyzeRequest), ErrorCode.PARAMS_ERROR,"空间图片分类分析请求为空");
        User loginUser = userService.getLoginUser(httpServletRequest);
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NOT_LOGIN_ERROR,"当前用户未登录");
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceCategoryAnalyze);
    }

    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyzeResponse(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                                  HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceTagAnalyzeRequest), ErrorCode.PARAMS_ERROR,"空间图片标签分析请求为空");
        User loginUser = userService.getLoginUser(httpServletRequest);
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NOT_LOGIN_ERROR,"当前用户未登录>");
        List<SpaceTagAnalyzeResponse> spaceTagAnalyze = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceTagAnalyze);
    }

    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyzeResponse(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                                    HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceSizeAnalyzeRequest), ErrorCode.PARAMS_ERROR,"空间图片大小分析请求为空");
        User loginUser = userService.getLoginUser(httpServletRequest);
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NOT_LOGIN_ERROR,"当前用户未登录");
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceSizeAnalyze);
    }

    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyzeResponse(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest,
                                                                                    HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUserAnalyzeRequest), ErrorCode.PARAMS_ERROR,"空间图片用户分析请求为空");
        User loginUser = userService.getLoginUser(httpServletRequest);
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NOT_LOGIN_ERROR,"当前用户未登录");
        List<SpaceUserAnalyzeResponse> spaceUserAnalyze = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUserAnalyze);
    }

    @PostMapping("/rank")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<Space>> getSpaceRankAnalyzeResponse(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                                                    HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceRankAnalyzeRequest), ErrorCode.PARAMS_ERROR,"空间图片大小分析请求为空");
        User loginUser = userService.getLoginUser(httpServletRequest);
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NOT_LOGIN_ERROR,"当前用户未登录");
        List<Space> spaceUserAnalyze = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUserAnalyze);
    }
}
