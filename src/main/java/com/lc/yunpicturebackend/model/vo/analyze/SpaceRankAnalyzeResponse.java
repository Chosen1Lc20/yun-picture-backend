package com.lc.yunpicturebackend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceRankAnalyzeResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 5449943320318247733L;

    /**
     * 用户Id
     */
    private Long UserId;

    /**
     * 图片占据空间的总大小
     */
    private Long totalSize;
}
