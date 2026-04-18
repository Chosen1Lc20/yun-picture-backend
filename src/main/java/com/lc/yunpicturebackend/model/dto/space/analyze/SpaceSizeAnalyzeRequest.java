package com.lc.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * <1kb,  >1kb && <1mb , >1mb
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceSizeAnalyzeRequest extends SpaceAnalyzeRequest{
    @Serial
    private static final long serialVersionUID = -7975702106192753063L;
}
