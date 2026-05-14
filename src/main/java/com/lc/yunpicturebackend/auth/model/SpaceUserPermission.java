package com.lc.yunpicturebackend.auth.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SpaceUserPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = -2627229447862348000L;

    /**
     * 权限的key
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;
}
