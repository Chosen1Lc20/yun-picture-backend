package com.lc.yunpicturebackend.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {
    @Serial
    private static final long serialVersionUID = 4069978334813343742L;

    private List<String> tagList;

    private List<String> categoryList;
}