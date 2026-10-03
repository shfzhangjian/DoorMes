package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardRoleScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardRoleScopeMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsQualityStandardRoleScopeServiceImplTest {

    private static final Long LOGIN_USER_ID = 100L;

    @InjectMocks
    private QmsQualityStandardRoleScopeServiceImpl service;

    @Mock
    private QmsQualityStandardRoleScopeMapper roleScopeMapper;
    @Mock
    private QmsQualityStandardMapper qualityStandardMapper;
    @Mock
    private PermissionApi permissionApi;

    @BeforeEach
    void setUpLoginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(LOGIN_USER_ID);
        loginUser.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    @AfterEach
    void clearLoginUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldMergeStandardScopesFromAllCurrentUserRoles() {
        Set<Long> roleIds = Set.of(10L, 20L);
        when(permissionApi.getSuperAdminUserIdList()).thenReturn(Collections.emptySet());
        when(permissionApi.getRoleIdListByUserId(LOGIN_USER_ID)).thenReturn(roleIds);
        when(roleScopeMapper.selectListByRoleIds(roleIds)).thenReturn(List.of(
                scope(10L, 101L), scope(10L, 102L), scope(20L, 102L), scope(20L, 103L)));

        QmsQualityStandardRoleScopeService.CurrentUserStandardScope scope =
                service.getCurrentUserStandardScope();

        assertFalse(scope.unrestricted());
        assertEquals(Set.of(101L, 102L, 103L), scope.standardIds());
    }

    @Test
    void shouldReturnEmptyRestrictedScopeWhenCurrentUserHasNoConfiguredStandard() {
        Set<Long> roleIds = Set.of(10L);
        when(permissionApi.getSuperAdminUserIdList()).thenReturn(Collections.emptySet());
        when(permissionApi.getRoleIdListByUserId(LOGIN_USER_ID)).thenReturn(roleIds);
        when(roleScopeMapper.selectListByRoleIds(roleIds)).thenReturn(Collections.emptyList());

        QmsQualityStandardRoleScopeService.CurrentUserStandardScope scope =
                service.getCurrentUserStandardScope();

        assertFalse(scope.unrestricted());
        assertTrue(scope.standardIds().isEmpty());
    }

    @Test
    void shouldNotRestrictSuperAdmin() {
        when(permissionApi.getSuperAdminUserIdList()).thenReturn(Set.of(LOGIN_USER_ID));

        QmsQualityStandardRoleScopeService.CurrentUserStandardScope scope =
                service.getCurrentUserStandardScope();

        assertTrue(scope.unrestricted());
        assertTrue(scope.standardIds().isEmpty());
        verify(permissionApi, never()).getRoleIdListByUserId(LOGIN_USER_ID);
    }

    private QmsQualityStandardRoleScopeDO scope(Long roleId, Long standardId) {
        return QmsQualityStandardRoleScopeDO.builder()
                .roleId(roleId)
                .standardId(standardId)
                .build();
    }
}
