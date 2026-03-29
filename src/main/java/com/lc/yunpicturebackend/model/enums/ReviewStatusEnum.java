package com.lc.yunpicturebackend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

@Getter
public enum ReviewStatusEnum {

    REVIEWING("待审核",0),
    PASS("审核通过",1),
    REJECT("审核拒绝",2);

    private String message;
    private Integer status;

    ReviewStatusEnum(String message, Integer status) {
        this.message = message;
        this.status = status;
    }

    public ReviewStatusEnum getReviewStatusEnumByValue(Integer value) {
        if(ObjectUtil.isEmpty(value)){
            return null;
        }
        ReviewStatusEnum[] enums = ReviewStatusEnum.values();
        for (ReviewStatusEnum e : enums) {
            if(e.status.equals(value)) {
                return e;
            }
        }
        return null;
    }
}
