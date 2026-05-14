package com.lc.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lc.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.lc.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.lc.yunpicturebackend.model.entity.SpaceUser;
import com.lc.yunpicturebackend.model.vo.SpaceUserVo;

import java.util.List;


/**
 * @author lianchao0921
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2026-04-22 10:19:15
 */
public interface SpaceUserService extends IService<SpaceUser> {
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    QueryWrapper<SpaceUser> getSpaceUserQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    SpaceUserVo getSpaceUserVo(SpaceUser spaceUser);

    List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUserList);
}
