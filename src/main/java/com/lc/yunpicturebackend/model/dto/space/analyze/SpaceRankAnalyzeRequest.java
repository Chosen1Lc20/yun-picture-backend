package com.lc.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 空间使用排行分析
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceRankAnalyzeRequest extends SpaceAnalyzeRequest{
    @Serial
    private static final long serialVersionUID = 3323909802870918027L;

    /**
     * 排名前topN的空间
     */
    private Integer topN = 10;
}
