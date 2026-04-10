package com.lc.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SearchPictureByColorRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -819188932471352794L;
    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 图片主色调
     */
    private String picColor;
}
