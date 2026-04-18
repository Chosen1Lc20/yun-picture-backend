package com.lc.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 空间图片分类分析请求 (包括私人空间,公共空间)
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceCategoryAnalyzeRequest extends SpaceAnalyzeRequest{
    @Serial
    private static final long serialVersionUID = -2833282548188552656L;

}
