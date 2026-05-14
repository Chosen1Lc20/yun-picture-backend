package com.lc.yunpicturebackend.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.lc.yunpicturebackend.auth.model.SpaceUserAuthConfig;
import com.lc.yunpicturebackend.auth.model.SpaceUserPermission;
import com.lc.yunpicturebackend.auth.model.SpaceUserRole;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.entity.Space;
import com.lc.yunpicturebackend.model.entity.SpaceUser;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.lc.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.lc.yunpicturebackend.service.SpaceUserService;
import com.lc.yunpicturebackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载配置文件到对象，并提供根据角色获取权限列表的方法
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String jsonStr = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(jsonStr, SpaceUserAuthConfig.class);
    }

    /** attention 核心
     * 根据空间获取当前用户权限列表
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRoles(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return new ArrayList<>();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRoles(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

    /**
     * 根据角色获取权限列表的方法
     */
    public static List<String> getPermissionsByRoles(String role){
        if(StrUtil.isBlank(role)){
            return new ArrayList<>();
        }
        //找到匹配的角色
        List<SpaceUserRole> roles = SPACE_USER_AUTH_CONFIG.getRoles();
        SpaceUserRole spaceUserRole = roles.stream().
                filter((r) -> role.equals(r.getKey()))
                .findFirst().orElse(null);
        if(spaceUserRole == null){
            return new ArrayList<>();
        }
        return spaceUserRole.getPermissions();
    }

}
