package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.*;
import cn.iocoder.yudao.module.system.convert.auth.AuthConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.logger.LoginLogTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.service.auth.AdminAuthService;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.social.SocialClientService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 认证")
@RestController
@RequestMapping("/system/auth")
@Validated
@Slf4j
public class AuthController {

    private static final Comparator<RoleDO> ROLE_SORT_COMPARATOR = Comparator
            .comparing(RoleDO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(RoleDO::getId);
    private static final Comparator<MenuDO> MENU_SORT_COMPARATOR = Comparator
            .comparing(MenuDO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(MenuDO::getId);
    private static final String INSTALL_PACKAGE_DOWNLOAD_PATH =
            "/admin-api/mes/hc/print-agent-package/latest/download?packageCode=";
    private static final Map<String, String> LOGIN_DOWNLOAD_PACKAGE_CODES = Map.of(
            "app", "HC_MES_PYDESK_APP",
            "print", "HC_MES_PRINT_AGENT",
            "print-driver", "HC_MES_PRINT_DRIVER");

    @Resource
    private AdminAuthService authService;
    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private SocialClientService socialClientService;

    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "使用账号密码登录")
    public CommonResult<AuthLoginRespVO> login(@RequestBody @Valid AuthLoginReqVO reqVO) {
        return success(authService.login(reqVO));
    }

    @GetMapping("/download/{type}")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "登录页公开下载工位安装包")
    @Parameter(name = "type", description = "下载类型：app=工位客户端，print=工位打印服务，print-driver=打印驱动", required = true)
    public void downloadLoginPackage(@PathVariable("type") String type,
                                     HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        String packageCode = LOGIN_DOWNLOAD_PACKAGE_CODES.get(type);
        if (StrUtil.isBlank(packageCode)) {
            writeNotFound(response, "下载类型不存在");
            return;
        }

        response.sendRedirect(request.getContextPath() + INSTALL_PACKAGE_DOWNLOAD_PATH + packageCode);
    }

