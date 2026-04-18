package com.lc.yunpicturebackend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 空间图片标签分类分析响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 2491603350137841334L;
    /**
     * 图片标签
     */
    private String tags;

    /**
     * 使用次数
     */
    private Long count;

}
