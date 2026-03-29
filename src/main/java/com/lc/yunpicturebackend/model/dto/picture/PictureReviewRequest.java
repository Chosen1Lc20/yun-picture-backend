package com.lc.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.util.Date;
@Data
public class PictureReviewRequest {
    /**
     * 图片id
     */
    private Long id;
    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

}
