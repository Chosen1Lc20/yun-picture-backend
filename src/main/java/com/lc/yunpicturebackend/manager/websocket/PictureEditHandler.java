package com.lc.yunpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.lc.yunpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.lc.yunpicturebackend.manager.websocket.model.enums.PictureEditActionEnum;
import com.lc.yunpicturebackend.manager.websocket.model.enums.PictureEditMessageTypeEnum;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lc.yunpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在连接成功,处理消息,连接关闭时进行相应的处理
 * TextWebSocketHandler可以以字符串方式发送和接收消息
 */

//todo 写注释
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    // key: pictureId---> value: 当前正在编辑的用户id
    private ConcurrentHashMap<Long, Long> pictureEditingUser = new ConcurrentHashMap<>();

    // key: pictureId---> value: websocket会话集合
    private ConcurrentHashMap<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User loginUser = (User) session.getAttributes().get("loginUser");
        Long pictureId = Long.valueOf((String) session.getAttributes().get("pictureId"));
        //连接建立成功,把当前会话添加到map中
        pictureSessions.computeIfAbsent(pictureId, k -> ConcurrentHashMap.newKeySet()).add(session);
        //给其他人广播,发送通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setEditAction(null);
        pictureEditResponseMessage.setMessage(String.format("%s加入会话", loginUser.getUserName()));
        pictureEditResponseMessage.setUserVo(userService.getUserVo(loginUser));
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    /**
     * 广播给参与该图片的所有协作者 (可指定不发送的目标)
     *
     * @param pictureId                  图片id
     * @param pictureEditResponseMessage 图片协同编辑响应信息
     * @param excludedSession            排除的会话
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludedSession) throws IOException {
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        //集合中存在会话
        if (!webSocketSessions.isEmpty()) {
            //配置序列化器,解决Long类型精度丢失问题
            ObjectMapper objectMapper = new ObjectMapper();
            //注册自定义序列化/反序列化逻辑。
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            String jsonMessage = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage message = new TextMessage(jsonMessage);
            for (WebSocketSession session : webSocketSessions) {
                if (excludedSession != null && excludedSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        }
    }

    /**
     * 广播给参与该图片的所有协作者
     *
     * @param pictureId                  图片id
     * @param pictureEditResponseMessage 图片协同编辑响应信息
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    /**
     * 处理图片协同编辑请求
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User loginUser = (User) session.getAttributes().get("loginUser");
        Long userId = (Long) session.getAttributes().get("userId");
        Long pictureId = Long.valueOf((String) session.getAttributes().get("pictureId"));
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum editMessageType = PictureEditMessageTypeEnum.getEnumByValue(type);
        //生产消息
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, loginUser, pictureId);
    }

    /**
     * 处理进入会话的消息
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param pictureId
     * @param loginUser
     * @throws IOException
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, Long pictureId, User loginUser) throws IOException {
        ThrowUtils.throwIf(pictureId <= 0 || ObjUtil.isNull(pictureId), ErrorCode.PARAMS_ERROR);
        //当前图片没有人在编辑,才能加入编辑
        if (!pictureEditingUser.containsKey(pictureId)) {
            pictureEditingUser.put(pictureId, loginUser.getId());
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s开始编辑图片", loginUser.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUserVo(userService.getUserVo(loginUser));
            pictureEditResponseMessage.setEditAction(null);
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 用户执行编辑操作时,将消息同步给其它的用户
     *
     * @param pictureId
     * @param loginUser
     * @param
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, Long pictureId, User loginUser) throws IOException {
        ThrowUtils.throwIf(pictureId <= 0 || ObjUtil.isNull(pictureId), ErrorCode.PARAMS_ERROR);
        //编辑图片的用户id
        Long editingUid = pictureEditingUser.get(pictureId);
        //获取编辑动作
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum editActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (editActionEnum == null) {
            return;
        }
        //当前用户是正在编辑的用户
        if (editingUid != null && editingUid.equals(loginUser.getId())) {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", loginUser.getUserName(), editActionEnum.getText());
            pictureEditResponseMessage.setEditAction(editActionEnum.getValue());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUserVo(userService.getUserVo(loginUser));
            //广播给当前客户端以外的其它用户,否则会造成重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理退出编辑图片的消息
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param pictureId
     * @param loginUser
     * @throws IOException
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, Long pictureId, User loginUser) throws IOException {
        ThrowUtils.throwIf(pictureId <= 0 || ObjUtil.isNull(pictureId), ErrorCode.PARAMS_ERROR);
        Long editingUid = pictureEditingUser.get(pictureId);
        //当前编辑者是本人才能退出
        if (editingUid != null && editingUid.equals(loginUser.getId())) {
            //移除掉当前编辑的用户
            pictureEditingUser.remove(pictureId);
            //构造响应,发送退出编辑消息的通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("%s退出编辑图片", loginUser.getUserName());
            pictureEditResponseMessage.setEditAction(null);
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUserVo(userService.getUserVo(loginUser));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }

    /**
     * websocket 连接关闭之后
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        //从会话中移除session
        User loginUser = (User) session.getAttributes().get("loginUser");
        Long userId = (Long) session.getAttributes().get("userId");
        Long pictureId = Long.valueOf((String) session.getAttributes().get("pictureId"));
        //如果当前用户是编辑者,那么就移除
        handleExitEditMessage(null, session, pictureId, loginUser);
        // 响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s离开编辑", loginUser.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUserVo(userService.getUserVo(loginUser));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
        //先广播给其它用户,然后再删除对应会话,避免最后一个用户离开编辑时报NPE
        Set<WebSocketSession> sessions = pictureSessions.get(pictureId);
        //从session中去除掉当前的会话连接
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
    }
}
