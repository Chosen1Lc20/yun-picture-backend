package com.lc.yunpicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 添加空间成员请求
 */
@Data
public class SpaceUserAddRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -6936810444832900654L;
    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
}
