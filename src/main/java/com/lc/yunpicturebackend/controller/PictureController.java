package com.lc.yunpicturebackend.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lc.yunpicturebackend.annotation.AuthCheck;
import com.lc.yunpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.lc.yunpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.lc.yunpicturebackend.common.BaseResponse;
import com.lc.yunpicturebackend.common.DeleteRequest;
import com.lc.yunpicturebackend.common.ResultUtils;
import com.lc.yunpicturebackend.constant.KeyConstant;
import com.lc.yunpicturebackend.constant.UserConstant;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.dto.picture.*;
import com.lc.yunpicturebackend.model.dto.picture.batch.PictureEditRequestByBatch;
import com.lc.yunpicturebackend.model.dto.picture.batch.PictureUploadByBatchRequest;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.PictureFormatEnum;
import com.lc.yunpicturebackend.model.enums.ReviewStatusEnum;
import com.lc.yunpicturebackend.model.vo.PictureTagCategory;
import com.lc.yunpicturebackend.model.vo.PictureVo;
import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.service.SpaceService;
import com.lc.yunpicturebackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
//https://picsum.photos/200 可以用来测试的接口,随机返回一张图片
@Slf4j
@RestController("/picture")
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    /**
     * 图片上传 (管理员和用户都可以上传图片)
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
    public BaseResponse<PictureVo> uploadPicture(
            @RequestPart("file") MultipartFile file,
            @RequestPart("params") PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(file==null,new BusinessException(ErrorCode.PARAMS_ERROR,"文件为空"));
        User loginUser = userService.getLoginUser(request);
        PictureVo pictureVo = pictureService.uploadPicture(file, pictureUploadRequest, loginUser);

        return ResultUtils.success(pictureVo);
    }

    @PostMapping("/upload/url")
    public BaseResponse<PictureVo> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        String fileUrl = pictureUploadRequest.getFileUrl();
        //校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(fileUrl),new BusinessException(ErrorCode.PARAMS_ERROR,"文件为空"));
        User loginUser = userService.getLoginUser(request);
        PictureVo pictureVo = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);

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
        User loginUser = userService.getLoginUser(request);
        Picture pictureToDel = pictureService.getById(id);
        pictureService.deletePictureById(pictureToDel, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片 (仅管理员使用)
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
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
        //补充审核参数
        pictureService.fillReviewParams(picture,userService.getLoginUser(request));
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
    public BaseResponse<PictureVo> getPictureVoById(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        Long id = pictureQueryRequest.getId();
        ThrowUtils.throwIf(ObjectUtil.isNull(id) || id<=0 ,new BusinessException(ErrorCode.PARAMS_ERROR,"传递参数错误>"));
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(ObjectUtil.isNull(picture),new BusinessException(ErrorCode.OPERATION_ERROR,"要查询的图片不存在"));
        Long spaceId = picture.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        if(spaceId!=null){
            //私有空间的照片只能空间创建人查看,系统管理员也不行!
            pictureService.checkPictureAuth(loginUser,picture);
        }
        PictureVo pictureVo = pictureService.getPictureVo(picture);
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

    /**
     * 分页查询 Picture 给用户用的 (只展示已过审的图片)
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("list/page/vo")
    public BaseResponse<Page<PictureVo>> listPagePictureVo(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser),ErrorCode.NO_AUTH_ERROR);
        //限制爬虫
        ThrowUtils.throwIf(pageSize>30,new BusinessException(ErrorCode.PARAMS_ERROR,"参数错误"));
        //普通用户默认只能查看已过审并且是公共图库的图片
        Long spaceId = pictureQueryRequest.getSpaceId();
        QueryWrapper<Picture> queryPictureWrapper;
        if(spaceId==null) {
            //查公共图库
            pictureQueryRequest.setReviewStatus(ReviewStatusEnum.PASS.getStatus());
            pictureQueryRequest.setNullSpaceId(true);
        } else{
            //todo
            //个人私有空间,spaceId不为null
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"空间不存在");
            //仅空间创建者可以查询自己的空间
            if(!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"当前用户无权限查看该空间");
            }
            //可以查看所有状态的图片(包括:过审,审核中,拒绝)
            pictureQueryRequest.setNullSpaceId(false);
        }
        queryPictureWrapper = pictureService.getQueryPictureWrapper(pictureQueryRequest);
        Page<Picture> picturePage = new Page<>(current, pageSize);
        Page<Picture> picturePageResult = pictureService.page(picturePage,queryPictureWrapper);

        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePageResult, request);
        return ResultUtils.success(pictureVoPage);
    }

    /**
     * 分页查询 Picture 给用户用的 使用了 caffeine和redis 做多级缓存
     * 在缓存的过程中,有json串和java对象之间的转换,这使得有一些为空的字段被过滤掉了
     * 为什么在查询时,先查caffeine,然后再查redis
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("list/page/vo/cache")
    public BaseResponse<Page<PictureVo>> listPagePictureVoWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureQueryRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空"));
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(pageSize>30,new BusinessException(ErrorCode.PARAMS_ERROR,"参数错误"));
        //普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(ReviewStatusEnum.PASS.getStatus());
        QueryWrapper<Picture> queryPictureWrapper = pictureService.getQueryPictureWrapper(pictureQueryRequest);
        //设计缓存的key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("%s_listPagePictureVo_%s", KeyConstant.REDIS_KEY, hashKey);
        String cachedValue = "";
        //先查本地缓存
        cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if(StringUtils.isNotEmpty(cachedValue)){
            Page<PictureVo> bean = JSONUtil.toBean(JSONUtil.parseObj(cachedValue), Page.class);
            return ResultUtils.success(bean);
        }
        //本地缓存未命中,查redis
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        //redis中的缓存结果
        cachedValue = opsForValue.get(cacheKey);
        //redis命中,返回结果并更新caffeine缓存
        if(StringUtils.isNotEmpty(cachedValue)){
            //更新caffeine缓存
            LOCAL_CACHE.put(cacheKey,cachedValue);
            Page<PictureVo> cachedPage = JSONUtil.toBean(JSONUtil.parseObj(cachedValue), Page.class);
            return ResultUtils.success(cachedPage);
        }
        //redis也没有,查数据库,然后添加到缓存中
        Page<Picture> picturePage = new Page<>(current, pageSize);
        Page<Picture> picturePageResult = pictureService.page(picturePage,queryPictureWrapper);
        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePageResult, request);
        // 5-10分钟过期,防止雪崩
        int expireTime = 300+RandomUtil.randomInt(0, 300);
        opsForValue.set(cacheKey,JSONUtil.toJsonStr(pictureVoPage),expireTime, TimeUnit.SECONDS);
        //不要忘了caffeine中也要设置
        LOCAL_CACHE.put(cacheKey,JSONUtil.toJsonStr(pictureVoPage));
        return ResultUtils.success(pictureVoPage);
    }

    /**
     * 编辑图片 (给用户使用)
     */
    @PostMapping("edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureEditRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser),ErrorCode.NO_AUTH_ERROR,"当前用户未登录");

        boolean result = pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(result);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意", "二次元");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报", "动漫");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 返回后端支持的图片格式
     * @return
     */
    @GetMapping("/picture_format")
    public BaseResponse<List<String>> listPictureFormat() {
        PictureFormatEnum[] values = PictureFormatEnum.values();
        List<String> formatList = Arrays.stream(values).map(PictureFormatEnum::getFormat).toList();
        return ResultUtils.success(formatList);
    }

    @PostMapping("/review")
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureReviewRequest),new BusinessException(ErrorCode.PARAMS_ERROR,"传递参数为空"));
        User loginUser = userService.getLoginUser(request);
        if(loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"当前用户未登录");
        }
        boolean result = pictureService.doPictureReview(pictureReviewRequest, loginUser);
        ThrowUtils.throwIf(!result,new BusinessException(ErrorCode.OPERATION_ERROR,"审核操作失败"));
        return ResultUtils.success(true);
    }
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload/batch")
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureUploadByBatchRequest),ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean isAdmin = userService.isAdmin(loginUser);
        ThrowUtils.throwIf(!isAdmin,ErrorCode.NO_AUTH_ERROR);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图
     * @param pictureSearchRequest
     * @param request
     * @return
     */
    @PostMapping("/search_picture/by/picture")
    public BaseResponse<List<ImageSearchResult>> searchSimilarPicture(@RequestBody PictureSearchRequest pictureSearchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureSearchRequest),ErrorCode.PARAMS_ERROR,"传递参数为空");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NO_AUTH_ERROR,"登录后使用该功能");
        Long pid = pictureSearchRequest.getId();
        Picture picture = pictureService.getById(pid);
        ThrowUtils.throwIf(picture==null,ErrorCode.PARAMS_ERROR,"指定的图片不存在");
        //attention注意:现在url的格式大部分是webp,做了图片压缩。
        String url = picture.getUrl();
        List<ImageSearchResult> imageSearchResults = ImageSearchApiFacade.searchImages(url);
        return ResultUtils.success(imageSearchResults);
    }

    /**
     * 根据颜色搜索图片
     * @param
     * @param request
     * @return
     */
    @PostMapping("/search_picture/by/color")
    public BaseResponse<List<PictureVo>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
                                                              HttpServletRequest request) {
        //参数校验
        ThrowUtils.throwIf(ObjectUtil.isNull(searchPictureByColorRequest),ErrorCode.PARAMS_ERROR);
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String picColor = searchPictureByColorRequest.getPicColor();
        ThrowUtils.throwIf(picColor==null,ErrorCode.PARAMS_ERROR,"<UNK>");
        ThrowUtils.throwIf(spaceId==null,ErrorCode.PARAMS_ERROR,"<UNK>");
        //调用service
        User loginUser = userService.getLoginUser(request);
        List<PictureVo> pictureVoList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVoList);
    }

    /**
     * 批量编辑图片
     * @param
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditRequestByBatch pictureEditRequestByBatch,
                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(pictureEditRequestByBatch),ErrorCode.PARAMS_ERROR,"参数不能为空");
        //参数校验
        Long spaceId = pictureEditRequestByBatch.getSpaceId();
        List<Long> pictureIdList = pictureEditRequestByBatch.getPictureIdList();
        ThrowUtils.throwIf(pictureIdList==null,ErrorCode.PARAMS_ERROR,"批量编辑的图片列表为空");
        ThrowUtils.throwIf(spaceId==null,ErrorCode.PARAMS_ERROR,"空间不存在");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser==null,ErrorCode.NO_AUTH_ERROR,"当前用户未登录");
        //调用service
        boolean result = pictureService.editPictureByBatch(pictureEditRequestByBatch, loginUser);
        return ResultUtils.success(result);
    }

    //todo Ai扩图接口


}
