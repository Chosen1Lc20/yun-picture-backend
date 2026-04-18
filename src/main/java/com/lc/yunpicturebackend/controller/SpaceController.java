package com.lc.yunpicturebackend.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lc.yunpicturebackend.annotation.AuthCheck;
import com.lc.yunpicturebackend.common.BaseResponse;
import com.lc.yunpicturebackend.common.DeleteRequest;
import com.lc.yunpicturebackend.common.ResultUtils;
import com.lc.yunpicturebackend.constant.KeyConstant;
import com.lc.yunpicturebackend.constant.UserConstant;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.manager.CosManager;
import com.lc.yunpicturebackend.model.dto.space.*;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.ReviewStatusEnum;
import com.lc.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.lc.yunpicturebackend.model.vo.SpaceLevel;
import com.lc.yunpicturebackend.model.vo.SpaceVo;
import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.service.SpaceService;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 创建空间 最多只能创建一个
     * @param spaceAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceAddRequest),ErrorCode.PARAMS_ERROR,"添加空间参数为空");
        User loginUser = userService.getLoginUser(request);
        long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        ThrowUtils.throwIf(spaceId == -1 , ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(spaceId);
    }

    /**
     * 删除空间 (管理员和空间创建者可以删除)
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpaceById(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(deleteRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0,new BusinessException(ErrorCode.PARAMS_ERROR,"参数传递错误"));
        //管理员和空间创建者才可以删除
        User loginUser = userService.getLoginUser(request);
        Space spaceToDel = spaceService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceToDel),new BusinessException(ErrorCode.PARAMS_ERROR,"空间不存在"));
        //校验权限,仅管理员或者本人可以操作
        spaceService.checkSpaceAuth(spaceToDel,loginUser);
        //有权限删除
        boolean result = spaceService.removeById(id);
        return result ? ResultUtils.success(result):ResultUtils.failure();
    }

    /**
     * 更新空间 (仅管理员使用)
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceUpdateRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        Long id = spaceUpdateRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0,new BusinessException(ErrorCode.PARAMS_ERROR,"空间不存在"));
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        //校验空间
        spaceService.validSpace(space,false);
        //自动补充参数
        spaceService.fillSpaceBySpaceLevel(space);
        //判断空间是否存在
        Space QueryedSpace = spaceService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(QueryedSpace),new BusinessException(ErrorCode.OPERATION_ERROR,"更新的空间不存在"));
        //操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result,new BusinessException(ErrorCode.OPERATION_ERROR,"更新空间失败"));
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取空间封装类
     * @param spaceQueryRequest 空间查询请求
     * @return BaseResponse<SpaceVo>
     */
    @PostMapping("/get/vo")
    public BaseResponse<SpaceVo> getSpaceVoById(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        Long id = spaceQueryRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0 ,new BusinessException(ErrorCode.PARAMS_ERROR,"传递参数错误>"));
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(space),new BusinessException(ErrorCode.OPERATION_ERROR,"要查询的空间不存在"));

        SpaceVo spaceVo = spaceService.getSpaceVo(space);
        return ResultUtils.success(spaceVo);
    }

    /**
     * 根据id获取空间 管理员使用
     * @param spaceQueryRequest 空间查询请求
     * @return BaseResponse<Space>
     */
    @PostMapping("/get")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        Long id = spaceQueryRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0 ,new BusinessException(ErrorCode.PARAMS_ERROR,"传递参数错误>"));
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(space),new BusinessException(ErrorCode.OPERATION_ERROR,"要查询的空间不存在"));

        return ResultUtils.success(space);
    }

    /**
     * 分页获取空间列表 (仅管理员可用)
     * @param spaceQueryRequest 空间查询请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listPageSpace(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        long current = spaceQueryRequest.getCurrent();
        long pageSize = spaceQueryRequest.getPageSize();
        QueryWrapper<Space> querySpaceWrapper = spaceService.getQuerySpaceWrapper(spaceQueryRequest);
        Page<Space> spacePage = new Page<>(current, pageSize);

        Page<Space> pageResult = spaceService.page(spacePage,querySpaceWrapper);

        return ResultUtils.success(pageResult);
    }

    /**
     * 分页查询 Space 给用户用的 (只展示已过审的空间)
     * @param spaceQueryRequest
     * @param request
     * @return
     */
    @PostMapping("list/page/vo")
    public BaseResponse<Page<SpaceVo>> listPageSpaceVo(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        long current = spaceQueryRequest.getCurrent();
        long pageSize = spaceQueryRequest.getPageSize();

        QueryWrapper<Space> querySpaceWrapper = spaceService.getQuerySpaceWrapper(spaceQueryRequest);

        Page<Space> spacePage = new Page<>(current, pageSize);
        Page<Space> spacePageResult = spaceService.page(spacePage,querySpaceWrapper);

        Page<SpaceVo> spaceVoPage = spaceService.getSpaceVoPage(spacePageResult);
        return ResultUtils.success(spaceVoPage);
    }
    
    /**
     * 编辑空间 (给用户使用)
     */
    @PostMapping("edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceEditRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        Long id = spaceEditRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0,new BusinessException(ErrorCode.PARAMS_ERROR,"空间不存在"));
        //判断空间是否存在
        Space queryedSpace = spaceService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(queryedSpace),new BusinessException(ErrorCode.NOT_FOUND_ERROR,"更新的空间不存在"));
        //空间存在
        Space oldSpace = new Space();
        BeanUtils.copyProperties(spaceEditRequest, oldSpace);
        //不同于更新update,编辑需要设置editTime
        oldSpace.setEditTime(new Date());
        //校验空间
        spaceService.validSpace(oldSpace,false);
        //仅本人或管理员可以操作
        User loginUser = userService.getLoginUser(request);
        spaceService.checkSpaceAuth(oldSpace,loginUser);
        //补充审核参数
        spaceService.fillSpaceBySpaceLevel(oldSpace);
        //有权限,操作数据库
        boolean result = spaceService.updateById(oldSpace);
        ThrowUtils.throwIf(!result,new BusinessException(ErrorCode.OPERATION_ERROR,"更新空间失败"));
        return ResultUtils.success(result);
    }

    @GetMapping("/spaceLevel")
    public BaseResponse<List<SpaceLevel>> getSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()).map(spaceLevelEnum ->
                    new SpaceLevel(
                    spaceLevelEnum.getValue(),
                    spaceLevelEnum.getText(),
                    spaceLevelEnum.getMaxSize(),
                    spaceLevelEnum.getMaxCount()
            )
        ).collect(Collectors.toList());

        return ResultUtils.success(spaceLevelList);
    }

}
