package com.lc.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lc.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.lc.yunpicturebackend.model.dto.space.SpaceEditRequest;
import com.lc.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.lc.yunpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.SpaceVo;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author lianchao0921
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-04-03 10:29:53
*/
public interface SpaceService extends IService<Space> {

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    SpaceVo getSpaceVo(Space space);

    Page<SpaceVo> getSpaceVoPage(Page<Space> page);

    void validSpace(Space space, boolean add);

    void validSpaceToEdit(SpaceEditRequest spaceEditRequest);

    QueryWrapper<Space> getQuerySpaceWrapper(SpaceQueryRequest spaceQueryRequest);

    void fillSpaceBySpaceLevel(Space space);

    void checkSpaceAuth(Space space, User loginUser);
}
