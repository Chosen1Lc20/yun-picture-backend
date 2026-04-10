package com.lc.yunpicturebackend.model.dto.picture.batch;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditRequestByBatch implements Serializable {
    @Serial
    private static final long serialVersionUID = -2266258041649137564L;
    /**
     * 空间Id
     */
    private Long spaceId;
    /**
     * 图片Id列表
     */
    private List<Long> pictureIdList;
    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     *  命名规则 (约定类似 xxx_{序号})
     */
    private String nameRule;
}
