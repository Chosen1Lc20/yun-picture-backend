package com.lc.yunpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.dto.space.analyze.*;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.analyze.*;

import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.service.SpaceAnalyzeService;
import com.lc.yunpicturebackend.service.SpaceService;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUsageAnalyzeRequest), ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(ObjUtil.isNull(loginUser), ErrorCode.NOT_LOGIN_ERROR, "当前用户未登录");
        Long spaceId = null;
        if(StringUtils.isNotBlank(spaceUsageAnalyzeRequest.getSpaceId())){
             spaceId = Long.valueOf(spaceUsageAnalyzeRequest.getSpaceId());
        }
        boolean isQueryCommonSpace = spaceUsageAnalyzeRequest.isQueryCommonSpace();
        boolean isQueryAllSpace = spaceUsageAnalyzeRequest.isQueryAllSpace();
        //查询公共图库或者全部空间
        checkSpaceAnalyzeAuth(loginUser, spaceUsageAnalyzeRequest);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //attention 只查询指定字段,减少查询消耗
        queryWrapper.select("picSize");
        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
        //attention 根据查询的空间类别来填充分析条件
        fillSpaceAnalyzeQueryWrapper(queryWrapper, spaceUsageAnalyzeRequest);
        //查询公共空间或者全部空间
        if (isQueryCommonSpace || isQueryAllSpace) {
            checkSpaceAnalyzeAuth(loginUser, spaceUsageAnalyzeRequest);
            //单一的字段值集合
            List<Object> pictureObjectList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            //图片大小集合
            Long usedSize = pictureObjectList.stream().mapToLong((picSize) -> (Long) picSize).sum();
            Long usedCount = (long) pictureObjectList.size();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            //查询私有空间
            ThrowUtils.throwIf(ObjUtil.isNull(spaceId) || spaceId <= 0, ErrorCode.PARAMS_ERROR, "参数为空");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.PARAMS_ERROR, "空间不存在");
            checkSpaceAnalyzeAuth(loginUser, spaceUsageAnalyzeRequest);

            long usedSize = space.getTotalSize();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);

            long usedCount = space.getTotalCount();
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);

            Long maxCount = space.getMaxCount();
            spaceUsageAnalyzeResponse.setMaxCount(maxCount);

            Long maxSize = space.getMaxSize();
            spaceUsageAnalyzeResponse.setMaxSize(maxSize);
            //后端直接算好百分比,返回给前端
            double sizeUsageRatio = NumberUtil.round(usedSize * 100.0 / maxSize, 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            double countUsageRatio = NumberUtil.round(usedCount * 100.0 / maxCount, 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 分析空间内的图片分类
     *
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceCategoryAnalyzeRequest), ErrorCode.PARAMS_ERROR, "参数为空");
        //校验权限
        checkSpaceAnalyzeAuth(loginUser, spaceCategoryAnalyzeRequest);
        //根据查询的空间类别来填充分析条件
        QueryWrapper<Picture> pictureQueryWrapper = fillSpaceAnalyzeQueryWrapper(new QueryWrapper<Picture>(), spaceCategoryAnalyzeRequest);
        //使用My-batis分组查询
        pictureQueryWrapper.select(" category AS category ",
                        "COUNT(*) AS count",
                        "SUM(picSize) AS totalSize").
                groupBy("category");
        List<Map<String, Object>> pictureList = pictureService.getBaseMapper().selectMaps(pictureQueryWrapper);
        log.info("pictureList:{}", pictureList);
        return pictureList.stream().map((map) -> {
            String category = map.get("category") != null
                    ? map.get("category").toString() : "未分类";
            //attention 这里之前发生了类型转换异常Long count =  (Long) map.get("count"));
            Long count = ((Number) map.get("count")).longValue();
            Long totalSize = ((Number)map.get("totalSize")).longValue();
            return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
        }).toList();
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceTagAnalyzeRequest), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(ObjUtil.isNull(loginUser), ErrorCode.NOT_LOGIN_ERROR, "当前用户未登录");
        //校验权限
        checkSpaceAnalyzeAuth(loginUser, spaceTagAnalyzeRequest);
        //根据查询的空间类别来填充分析条件
        QueryWrapper<Picture> pictureQueryWrapper = fillSpaceAnalyzeQueryWrapper(new QueryWrapper<>(), spaceTagAnalyzeRequest);
        pictureQueryWrapper.select("tags");
        //获取符合条件的tagsList,过滤没有标签的图片
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper).
                stream().
                filter(ObjUtil::isNotNull).
                //去掉""
                map(Object::toString).
                collect(Collectors.toList());
        log.info("tagsJsonList:{}",tagsJsonList);
        // tagsJsonList:[{tags=["热门","高清"]}, {tags=["热门","高清","帅"]}] 这是之前写错的
        // [["热门","高清"], ["热门","高清","帅"], 这是对的
        Map<String, Long> tagCountMap = tagsJsonList.stream().
                flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream()).
                //                                 按什么分组          分组以后要干什么
                        collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        //转化为响应对象
        return tagCountMap.entrySet().stream().
                sorted((e1, e2) -> {
                    //默认是降序排序
                    return Long.compare(e2.getValue(), e1.getValue());
                })
                .map(entry -> {
                    String tag = entry.getKey();
                    Long count = entry.getValue();
                    return new SpaceTagAnalyzeResponse(tag, count);
                }).toList();
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(ObjUtil.isNull(spaceSizeAnalyzeRequest), ErrorCode.PARAMS_ERROR, "参数为空");
        //校验权限
        checkSpaceAnalyzeAuth(loginUser, spaceSizeAnalyzeRequest);
        QueryWrapper<Picture> pictureQueryWrapper = fillSpaceAnalyzeQueryWrapper(new QueryWrapper<>(), spaceSizeAnalyzeRequest);
        pictureQueryWrapper.select("picSize");

        List<Object> objectList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper);
        List<Long> sizeList = objectList.stream().map((object) -> {
            return (Long) object;
        }).toList();
        // <500kb,  >500kb && <1mb , >1mb <>2MB
        LinkedHashMap<String, Long> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("<500KB", sizeList.stream().filter((size) -> size < 500 * 1024).count());
        linkedHashMap.put("500KB-1MB", sizeList.stream().filter((size) -> size >= 500 * 1024 && size < 1024 * 1024).count());
        linkedHashMap.put("1MB-2MB", sizeList.stream().filter((size) -> size >= 1024 * 1024 && size < 2048 * 1024).count());
        linkedHashMap.put(">=2MB", sizeList.stream().filter((size) -> size >= 2048 * 1024).count());

        //图片大小范围
        //图片大小范围内的数量
        return linkedHashMap.entrySet().stream().map((entry) -> {
            //图片大小范围
            String sizeRange = entry.getKey();
            //图片大小范围内的数量
            Long count = entry.getValue();
            return new SpaceSizeAnalyzeResponse(sizeRange, count);
        }).toList();
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(loginUser, spaceUserAnalyzeRequest);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = null;
        if(StringUtils.isNotBlank(spaceUserAnalyzeRequest.getUserId())){
            userId = Long.valueOf(spaceUserAnalyzeRequest.getUserId());
        }
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillSpaceAnalyzeQueryWrapper(queryWrapper, spaceUserAnalyzeRequest);

        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }

        // 分组和排序 (升序)
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询结果并转换
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可以操作");
        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        spaceQueryWrapper.select("id","spaceName","userId","totalSize").
                orderByDesc("totalSize").
                last(" limit " + spaceRankAnalyzeRequest.getTopN());
        return spaceService.list(spaceQueryWrapper);
    }


    /**
     * 根据查询请求区分查询的空间
     *
     * @param queryWrapper
     * @param spaceAnalyzeRequest
     * @return QueryWrapper<Picture>
     */
    @Override
    public QueryWrapper<Picture> fillSpaceAnalyzeQueryWrapper(QueryWrapper<Picture> queryWrapper, SpaceAnalyzeRequest spaceAnalyzeRequest) {
        Long spaceId = null;
        //前端没有传递spaceId,也就是传了''
        if(StringUtils.isNotBlank(spaceAnalyzeRequest.getSpaceId())){
            spaceId = Long.valueOf(spaceAnalyzeRequest.getSpaceId());
        }
        boolean isQueryCommonSpace = spaceAnalyzeRequest.isQueryCommonSpace();
        boolean isQueryAllSpace = spaceAnalyzeRequest.isQueryAllSpace();
        //查询的不是私人空间
        if (isQueryAllSpace) {
            return queryWrapper;
        }
        if (isQueryCommonSpace) {
            queryWrapper.isNull("spaceId");
            return queryWrapper;
        }
        //查询的是私人空间
        if (ObjUtil.isNotNull(spaceId)) {
            queryWrapper.eq("spaceId", spaceId);
            return queryWrapper;
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR);
    }

    /**
     * 校验空间分析的权限
     *
     * @param loginUser           当前登录用户
     * @param spaceAnalyzeRequest 空间分析请求
     */
    @Override
    public void checkSpaceAnalyzeAuth(User loginUser, SpaceAnalyzeRequest spaceAnalyzeRequest) {
        //参数校验
        ThrowUtils.throwIf(ObjUtil.isNull(spaceAnalyzeRequest), ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(ObjUtil.isNull(loginUser), ErrorCode.NOT_LOGIN_ERROR, "当前用户未登录");
        //取参数
        Long spaceId = null;
        //前端没有传递spaceId,也就是传了''
        if(StringUtils.isNotBlank(spaceAnalyzeRequest.getSpaceId())){
            spaceId = Long.valueOf(spaceAnalyzeRequest.getSpaceId());
        }
        boolean isQueryCommonSpace = spaceAnalyzeRequest.isQueryCommonSpace();
        boolean isQueryAllSpace = spaceAnalyzeRequest.isQueryAllSpace();
        //仅管理员可以查询公共图库和全部空间
        if (isQueryCommonSpace || isQueryAllSpace) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可以操作");
        } else {
            //查询的是私人空间
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "spaceId不能为空");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isNull(space), ErrorCode.PARAMS_ERROR, "空间不存在,传递的id错误");
            //空间存在
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前用户没有权限操作");
            }
        }
    }

}
