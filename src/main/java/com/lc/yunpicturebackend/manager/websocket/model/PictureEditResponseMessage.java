package com.lc.yunpicturebackend.manager.websocket.model;

import com.lc.yunpicturebackend.model.vo.UserVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

/**
 * 图片协同编辑响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {
    /**
     * 消息类型，例如 "INFO", "ERROR", "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 消息
     */
    private String message;

    /**
     * 用户信息
     */
    private UserVo userVo;

}
