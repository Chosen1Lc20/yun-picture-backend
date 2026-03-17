package com.lc.yunpicturebackend.model.enums;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Getter;

@Getter
public enum PictureFormatEnum {
    //"jpg","png","webp","jpeg"
    JPG("jpg"),
    PNG("png"),
    WEBP("webp"),
    JPEG("jpeg");

    private final String format;

    PictureFormatEnum(String value) {
        this.format = value;
    }

    public static PictureFormatEnum getByValue(String value) {
        if(StringUtils.isBlank(value)){
            return null;
        }
        for(PictureFormatEnum e : PictureFormatEnum.values()) {
            if(e.format.equals(value)) {
                return e;
            }
        }
        return null;
    }
}
