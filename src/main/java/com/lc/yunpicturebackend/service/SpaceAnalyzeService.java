package com.lc.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.lc.yunpicturebackend.model.dto.space.analyze.*;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService {
    QueryWrapper<Picture> fillSpaceAnalyzeQueryWrapper(QueryWrapper<Picture> queryWrapper, SpaceAnalyzeRequest spaceAnalyzeRequest);

    void checkSpaceAnalyzeAuth(User loginUser, SpaceAnalyzeRequest spaceAnalyzeRequest);

    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
