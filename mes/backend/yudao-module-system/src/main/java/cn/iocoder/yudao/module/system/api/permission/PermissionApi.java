package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;

import java.util.Collection;
import java.util.Set;

/**
 * 权限 API 接口
 *
 * @author 芋道源码
 */
public interface PermissionApi extends PermissionCommonApi {

    /**
     * 获得用户拥有的角色编号集合
     *
     * @param userId 用户编号
     * @return 角色编号集合
     */
    Set<Long> getRoleIdListByUserId(Long userId);

    /**
     * 获得拥有多个角色的用户编号集合
     *
     * @param roleIds 角色编号集合
     * @return 用户编号集合
     */
    Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds);

    /**
     * 获得拥有指定权限的用户编号集合
     *
     * @param permissions 权限标识集合
     * @return 用户编号集合
     */
    Set<Long> getUserIdListByPermissions(Collection<String> permissions);

    /**
     * 获得超级管理员用户编号集合
     *
     * @return 用户编号集合
     */
    Set<Long> getSuperAdminUserIdList();

}
