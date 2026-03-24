package com.lc.yunpicturebackend.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lc.yunpicturebackend.annotation.AuthCheck;
import com.lc.yunpicturebackend.common.BaseResponse;
import com.lc.yunpicturebackend.common.DeleteRequest;
import com.lc.yunpicturebackend.common.ResultUtils;
import com.lc.yunpicturebackend.constant.UserConstant;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.manager.CosManager;
import com.lc.yunpicturebackend.model.dto.picture.*;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.PictureTagCategory;
import com.lc.yunpicturebackend.model.vo.PictureVo;
import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
@Slf4j
@RestController("/picture")
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private CosManager cosManager;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 图片上传
     * @param file 文件
     * @param pictureUploadRequest 图片上传请求
     * @param request HttpServletRequest
     * @return BaseResponse<PictureVo>
     */
    @Operation(summary = "图片上传", description = "multipart/form-data 格式上传图片，附带图片信息")
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE // 核心：明确接口只接收文件上传格式
    )
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVo> uploadPicture(
            @RequestPart("file") MultipartFile file,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(file==null,new BusinessException(ErrorCode.PARAMS_ERROR,"文件为空"));
        User loginUser = userService.getLoginUser(request);
        PictureVo pictureVo = pictureService.uploadPicture(file, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVo);
    }

    /**
     * 删除图片 (管理员和图片创建者可以删除)
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePictureById(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(deleteRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0,new BusinessException(ErrorCode.PARAMS_ERROR,"参数传递错误"));
        //管理员和图片创建者才可以删除
        User loginUser = userService.getLoginUser(request);
        Picture pictureToDel = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureToDel),new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在"));
        if(!userService.isAdmin(loginUser)){
            if(!pictureToDel.getUserId().equals(loginUser.getId()) ){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"当前用户无权删除图片");
            }
        }
        //有权限删除
        boolean result = pictureService.removeById(id);
        return result ? ResultUtils.success(result):ResultUtils.failure();
    }

    /**
     * 更新图片 (仅管理员使用)
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureUpdateRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        Long id = pictureUpdateRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0,new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在"));
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        //将list转为json串
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        //校验图片
        pictureService.validPicture(picture);
        //判断图片是否存在
        Picture QueryedPicture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(QueryedPicture),new BusinessException(ErrorCode.OPERATION_ERROR,"更新的图片不存在"));
        //操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result,new BusinessException(ErrorCode.OPERATION_ERROR,"更新图片失败"));
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取图片封装类
     * @param pictureQueryRequest 图片查询请求
     * @return BaseResponse<PictureVo>
     */
    @PostMapping("/get/vo")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVo> getPictureVoById(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        Long id = pictureQueryRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0 ,new BusinessException(ErrorCode.PARAMS_ERROR,"传递参数错误>"));
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),new BusinessException(ErrorCode.OPERATION_ERROR,"要查询的图片不存在"));

        PictureVo pictureVo = PictureVo.objToVo(picture);
        return ResultUtils.success(pictureVo);
    }

    /**
     * 根据id获取图片封装类
     * @param pictureQueryRequest 图片查询请求
     * @return BaseResponse<Picture>
     */
    @PostMapping("/get")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        Long id = pictureQueryRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0 ,new BusinessException(ErrorCode.PARAMS_ERROR,"传递参数错误>"));
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),new BusinessException(ErrorCode.OPERATION_ERROR,"要查询的图片不存在"));

        return ResultUtils.success(picture);
    }

    /**
     * 分页获取图片列表 (仅管理员可用)
     * @param pictureQueryRequest 图片查询请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPagePicture(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        QueryWrapper<Picture> queryPictureWrapper = pictureService.getQueryPictureWrapper(pictureQueryRequest);
        Page<Picture> picturePage = new Page<>(current, pageSize);

        Page<Picture> pageResult = pictureService.page(picturePage,queryPictureWrapper);

        return ResultUtils.success(pageResult);
    }

    @PostMapping("list/page/vo")
    public BaseResponse<Page<PictureVo>> listPagePictureVo(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize>30,new BusinessException(ErrorCode.PARAMS_ERROR,"参数错误"));
        QueryWrapper<Picture> queryPictureWrapper = pictureService.getQueryPictureWrapper(pictureQueryRequest);

        Page<Picture> picturePage = new Page<>(current, pageSize);
        Page<Picture> picturePageResult = pictureService.page(picturePage,queryPictureWrapper);

        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePageResult, request);
        return ResultUtils.success(pictureVoPage);
    }

    /**
     * 编辑图片 (给用户使用)
     */
    @PostMapping("edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureEditRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        Long id = pictureEditRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0,new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在"));
        //判断图片是否存在
        Picture queryedPicture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(queryedPicture),new BusinessException(ErrorCode.NOT_FOUND_ERROR,"更新的图片不存在"));
        //图片存在
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        //将list转为json串
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //不同于更新update,编辑需要设置editTime
        picture.setEditTime(new Date());
        //校验图片
        pictureService.validPicture(picture);
        //仅本人或管理员可以操作
        User loginUser = userService.getLoginUser(request);
        if(!userService.isAdmin(loginUser)) {
            //不是管理员,也不是本人,抛出异常
            if (!queryedPicture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前用户没有权限编辑图片");
            }
        }
        //有权限,操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result,new BusinessException(ErrorCode.OPERATION_ERROR,"更新图片失败"));
        return ResultUtils.success(result);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意", "二次元");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

}
