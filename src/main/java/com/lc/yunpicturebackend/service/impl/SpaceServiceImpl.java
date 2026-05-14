package com.lc.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.lc.yunpicturebackend.model.dto.space.SpaceEditRequest;
import com.lc.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.SpaceUser;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.lc.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.lc.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.lc.yunpicturebackend.model.vo.SpaceVo;
import com.lc.yunpicturebackend.model.vo.UserVo;
import com.lc.yunpicturebackend.service.SpaceService;
import com.lc.yunpicturebackend.mapper.SpaceMapper;
import com.lc.yunpicturebackend.service.SpaceUserService;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* @author lianchao0921
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-04-03 10:29:53
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    private ConcurrentHashMap<Long, Object> lockMap;

    @Lazy
    @Resource
    private SpaceUserService spaceUserService;

//    @Lazy
//    @Resource
//    private DynamicShardingManager dynamicShardingManager;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //根据用户id来判断创建空间个数的合法性
        String spaceName = spaceAddRequest.getSpaceName();
        if(StrUtil.isBlank(spaceName)){
            spaceName = "默认空间";
        }
        Integer spaceLeve = spaceAddRequest.getSpaceLevel();
        if(spaceLeve == null){
            spaceLeve = SpaceLevelEnum.COMMON.getValue();
        }
        Integer spaceType = spaceAddRequest.getSpaceType();
        if(spaceType == null){
            //默认要创建私人空间
            spaceType = SpaceTypeEnum.PRIVATE.getValue();
        }
        Space space = new Space();
        space.setSpaceName(spaceName);
        space.setSpaceLevel(spaceLeve);
        space.setUserId(loginUser.getId());
        space.setSpaceType(spaceType);
        this.fillSpaceBySpaceLevel(space);
        //校验空间是否有效
        this.validSpace(space,true);
        //权限校验 普通用户只能创建普通空间,管理员可以创建任意级别空间
        Long uid = loginUser.getId();
        if(!userService.isAdmin(loginUser) && SpaceLevelEnum.COMMON.getValue() != spaceLeve){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"当前用户无权限创建更高级别空间");
        }
        lockMap = new ConcurrentHashMap<>();
        //针对用户进行加锁
        Object lock = lockMap.computeIfAbsent(uid, (key) -> new Object());
        synchronized(lock){
            transactionTemplate.execute((status)->{
                //每个用户只能创建一个私有空间和一个公共空间。管理员可以创建多个公共空间
                //attention 这里管理员可以创建多个公共空间,应该只能创建一个私有空间。但这里的逻辑并没有检测只能创建一个私有空间
                try{
                    if(!userService.isAdmin(loginUser)){
                        boolean exists = this.lambdaQuery().
                                eq(Space::getUserId, uid).
                                eq(Space::getSpaceType, spaceAddRequest.getSpaceType()).
                                exists();
                        //存在空间
                        ThrowUtils.throwIf(exists,ErrorCode.OPERATION_ERROR,"要创建的空间已存在");
                    }
                    boolean save = this.save(space);
                    ThrowUtils.throwIf(!save,ErrorCode.OPERATION_ERROR,"创建空间失败");
                    //不存在,如果是团队空间,则自动将创建者设置为管理员
                    if(spaceAddRequest.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())){
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setUserId(uid);
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        boolean SpaceUserRes = spaceUserService.save(spaceUser);
                        ThrowUtils.throwIf(!SpaceUserRes,ErrorCode.OPERATION_ERROR,"创建团队成员失败");
                    }
                    return space.getId();
                } finally {
                    //防止内存泄露
                    lockMap.remove(uid);
                }
            });
        }
        return Optional.ofNullable(space.getId()).orElse(-1L);
    }


    /**
     * 根据 spaceQueryRequest 获得space的queryWrapper
     * @param spaceQueryRequest 空间查询请求
     * @return queryWrapper
     */
    @Override
    public QueryWrapper<Space> getQuerySpaceWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        if(spaceQueryRequest==null){
            return spaceQueryWrapper;
        }
        //取属性
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLeve = spaceQueryRequest.getSpaceLevel();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        //默认降序
        String sortOrder = spaceQueryRequest.getSortOrder();

        spaceQueryWrapper.eq(id !=null,"id", id);
        spaceQueryWrapper.like(StringUtils.isNotBlank(spaceName),"spaceName", spaceName);
        spaceQueryWrapper.eq(ObjectUtil.isNotNull(spaceLeve),"spaceLevel", spaceLeve);
        spaceQueryWrapper.eq(ObjectUtil.isNotNull(userId),"userId", userId);
        spaceQueryWrapper.eq(ObjectUtil.isNotNull(spaceType),"spaceType", spaceType);
        //排序
        spaceQueryWrapper.orderBy(StringUtils.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        return spaceQueryWrapper;
    }

    /**
     *  //todo 写注释
     * @param space
     * @return
     */
    @Override
    public SpaceVo getSpaceVo(Space space) {

        SpaceVo spaceVo = SpaceVo.objToVo(space);
        //创建空间的用户id
        Long userId = spaceVo.getUserId();
        ThrowUtils.throwIf(ObjectUtil.isNull(userId),new BusinessException(ErrorCode.SYSTEM_ERROR,"创建空间用户id不存在"));
        //创建空间的用户
        User user = userService.getById(userId);
        UserVo userVo = userService.getUserVo(user);
        spaceVo.setUserVo(userVo);

        return spaceVo;
    }

    @Override
    public Page<SpaceVo> getSpaceVoPage(Page<Space> page ) {
        List<Space> spaceList = page.getRecords();
        Page<SpaceVo> spaceVoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if(CollUtil.isEmpty(spaceList)){
            return spaceVoPage;
        }
        List<SpaceVo> spaceVoList =
                page.getRecords().stream().map(SpaceVo::objToVo).collect(Collectors.toList());
        //先查询空间列表里所有的用户id,要不重复,所以选择set
        Set<Long> UidSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        //将用户id和用户关联起来,这样只需查询一次数据库
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(UidSet).stream().collect(Collectors.groupingBy((User::getId)));
        //spaceVo 有userVo属性需要关联
        for(SpaceVo spaceVo:spaceVoList){
            if(userIdUserListMap.containsKey(spaceVo.getUserId())){
                //get(0)是 取出用户
                User user = userIdUserListMap.get(spaceVo.getUserId()).get(0);
                UserVo userVo = userService.getUserVo(user);
                spaceVo.setUserVo(userVo);
            }
        }

        spaceVoPage.setRecords(spaceVoList);

        return spaceVoPage;
    }
    //什么样的空间算是有效??
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        //创建空间时校验空间
        if(add){
            ThrowUtils.throwIf(ObjectUtil.isNull(spaceName),ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(ObjectUtil.isNull(spaceLevel),ErrorCode.PARAMS_ERROR);
        }
        //更新空间时校验
        if(ObjectUtil.isNull(spaceName) && spaceName.length()>30){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空或过长");
        }
        if(spaceLevel!=null){
            SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
            if(ObjectUtil.isNull(spaceLevelEnum)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别传递错误");
            }
        }
        if(spaceType!=null){
            SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
            if(ObjectUtil.isNull(spaceTypeEnum)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间类别传递错误");
            }
        }
    }

    /**
     * 校验要编辑的图片是否有效
     * @param spaceEditRequest
     */
    @Override
    public void validSpaceToEdit(SpaceEditRequest spaceEditRequest) {
        ThrowUtils.throwIf(spaceEditRequest == null, ErrorCode.PARAMS_ERROR);
        String spaceName = spaceEditRequest.getSpaceName();
        //编辑空间时校验
        if(ObjectUtil.isNull(spaceName) && spaceName.length()>30){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空或过长");
        }
    }

    /**
     * 在创建空间或更新空间时,自动填充参数
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space){
        ThrowUtils.throwIf(ObjectUtil.isNull(space),ErrorCode.PARAMS_ERROR,"请求参数不能为空");
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        ThrowUtils.throwIf(ObjectUtil.isNull(spaceLevelEnum),ErrorCode.PARAMS_ERROR,"空间级别的值错误,不在正确范围");
        //如果管理员指定了空间的属性值,那么就用管理员的
        if(space.getMaxCount()==null){
            space.setMaxCount(spaceLevelEnum.getMaxCount());
        }
        if(space.getMaxSize()==null){
            space.setMaxSize(spaceLevelEnum.getMaxSize());
        }
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        ThrowUtils.throwIf(ObjectUtil.isNull(loginUser),ErrorCode.PARAMS_ERROR,"当前用户未登录");
        //仅本人或管理员可以访问
        if( !space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser) ){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"当前用户没有该空间的权限");
        }
    }

}




