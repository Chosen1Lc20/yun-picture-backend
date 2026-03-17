package com.lc.yunpicturebackend.service.impl;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.manager.FileManager;
import com.lc.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.lc.yunpicturebackend.model.dto.picture.PictureEditRequest;
import com.lc.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.lc.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.PictureFormatEnum;
import com.lc.yunpicturebackend.model.vo.PictureVo;
import com.lc.yunpicturebackend.model.vo.UserVo;
import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.mapper.PictureMapper;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
* @author lianchao0921
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-03-14 21:17:46
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    /**
     * 上传图片 注意上传图片时的后缀名是小写的,但是上传返回的结果的后缀名是大写的
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVo uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NOT_LOGIN_ERROR,"当前用户未登录");
        Long pid = null;
        //用于判断是新增还是更新图片
        if(pictureUploadRequest!=null){
            pid = pictureUploadRequest.getId();
        }
        //如果是更新图片,需要校验图片是否存在
        if(pid!=null){
            boolean exists = this.lambdaQuery().eq(Picture::getId, pid).exists();
            ThrowUtils.throwIf(!exists,new BusinessException(ErrorCode.OPERATION_ERROR,"图片不存在"));
        }
        //上传图片,得到信息
        //按照用户id划分目录
        String UploadPathPrefix = String.format("public/%s",loginUser.getId().toString());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, UploadPathPrefix);

        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //如果pid不为空,则是更新,否则是插入
        if(pid!=null){
            //如果是更新,则需要补充id和editTime
            picture.setId(pid);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        if(!result){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"上传图片操作失败");
        }
        return PictureVo.objToVo(picture);
    }

    /**
     * 根据 pictureQueryRequest 获得picture的queryWrapper
     * @param pictureQueryRequest 图片查询请求
     * @return queryWrapper
     */
    @Override
    public QueryWrapper<Picture> getQueryPictureWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        if(pictureQueryRequest==null){
            return pictureQueryWrapper;
        }
        //取属性
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        String searchText = pictureQueryRequest.getSearchText();
        String sortField = pictureQueryRequest.getSortField();
        //默认降序
        String sortOrder = pictureQueryRequest.getSortOrder();
        //tags列表转json串
        List<String> tags = pictureQueryRequest.getTags();
        //数据库中存储的类似这样["java","c++","javascript","javaee"]

        pictureQueryWrapper.eq(id !=null,"id", id);
        pictureQueryWrapper.like(StringUtils.isNotBlank(name),"name", name);
        pictureQueryWrapper.like(StringUtils.isNotBlank(introduction),"introduction", introduction);
        pictureQueryWrapper.like(picFormat !=null,"picFormat", picFormat);
        pictureQueryWrapper.eq(StringUtils.isNotBlank(category),"category", category);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picSize),"picSize", picSize);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picWidth),"picWidth", picWidth);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picHeight),"picHeight", picHeight);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picScale),"picScale", picScale);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(userId),"userId",userId);
        //searchText同时匹配name和introduction
        pictureQueryWrapper.and(StringUtils.isNotBlank(searchText),(wrapper)->{
            wrapper.like("name", searchText).or().like("introduction", searchText);
        });
        //JSON数组查询
        if(CollUtil.isNotEmpty(tags)){
            for (String tag:tags){
                pictureQueryWrapper.like("tags", "\""+tag+"\"");
            }
        }
        //排序
        pictureQueryWrapper.orderBy(StringUtils.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        return pictureQueryWrapper;
    }

    /**
     *  //todo 写注释
     * @param picture
     * @return
     */
    @Override
    public PictureVo getPictureVo(Picture picture, HttpServletRequest request) {

        PictureVo pictureVo = PictureVo.objToVo(picture);
        //创建图片的用户id
        Long userId = pictureVo.getUserId();
        ThrowUtils.throwIf(ObjectUtil.isNull(userId),new BusinessException(ErrorCode.SYSTEM_ERROR,"创建图片用户id不存在"));
        //创建图片的用户
        User user = userService.getById(userId);
        UserVo userVo = userService.getUserVo(user);
        pictureVo.setUser(userVo);

        return pictureVo;
    }

    @Override
    public Page<PictureVo> getPictureVoPage(Page<Picture> page, HttpServletRequest request) {
        List<Picture> pictureList = page.getRecords();
        Page<PictureVo> pictureVoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if(CollUtil.isNotEmpty(pictureList)){
            return pictureVoPage;
        }
        List<PictureVo> pictureVoList =
                page.getRecords().stream().map(PictureVo::objToVo).collect(Collectors.toList());
        //先查询图片列表里所有的用户id,要不重复,所以选择set
        Set<Long> UidSet = pictureList.stream().map(Picture::getId).collect(Collectors.toSet());
        //将用户id和用户关联起来,这样只需查询一次数据库
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(UidSet).stream().collect(Collectors.groupingBy((User::getId)));
        //pictureVo 有userVo属性需要关联
        for(PictureVo pictureVo:pictureVoList){
            if(userIdUserListMap.containsKey(pictureVo.getId())){
                User user = userIdUserListMap.get(pictureVo.getId()).get(0);
                UserVo userVo = userService.getUserVo(user);
                pictureVo.setUser(userVo);
            }
        }

        pictureVoPage.setRecords(pictureVoList);

        return pictureVoPage;
    }
    //什么样的图片算是有效??
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

}




