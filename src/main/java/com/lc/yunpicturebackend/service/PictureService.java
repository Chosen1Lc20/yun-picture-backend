package com.lc.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lc.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.lc.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.PictureVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
* @author lianchao0921
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-03-14 21:17:46
*/
public interface PictureService extends IService<Picture> {
    PictureVo uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    PictureVo getPictureVo(Picture picture, HttpServletRequest request);

    Page<PictureVo> getPictureVoPage(Page<Picture> page, HttpServletRequest request);

    void validPicture(Picture picture);

    QueryWrapper<Picture> getQueryPictureWrapper(PictureQueryRequest pictureQueryRequest);

}
