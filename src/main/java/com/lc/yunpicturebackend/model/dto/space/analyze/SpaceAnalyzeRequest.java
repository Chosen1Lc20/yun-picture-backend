package com.lc.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SpaceAnalyzeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5961134021696384560L;
    /**
     * 空间Id
     */
    private String spaceId;

    /**
     * 查询公共图库
     */
    private boolean queryCommonSpace;

    /**
     * 查询全部空间(包括公共图库)
     */
    private boolean queryAllSpace;
}
