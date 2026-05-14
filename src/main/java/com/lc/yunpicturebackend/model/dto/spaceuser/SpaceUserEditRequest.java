package com.lc.yunpicturebackend.model.dto.spaceuser;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
/**
 * 根据 spaceUser表的id设置用户的角色
 */
public class SpaceUserEditRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = -105926434782011620L;

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;
}
