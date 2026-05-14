package com.lc.yunpicturebackend.auth.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserAuthConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = -3197991411228916303L;

    private List<SpaceUserPermission> permissions;

    private List<SpaceUserRole> roles;
}
