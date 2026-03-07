package com.lc.yunpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserDeleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -1010365720216144494L;

    /**
     * 用户id
     */
    private long id;

    /**
     * 用户账号
     */
    private String userAccount;
}
