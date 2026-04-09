package com.lc.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 以图搜图请求
 */
@Data
public class PictureSearchRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -171636189256151754L;
    /**
     * 要搜索的图片id
     */
    private Long id;
}
