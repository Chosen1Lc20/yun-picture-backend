package com.lc.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lc.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.lc.yunpicturebackend.model.dto.picture.*;
import com.lc.yunpicturebackend.model.dto.picture.batch.PictureEditRequestByBatch;
import com.lc.yunpicturebackend.model.dto.picture.batch.PictureUploadByBatchRequest;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.vo.PictureVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author lianchao0921
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-03-14 21:17:46
*/
public interface PictureService extends IService<Picture> {
    PictureVo uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    PictureVo getPictureVo(Picture picture );

    Page<PictureVo> getPictureVoPage(Page<Picture> page, HttpServletRequest request);

    void validPicture(Picture picture);

    QueryWrapper<Picture> getQueryPictureWrapper(PictureQueryRequest pictureQueryRequest);

    boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture,User loginUser);

    /**
     * 批量上传图片
     * @param pictureUploadByBatchRequest
     * @return 上传成功的页数
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    void clearPictureFile(Picture oldPicture);

    void checkPictureAuth(User loginUser, Picture picture);

    boolean deletePictureById(Picture picture, User loginUser);

    boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    List<PictureVo> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    CreateOutPaintingTaskResponse createPictureOutpaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
    /**
     * 批量编辑图片
     * @param pictureEditRequestByBatch
     * @param loginUser
     * @return
     */
    boolean editPictureByBatch(PictureEditRequestByBatch pictureEditRequestByBatch, User loginUser);
}
