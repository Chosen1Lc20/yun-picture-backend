package com.lc.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2480085631516702856L;

    /**
     * 图片 id (用于修改)
     */
    private Long id;

    /**
     * 图片url地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间id
     */
    private Long spaceId;
}
