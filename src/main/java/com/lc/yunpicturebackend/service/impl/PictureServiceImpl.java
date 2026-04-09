package com.lc.yunpicturebackend.service.impl;
import java.io.IOException;
import java.util.*;
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
import com.lc.yunpicturebackend.manager.CosManager;
import com.lc.yunpicturebackend.manager.FileManager;
import com.lc.yunpicturebackend.manager.upload.FilePictureUpload;
import com.lc.yunpicturebackend.manager.upload.UrlPictureUpload;
import com.lc.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.lc.yunpicturebackend.model.dto.picture.*;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.PictureFormatEnum;
import com.lc.yunpicturebackend.model.enums.ReviewStatusEnum;
import com.lc.yunpicturebackend.model.vo.PictureVo;
import com.lc.yunpicturebackend.model.vo.UserVo;
import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.mapper.PictureMapper;
import com.lc.yunpicturebackend.service.SpaceService;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
* @author lianchao0921
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-03-14 21:17:46
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    /**
     * 上传图片(支持图片编辑) 注意上传图片时的后缀名是小写的,但是上传返回的结果的后缀名是大写的
     * 支持本地文件上传和 url上传
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVo uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NOT_LOGIN_ERROR,"当前用户未登录");
        Long spaceId = pictureUploadRequest.getSpaceId();
        //校验空间是否存在
        if(spaceId!=null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"空间不存在");
            //必须是空间创建者(本人)才能上传
            if(!space.getUserId().equals(loginUser.getId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            if(space.getMaxSize()<space.getTotalSize()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"空间已满");
            }
            if(space.getMaxCount()<space.getTotalCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"空间已满");
            }
        }
        Long pid = pictureUploadRequest.getId();
        //如果是更新图片,需要校验图片是否存在 传递的spaceId是否和原图片的spaceId一致,若未传,复用原有的
        if(pid!=null && pid>0){
            Picture oldPicture = this.getById(pid);
            boolean exists = this.lambdaQuery().eq(Picture::getId, pid).exists();
            ThrowUtils.throwIf(!exists,new BusinessException(ErrorCode.OPERATION_ERROR,"图片不存在"));

            if(spaceId==null){
                //上传到公共图库
                spaceId = oldPicture.getSpaceId();
            }else {
              //传了spaceId 必须和原有图片一致
                ThrowUtils.throwIf(spaceId.equals(oldPicture.getSpaceId()),ErrorCode.PARAMS_ERROR,"空间id不一致");
            }
        }
        //根据spaceId 判断上传到哪?
        String UploadPathPrefix;
        if(spaceId==null){
            //上传到公共图库
            UploadPathPrefix = String.format("public/%s",loginUser.getId().toString());
        }else {
            //上传到用户私有空间
            UploadPathPrefix = String.format("space/%s",spaceId);
        }

        //新增图片,上传到COS,得到信息
        //按照用户id划分目录
        UploadPictureResult uploadPictureResult = null;
        if(inputSource instanceof MultipartFile){
             uploadPictureResult = filePictureUpload.uploadPicture((MultipartFile)inputSource,UploadPathPrefix);
        }
        if(inputSource instanceof String){
            uploadPictureResult = urlPictureUpload.uploadPicture((String)inputSource,UploadPathPrefix);
        }
        //图片名称
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setName(uploadPictureResult.getPicName());
        if(StringUtils.isNotBlank(pictureUploadRequest.getPicName())){
            picture.setName(pictureUploadRequest.getPicName());
        }
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //补充设置spaceId
        picture.setSpaceId(spaceId);
        //补充审核参数
        this.fillReviewParams(picture,loginUser);

        //如果pid不为空,则是更新,否则是插入
        if(pid!=null){
            //仅本人和管理员可以更新
            Picture oldPicture = this.getById(pid);
            if(!userService.isAdmin(loginUser) && !loginUser.getId().equals(oldPicture.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //如果是更新,则需要补充id和editTime
            picture.setId(pid);
            picture.setEditTime(new Date());
        }
        Long finalSpaceID = spaceId;
        boolean result = this.saveOrUpdate(picture);
        if(!result){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"上传图片操作失败");
        }
        if(ObjectUtil.isNotNull(finalSpaceID)){
            boolean update = spaceService.lambdaUpdate().
                    eq(Space::getId, finalSpaceID)
                    .setSql("totalSize = totalSize + " + picture.getPicSize())
                    .setSql("totalCount = totalCount +  1 ")
                    .update();
            ThrowUtils.throwIf(!update,new BusinessException(ErrorCode.OPERATION_ERROR,"更新额度失败"));
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
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        //默认降序
        String sortOrder = pictureQueryRequest.getSortOrder();
        //tags列表转json串
        List<String> tags = pictureQueryRequest.getTags();
        //数据库中存储的类似这样["java","c++","javascript","javaee"]

        pictureQueryWrapper.eq(id !=null,"id", id);
        pictureQueryWrapper.like(StringUtils.isNotBlank(name),"name", name);
        pictureQueryWrapper.like(StringUtils.isNotBlank(introduction),"introduction", introduction);
        pictureQueryWrapper.like(StringUtils.isNotBlank(picFormat),"picFormat", picFormat);
        pictureQueryWrapper.eq(StringUtils.isNotBlank(category),"category", category);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picSize),"picSize", picSize);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picWidth),"picWidth", picWidth);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picHeight),"picHeight", picHeight);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(picScale),"picScale", picScale);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(userId),"userId",userId);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(reviewStatus),"reviewStatus",reviewStatus);
        pictureQueryWrapper.like(StringUtils.isNotBlank(reviewMessage),"reviewMessage", reviewMessage);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(reviewerId),"reviewerId", reviewerId);
        pictureQueryWrapper.eq(ObjectUtil.isNotNull(spaceId),"spaceId", spaceId);
        pictureQueryWrapper.ge(ObjectUtil.isNotNull(startEditTime),"editTime", startEditTime);
        pictureQueryWrapper.lt(ObjectUtil.isNotNull(endEditTime),"editTime", endEditTime);

        //nullSpaceId为true时,拼接 is Null 条件
        pictureQueryWrapper.isNull(nullSpaceId,"spaceId");
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
     *  //获取图片封装类 ,封装了用户信息
     * @param picture
     * @return
     */
    @Override
    public PictureVo getPictureVo(Picture picture) {

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

    /**
     * todo 写注释
     * @param page
     * @param request
     * @return
     */
    @Override
    public Page<PictureVo> getPictureVoPage(Page<Picture> page, HttpServletRequest request) {
        List<Picture> pictureList = page.getRecords();
        Page<PictureVo> pictureVoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if(CollUtil.isEmpty(pictureList)){
            return pictureVoPage;
        }
        List<PictureVo> pictureVoList =
                page.getRecords().stream().map(PictureVo::objToVo).collect(Collectors.toList());
        //先查询图片列表里所有的用户id,要不重复,所以选择set
        Set<Long> UidSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        //将用户id和用户关联起来,这样只需查询一次数据库
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(UidSet).stream().collect(Collectors.groupingBy((User::getId)));
        //pictureVo 有userVo属性需要关联
        for(PictureVo pictureVo:pictureVoList){
            if(userIdUserListMap.containsKey(pictureVo.getUserId())){
                //get(0)是取用户,一个id只对应一个用户
                User user = userIdUserListMap.get(pictureVo.getUserId()).get(0);
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

    /**
     * 图片审核功能 (仅管理员)
     * @param pictureReviewRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureReviewRequest), ErrorCode.PARAMS_ERROR,"请求参数错误");
        Long id = pictureReviewRequest.getId();
        //2.校验用户权限
        ThrowUtils.throwIf(!userService.isAdmin(loginUser),new BusinessException(ErrorCode.NO_AUTH_ERROR));
        //老图
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(oldPicture),ErrorCode.PARAMS_ERROR,"图片不存在");
        //3.不能修改相同的审核状态
        if(oldPicture.getReviewStatus().equals(pictureReviewRequest.getReviewStatus())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"请勿重复审核");
        }
        //4.操作数据库
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        //这里为什么不用oldPicture,而是重新new了一个picture?
        //原因是mybatis在更新时,默认只更新非空字段。oldPicture的字段都有值,在更新时会全部传过去,重新提交一遍
        //而picture只有部分字段有值,在更新时只会提交部分字段,性能要好一些
        return this.updateById(picture);
    }

    /**
     * 设置审核状态 (管理员审核的图片自动过审)
     */
    @Override
    public void fillReviewParams(Picture picture,User loginUser){
        if(userService.isAdmin(loginUser)){
            picture.setReviewStatus(ReviewStatusEnum.PASS.getStatus());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            //非管理员,创建和编辑图片都要改为待审核
            picture.setReviewStatus(ReviewStatusEnum.REVIEWING.getStatus());
        }
    }

    @Override
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.校验请求参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        int searchCount = pictureUploadByBatchRequest.getSearchCount();
        ThrowUtils.throwIf(searchCount > 30 || searchCount<1, ErrorCode.PARAMS_ERROR,"请求数量不能超过30条,不能小于1");
        ThrowUtils.throwIf(StringUtils.isBlank(searchText),ErrorCode.PARAMS_ERROR,"关键词不能为空");
        //2.拼接url地址
        //https://cn.bing.com/images/async?q=%E4%B8%96%E7%95%8C%E6%97%85%E6%B8%B8%E8%83%9C%E5%9C%B0&mmasync=1
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {
            //3.调用接口
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取连接失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if(ObjectUtil.isNull(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取元素失败");
        }
        Elements elements = div.select("img.mimg");
        int uploadCount = 0;
        for(Element imgElement : elements){
            String fileUrl = imgElement.attr("src");
            if(StringUtils.isBlank(fileUrl)){
                log.info("无法获取当前图片:{},已跳过,继续获取下一张",fileUrl);
                continue;
            }
            //处理图片上传地址,防止出现转义问题
            int questionPos = fileUrl.indexOf("?");
            if(questionPos > -1){
                fileUrl = fileUrl.substring(0, questionPos);
            }
            //4.上传图片
            try{
                String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
                if(StringUtils.isBlank(namePrefix)){
                    namePrefix = searchText;
                }
                PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
                pictureUploadRequest.setPicName(namePrefix+(uploadCount+1));
                PictureVo pictureVo = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                uploadCount++;
                log.info("图片上传成功,图片id:{}",pictureVo.getId());
            }
            catch (Exception e){
                log.info("图片上传失败",e);
                continue;
            }
            if(uploadCount>=searchCount){
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        String picUrl = oldPicture.getUrl();
        int startIndex = picUrl.indexOf("public");
        cosManager.deleteObject(picUrl.substring(startIndex));
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl.substring(startIndex));
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员，也就是本人可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public boolean deletePictureById(Picture pictureToDel, User loginUser) {
        //管理员和图片创建者才可以删除
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureToDel),new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在"));
        Long pid = pictureToDel.getId();
        this.checkPictureAuth(loginUser,pictureToDel);
        //有权限删除
        transactionTemplate.execute((status)->{
            boolean result = this.removeById(pid);
            ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"删除失败");
            Long finalSpaceId = pictureToDel.getSpaceId();
            if(finalSpaceId!=null){
                boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize - " + pictureToDel.getPicSize())
                        .setSql("totalCount = totalCount -  1")
                        .update();
                ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"更新额度失败");
            }
            return true;
        });
        //异步清理文件
        this.clearPictureFile(pictureToDel);
        return true;
    }

    /**
     * 编辑图片 只能更改自己空间的图片
     * @param pictureEditRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        Long id = pictureEditRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0,new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在"));
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(oldPicture),new BusinessException(ErrorCode.NOT_FOUND_ERROR,"更新的图片不存在"));
        //图片存在
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        //将list转为json串
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //不同于更新update,编辑需要设置editTime
        picture.setEditTime(new Date());
        //校验图片
        this.validPicture(picture);
        //补充审核参数
        this.fillReviewParams(picture,loginUser);
        //校验权限
        //之前oldPicture的位置 传成了 picture。 导致在编辑图片时候出现严重的BUG:上传完图片后无法编辑
        this.checkPictureAuth(loginUser,oldPicture);
        //有权限,操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result,new BusinessException(ErrorCode.OPERATION_ERROR,"更新图片失败"));
        return true;
    }
}



