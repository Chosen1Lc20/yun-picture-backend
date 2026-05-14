package com.lc.yunpicturebackend.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * StpLogic 门面类，管理项目中所有的 StpLogic 账号体系
 * 添加 @Component 注解的目的是确保静态属性 DEFAULT 和 SPACE 被初始化
 */
@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";

    /**
     * 默认原生会话对象，项目中目前没使用到
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Space 会话对象，管理 Space 表所有账号的登录、权限认证
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);

    public static void main(String[] args) {
        // 在当前会话进行 Space 账号登录
        StpKit.SPACE.login(10001);

        // 检测当前会话是否以 Space 账号登录，并具有 picture:edit 权限
        StpKit.SPACE.checkPermission("picture:edit");

        // 获取当前 Space 会话的 Session 对象，并进行写值操作
        StpKit.SPACE.getSession().set("user", "程序员cl");

    }
}
