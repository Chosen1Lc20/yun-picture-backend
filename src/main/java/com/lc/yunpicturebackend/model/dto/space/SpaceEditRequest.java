package com.lc.yunpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 目前仅允许编辑图片名称
 */
@Data
public class SpaceEditRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 8040147467827539477L;
    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

}
