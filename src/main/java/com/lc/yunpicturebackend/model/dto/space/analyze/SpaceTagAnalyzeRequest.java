package com.lc.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceTagAnalyzeRequest extends SpaceAnalyzeRequest{
    @Serial
    private static final long serialVersionUID = -8172061128930275343L;
}
