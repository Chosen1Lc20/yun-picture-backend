package com.lc.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 分析用户上传行为分析,支持只分析某个用户的上传行为
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    @Serial
    private static final long serialVersionUID = 1016262669791927094L;
    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 时间维度：day / week / month
     */
    private String timeDimension;
}
