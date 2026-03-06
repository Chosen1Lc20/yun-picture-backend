package com.lc.yunpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -5981032304470917726L;
    /**
     * 用户账号
     */
    private String userAccount;
    /**
     * 用户密码
     */
    private String userPassword;
}
