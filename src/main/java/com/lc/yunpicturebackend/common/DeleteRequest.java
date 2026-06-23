package com.lc.yunpicturebackend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    /**
     * id (spaceUserId)
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
