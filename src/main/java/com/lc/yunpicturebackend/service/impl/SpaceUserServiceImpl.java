package com.lc.yunpicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.mapper.SpaceUserMapper;
import com.lc.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.lc.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.SpaceUser;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.lc.yunpicturebackend.model.vo.SpaceUserVo;
import com.lc.yunpicturebackend.model.vo.SpaceVo;
import com.lc.yunpicturebackend.model.vo.UserVo;
import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.service.SpaceService;
import com.lc.yunpicturebackend.service.SpaceUserService;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author lianchao0921
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2026-04-22 10:19:15
 */
@Slf4j
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private SpaceUserMapper spaceUserMapper;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);
        // 数据库操作
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }


    @Override
    public QueryWrapper<SpaceUser> getSpaceUserQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        //校验空间角色,viewer,editor,admin。防止乱传
        List<String> spaceRoleList = SpaceRoleEnum.getAllValues();
        if(ObjUtil.isNotNull(spaceRole)){
            ThrowUtils.throwIf(!spaceRoleList.contains(spaceRole), ErrorCode.PARAMS_ERROR);
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotNull(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }


    /**
     * 用来在创建或者更新空间用户时进行校验
     *
     * @param spaceUser
     * @param add
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //校验是否已经添加该成员
            boolean exists = this.exists(new QueryWrapper<SpaceUser>().eq("spaceId", spaceId).eq("userId", userId));
            ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "已经添加该成员");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public SpaceUserVo getSpaceUserVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVo spaceUserVo = new SpaceUserVo();
        BeanUtils.copyProperties(spaceUser, spaceUserVo);
        //补充userVo和spaceVo
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            SpaceVo spaceVo = spaceService.getSpaceVo(spaceService.getById(spaceId));
            spaceUserVo.setSpaceVo(spaceVo);
        }
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            UserVo userVo = userService.getUserVo(userService.getById(userId));
            spaceUserVo.setUserVo(userVo);
        }
        return spaceUserVo;
    }

    public List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUserList) {
        if (spaceUserList == null || spaceUserList.isEmpty()) {
            return new ArrayList<>();
        }
        List<SpaceUserVo> spaceUserVoList = spaceUserList.stream().map(SpaceUserVo::objToVo).toList();
        //收集uid和spaceid列表
        Set<Long> uidSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceidSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        //id与user的对应
        Map<Long, List<User>> uidToUserMap = userService.listByIds(uidSet).stream().collect(Collectors.groupingBy(User::getId));
        //spaceId与space的对应
        Map<Long, List<Space>> spaceIdToSpaceMap = spaceService.listByIds(spaceidSet).stream().collect(Collectors.groupingBy(Space::getId));

        for (SpaceUserVo spaceUserVo : spaceUserVoList) {
            Long userId = spaceUserVo.getUserId();
            //填充userVo
            User user = null;
            if (userId != null && userId > 0) {
                user = uidToUserMap.get(userId).get(0);
            }
            spaceUserVo.setUserVo(userService.getUserVo(user));
            Long spaceId = spaceUserVo.getSpaceId();
            //填充spaceVo
            Space space = null;
            if (spaceId != null && spaceId > 0) {
                space = spaceIdToSpaceMap.get(spaceId).get(0);
            }
            spaceUserVo.setSpaceVo(spaceService.getSpaceVo(space));
        }
        log.info("aaaa:{}", spaceUserVoList);
        return spaceUserVoList;
    }

}




