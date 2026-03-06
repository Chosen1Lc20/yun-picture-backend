package com.lc.yunpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    USER("用户","user"),
    ADMIN("管理员","admin");

    private final String text ;
    private final String value ;

    UserRoleEnum(String text, String value) {
        this.text = text ;
        this.value = value;
    }

    /**
     * 根据 value获取枚举值
     * @param value user 或 admin
     * @return 枚举对象
     */
    public static UserRoleEnum getUserRoleEnumByValue(String value) {
        if(ObjUtil.isEmpty(value)){
            return null;
        }
        for(UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if(userRoleEnum.getValue().equals(value)){
                return userRoleEnum;
            }
        }
        return null;
    }
}
