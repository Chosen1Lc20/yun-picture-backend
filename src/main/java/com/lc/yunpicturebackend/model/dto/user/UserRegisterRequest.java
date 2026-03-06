package com.lc.yunpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -7368683870266451097L;
    /**
     * 用户账号
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;
    /**
     * 确认密码
     */
    private String checkPassword;
}
