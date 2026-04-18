package com.lc.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
/**
 * 空间资源使用分析请求类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUsageAnalyzeRequest extends SpaceAnalyzeRequest {

    @Serial
    private static final long serialVersionUID = -5836028702203270605L;
}
