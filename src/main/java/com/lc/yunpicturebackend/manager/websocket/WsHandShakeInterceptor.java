package com.lc.yunpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import com.lc.yunpicturebackend.auth.SpaceUserAuthManager;
import com.lc.yunpicturebackend.auth.model.SpaceUserPermissionConstant;
import com.lc.yunpicturebackend.constant.UserConstant;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.SpaceUser;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.service.SpaceService;
import com.lc.yunpicturebackend.service.SpaceUserService;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WsHandShakeInterceptor implements HandshakeInterceptor {

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    /**
     *
     * @param request Spring 对服务端 HTTP 请求的通用封装，屏蔽了 Tomcat/Jetty 等容器的差异。
     * @param response 对应握手阶段的 HTTP 响应对象，可设置响应头、拒绝握手、返回错误码等。
     * @param wsHandler 握手成功后，真正处理 WebSocket 连接、消息收发、断开连接 的业务处理器。
     * @param attributes WebSocket 会话属性容器
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        //从http请求中获得用户信息
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String pictureId = httpServletRequest.getParameter("pictureId");
            if (ObjUtil.isNull(pictureId)) {
                log.error("未指定图片id,拒绝握手");
                return false;
            }
            //http的session
            HttpSession session = httpServletRequest.getSession();
            User loginUser = (User) session.getAttribute(UserConstant.USER_LOGIN_STATE);
            if (ObjUtil.isNull(loginUser)) {
                log.error("当前用户未登录,拒绝握手");
                return false;
            }
            Picture picture = pictureService.getById(Long.valueOf(pictureId));
            //图片不存在
            if (ObjUtil.isNull(picture)) {
                log.error("图片不存在,拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            //公共图库
            if (ObjUtil.isNull(spaceId)) {
                log.error("不支持协同编辑公共图库的图片,拒绝握手");
                return false;
            }
            //空间不存在
            Space space = spaceService.getById(spaceId);
            if (ObjUtil.isNull(space)) {
                log.error("空间不存在,拒绝握手");
                return false;
            }
            //不是团队空间
            if (!space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
                log.error("仅支持团队空间内的协同编辑,拒绝握手");
                return false;
            }
            //校验权限,获取权限列表
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("不具有编辑权限,拒绝握手");
                return false;
            }
            //存储到websocket的session中
            attributes.put("loginUser", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", pictureId);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        log.info("握手结束啦");
    }
}
