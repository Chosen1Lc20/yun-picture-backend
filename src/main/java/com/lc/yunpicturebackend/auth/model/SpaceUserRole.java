package com.lc.yunpicturebackend.auth.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserRole implements Serializable {

    @Serial
    private static final long serialVersionUID = -654034599087467318L;

    /**
     * 角色的key
     */
    private String key;

    /**
     * 角色的名字
     */
    private String name;

    /**
     * 角色的权限
     */
    private List<String> permissions;

    /**
     * 角色的描述
     */
    private List<String> description;
}