    private void writeNotFound(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    @PostMapping("/logout")
    @PermitAll
    @Operation(summary = "登出系统")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        }
        return success(true);
    }

    @PostMapping("/refresh-token")
    @PermitAll
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true)
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return success(authService.refreshToken(refreshToken));
    }

    @GetMapping("/get-permission-info")
    @Operation(summary = "获取登录用户的权限信息")
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            return success(null);
        }

        // 1.2 获得角色列表
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            return success(AuthConvert.INSTANCE.convert(user, Collections.emptyList(), Collections.emptyList()));
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色
        roles.sort(ROLE_SORT_COMPARATOR);

        // 1.3 获得菜单列表
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
        List<MenuDO> menuList = menuService.getMenuList(menuIds);
        List<MenuDO> permissionMenuList = new ArrayList<>(menuList);
        List<MenuDO> routeMenuList = menuService.filterDisableMenus(menuList);

        // 2. 拼接结果返回
        AuthPermissionInfoRespVO respVO = AuthConvert.INSTANCE.convert(user, roles, new ArrayList<>(routeMenuList));
        respVO.setPermissions(convertSet(permissionMenuList, MenuDO::getPermission));
        respVO.getUser().setHomePath(buildRoleHomePath(roles, routeMenuList));
        return success(respVO);
    }

    private String buildRoleHomePath(List<RoleDO> roles, List<MenuDO> menuList) {
        if (CollUtil.isEmpty(roles) || CollUtil.isEmpty(menuList)) {
            return null;
        }
        Map<Long, MenuDO> menuMap = convertMap(menuList, MenuDO::getId);
        Set<Long> handledPortalMenuIds = new HashSet<>();
        for (RoleDO role : roles) {
            Long portalMenuId = role.getPortalMenuId();
            if (portalMenuId == null || !handledPortalMenuIds.add(portalMenuId)) {
                continue;
            }
            MenuDO portalMenu = menuMap.get(portalMenuId);
            if (portalMenu == null || MenuTypeEnum.BUTTON.getType().equals(portalMenu.getType())) {
                continue;
            }
            MenuDO targetMenu = resolvePortalTargetMenu(portalMenu, menuList);
            String homePath = buildRoutePath(targetMenu, menuMap);
            if (StrUtil.isNotBlank(homePath)) {
                return homePath;
            }
        }
        return null;
    }

    private MenuDO resolvePortalTargetMenu(MenuDO portalMenu, List<MenuDO> menuList) {
        if (MenuTypeEnum.MENU.getType().equals(portalMenu.getType())) {
            return portalMenu;
        }
        return menuList.stream()
                .filter(menu -> Objects.equals(menu.getParentId(), portalMenu.getId()))
                .sorted(MENU_SORT_COMPARATOR)
                .map(menu -> resolvePortalTargetMenu(menu, menuList))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(portalMenu);
    }

    private String buildRoutePath(MenuDO menu, Map<Long, MenuDO> menuMap) {
        if (menu == null) {
            return null;
        }
        LinkedList<String> pathSegments = new LinkedList<>();
        Set<Long> visitedMenuIds = new HashSet<>();
        MenuDO current = menu;
        while (current != null && visitedMenuIds.add(current.getId())) {
            String path = normalizePathSegment(current.getPath());
            if (StrUtil.isNotBlank(path)) {
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    return null;
                }
                pathSegments.addFirst(path);
            }
            if (MenuDO.ID_ROOT.equals(current.getParentId())) {
                break;
            }
            current = menuMap.get(current.getParentId());
        }
        return CollUtil.isEmpty(pathSegments) ? null : "/" + String.join("/", pathSegments);
    }

    private String normalizePathSegment(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String result = path.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @PostMapping("/register")
    @PermitAll
    @Operation(summary = "注册用户")
    public CommonResult<AuthLoginRespVO> register(@RequestBody @Valid AuthRegisterReqVO registerReqVO) {
        return success(authService.register(registerReqVO));
    }

    // ========== 短信登录相关 ==========

    @PostMapping("/sms-login")
    @PermitAll
    @Operation(summary = "使用短信验证码登录")
    // 可按需开启限流：https://github.com/YunaiV/ruoyi-vue-pro/issues/851
    // @RateLimiter(time = 60, count = 6, keyResolver = ExpressionRateLimiterKeyResolver.class, keyArg = "#reqVO.mobile")
    public CommonResult<AuthLoginRespVO> smsLogin(@RequestBody @Valid AuthSmsLoginReqVO reqVO) {
        return success(authService.smsLogin(reqVO));
    }

    @PostMapping("/send-sms-code")
    @PermitAll
    @Operation(summary = "发送手机验证码")
    public CommonResult<Boolean> sendLoginSmsCode(@RequestBody @Valid AuthSmsSendReqVO reqVO) {
        authService.sendSmsCode(reqVO);
        return success(true);
    }

    @PostMapping("/reset-password")
    @PermitAll
    @Operation(summary = "重置密码")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AuthResetPasswordReqVO reqVO) {
        authService.resetPassword(reqVO);
        return success(true);
    }

    // ========== 社交登录相关 ==========

    @GetMapping("/social-auth-redirect")
    @PermitAll
    @Operation(summary = "社交授权的跳转")
    @Parameters({
            @Parameter(name = "type", description = "社交类型", required = true),
            @Parameter(name = "redirectUri", description = "回调路径")
    })
    public CommonResult<String> socialLogin(@RequestParam("type") Integer type,
                                            @RequestParam("redirectUri") String redirectUri) {
        return success(socialClientService.getAuthorizeUrl(
                type, UserTypeEnum.ADMIN.getValue(), redirectUri));
    }

    @PostMapping("/social-login")
    @PermitAll
    @Operation(summary = "社交快捷登录，使用 code 授权码", description = "适合未登录的用户，但是社交账号已绑定用户")
    public CommonResult<AuthLoginRespVO> socialQuickLogin(@RequestBody @Valid AuthSocialLoginReqVO reqVO) {
        return success(authService.socialLogin(reqVO));
    }

}
