package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierMaskFieldRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierMaskFieldSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierScopeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.MesSupplierPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.supplier.vo.MesSupplierRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierMaskFieldDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierScopeMemberDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierMaskFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierScopeMemberMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_SCOPE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_SCOPE_ADMIN_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_SCOPE_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_SCOPE_IN_USE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_SCOPE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_VERSION_CONFLICT;

@Service
@Validated
public class SrmSupplierScopeServiceImpl implements SrmSupplierScopeService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final String ROLE_SUPPLIER_SUPER_ADMIN = "srm_supplier_super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";
    private static final int INITIAL_VERSION = 0;
    private static final int DEFAULT_SORT = 0;
    private static final String MASK_PLACEHOLDER = "*";
    private static final Set<String> NON_MASK_FIELDS = Set.of("supplierCode", "supplierName");

    @Resource
    private SrmSupplierScopeMapper scopeMapper;
    @Resource
    private SrmSupplierScopeMemberMapper memberMapper;
    @Resource
    private SrmSupplierMaskFieldMapper maskFieldMapper;
    @Resource
    private MesSupplierMapper supplierMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScope(SrmSupplierScopeSaveReqVO reqVO) {
        assertSupplierSuperAdmin();
        SrmSupplierScopeDO scope = new SrmSupplierScopeDO();
        copyScopeFields(reqVO, scope);
        scope.setScopeCode(buildScopeCode());
        scope.setStatus(StrUtil.blankToDefault(reqVO.getStatus(), STATUS_ENABLED));
        scope.setSort(reqVO.getSort() == null ? DEFAULT_SORT : reqVO.getSort());
        scope.setVersion(INITIAL_VERSION);
        scopeMapper.insert(scope);
        saveMembers(scope.getId(), reqVO.getMembers());
        return scope.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScope(SrmSupplierScopeSaveReqVO reqVO) {
        assertSupplierSuperAdmin();
        SrmSupplierScopeDO scope = validateScope(reqVO.getId());
        copyScopeFields(reqVO, scope);
        scope.setVersion(reqVO.getVersion());
        if (scopeMapper.updateById(scope) == 0) {
            throw exception(SRM_VERSION_CONFLICT);
        }
        memberMapper.deleteByScopeId(scope.getId());
        saveMembers(scope.getId(), reqVO.getMembers());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScope(Long id) {
        assertSupplierSuperAdmin();
        validateScope(id);
        Long count = supplierMapper.selectCount(new LambdaQueryWrapperX<MesSupplierDO>()
                .eq(MesSupplierDO::getScopeId, id));
        if (count != null && count > 0) {
            throw exception(SRM_SUPPLIER_SCOPE_IN_USE);
        }
        memberMapper.deleteByScopeId(id);
        scopeMapper.deleteById(id);
    }

    @Override
    public SrmSupplierScopeRespVO getScope(Long id) {
        assertSupplierSuperAdmin();
        return buildScopeResp(validateScope(id), true);
    }

    @Override
    public PageResult<SrmSupplierScopeRespVO> getScopePage(SrmSupplierScopePageReqVO reqVO) {
        assertSupplierSuperAdmin();
        PageResult<SrmSupplierScopeDO> page = scopeMapper.selectPage(reqVO);
        List<Long> scopeIds = page.getList().stream().map(SrmSupplierScopeDO::getId).toList();
        Map<Long, Integer> memberCountMap = countMembers(scopeIds);
        List<SrmSupplierScopeRespVO> list = page.getList().stream()
                .map(scope -> {
                    SrmSupplierScopeRespVO resp = buildScopeResp(scope, false);
                    resp.setMemberCount(memberCountMap.getOrDefault(scope.getId(), 0));
                    return resp;
                })
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public List<SrmSupplierScopeRespVO> getSimpleScopeList() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (isSupplierSuperAdmin(userId)) {
            return scopeMapper.selectEnabledList().stream().map(scope -> buildScopeResp(scope, false)).toList();
        }
        Set<Long> scopeIds = resolveAccessMap(userId).keySet();
        if (CollUtil.isEmpty(scopeIds)) {
            return List.of();
        }
        return scopeMapper.selectListByIds(scopeIds).stream()
                .filter(scope -> STATUS_ENABLED.equals(scope.getStatus()))
                .map(scope -> buildScopeResp(scope, false))
                .toList();
    }

    @Override
    public List<SrmSupplierMaskFieldRespVO> getMaskFields() {
        assertSupplierSuperAdmin();
        return BeanUtils.toBean(maskFieldMapper.selectOrderedList(), SrmSupplierMaskFieldRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMaskFields(SrmSupplierMaskFieldSaveReqVO reqVO) {
        assertSupplierSuperAdmin();
        Map<String, Boolean> requestMap = reqVO.getItems().stream()
                .collect(Collectors.toMap(item -> StrUtil.trim(item.getFieldKey()), SrmSupplierMaskFieldSaveReqVO.Item::getMaskEnabled,
                        (oldValue, newValue) -> newValue, LinkedHashMap::new));
        for (SrmSupplierMaskFieldDO field : maskFieldMapper.selectOrderedList()) {
            if (!requestMap.containsKey(field.getFieldKey()) || NON_MASK_FIELDS.contains(field.getFieldKey())) {
                continue;
            }
            SrmSupplierMaskFieldDO updateObj = new SrmSupplierMaskFieldDO();
            updateObj.setId(field.getId());
            updateObj.setMaskEnabled(Boolean.TRUE.equals(requestMap.get(field.getFieldKey())));
            updateObj.setVersion(field.getVersion());
            if (maskFieldMapper.updateById(updateObj) == 0) {
                throw exception(SRM_VERSION_CONFLICT);
            }
        }
    }

    @Override
    public void applySupplierScopeFilter(MesSupplierPageReqVO reqVO) {
        Collection<Long> scopeIds = getCurrentAccessibleScopeIds();
        if (scopeIds == null) {
            reqVO.setScopeRestricted(false);
            reqVO.setAccessibleScopeIds(null);
            return;
        }
        reqVO.setScopeRestricted(true);
        reqVO.setAccessibleScopeIds(CollUtil.isEmpty(scopeIds) ? List.of(-1L) : scopeIds);
    }

    @Override
    public Collection<Long> getCurrentAccessibleScopeIds() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (isSupplierSuperAdmin(userId)) {
            return null;
        }
        return resolveAccessMap(userId).keySet();
    }

    @Override
    public MesSupplierRespVO buildSupplierResp(MesSupplierDO supplier) {
        SupplierAccess access = resolveSupplierAccess(supplier);
        MesSupplierRespVO respVO = BeanUtils.toBean(supplier, MesSupplierRespVO.class);
        respVO.setViewPermission(access.permissionLevel());
        respVO.setCanEdit(access.canEdit());
        respVO.setMaskedFields(List.of());
        if (PERMISSION_MASKED.equals(access.permissionLevel())) {
            applyMask(respVO);
        }
        return respVO;
    }

    @Override
    public void assertSupplierVisible(MesSupplierDO supplier) {
        if (!resolveSupplierAccess(supplier).visible()) {
            throw exception(SRM_SUPPLIER_SCOPE_ACCESS_DENIED);
        }
    }

    @Override
    public void assertSupplierEditable(MesSupplierDO supplier) {
        if (!resolveSupplierAccess(supplier).canEdit()) {
            throw exception(SRM_SUPPLIER_SCOPE_ACCESS_DENIED);
        }
    }

    @Override
    public boolean isSupplierSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyRoles(userId, ROLE_SUPER_ADMIN, ROLE_SUPPLIER_SUPER_ADMIN)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private void assertSupplierSuperAdmin() {
        if (!isSupplierSuperAdmin(SecurityFrameworkUtils.getLoginUserId())) {
            throw exception(SRM_SUPPLIER_SCOPE_ADMIN_REQUIRED);
        }
    }

    private SupplierAccess resolveSupplierAccess(MesSupplierDO supplier) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (supplier == null) {
            return new SupplierAccess(false, false, PERMISSION_MASKED);
        }
        if (isSupplierSuperAdmin(userId)) {
            return new SupplierAccess(true, true, PERMISSION_EDIT);
        }
        if (supplier.getScopeId() == null) {
            return new SupplierAccess(false, false, PERMISSION_MASKED);
        }
        String permission = resolveAccessMap(userId).get(supplier.getScopeId());
        if (StrUtil.isBlank(permission)) {
            return new SupplierAccess(false, false, PERMISSION_MASKED);
        }
        return new SupplierAccess(true, PERMISSION_EDIT.equals(permission), permission);
    }

    private Map<Long, String> resolveAccessMap(Long userId) {
        if (userId == null) {
            return Map.of();
        }
        List<SrmSupplierScopeMemberDO> members = memberMapper.selectListByUserId(userId);
        if (CollUtil.isEmpty(members)) {
            return Map.of();
        }
        Set<Long> scopeIds = members.stream()
                .map(SrmSupplierScopeMemberDO::getScopeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> enabledScopeIds = scopeMapper.selectListByIds(scopeIds).stream()
                .filter(scope -> STATUS_ENABLED.equals(scope.getStatus()))
                .map(SrmSupplierScopeDO::getId)
                .collect(Collectors.toSet());
        Map<Long, String> result = new LinkedHashMap<>();
        for (SrmSupplierScopeMemberDO member : members) {
            if (!enabledScopeIds.contains(member.getScopeId())) {
                continue;
            }
            result.merge(member.getScopeId(), normalizePermission(member.getPermissionLevel()),
                    this::higherPermission);
        }
        return result;
    }

    private void applyMask(MesSupplierRespVO respVO) {
        Set<String> maskFields = maskFieldMapper.selectOrderedList().stream()
                .filter(field -> Boolean.TRUE.equals(field.getMaskEnabled()))
                .map(SrmSupplierMaskFieldDO::getFieldKey)
                .filter(field -> !NON_MASK_FIELDS.contains(field))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        respVO.setMaskedFields(new ArrayList<>(maskFields));
        for (String fieldName : maskFields) {
            maskFieldValue(respVO, fieldName);
        }
    }

    private void maskFieldValue(MesSupplierRespVO respVO, String fieldName) {
        try {
            Field field = MesSupplierRespVO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.getType().equals(String.class)) {
                field.set(respVO, MASK_PLACEHOLDER);
            } else if (!field.getType().isPrimitive()) {
                field.set(respVO, null);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // 字段配置允许兼容历史字段，当前响应对象没有的字段忽略。
        }
    }

    private void copyScopeFields(SrmSupplierScopeSaveReqVO reqVO, SrmSupplierScopeDO scope) {
        scope.setScopeName(StrUtil.trim(reqVO.getScopeName()));
        scope.setStatus(StrUtil.blankToDefault(reqVO.getStatus(), STATUS_ENABLED));
        if (!STATUS_ENABLED.equals(scope.getStatus()) && !STATUS_DISABLED.equals(scope.getStatus())) {
            scope.setStatus(STATUS_ENABLED);
        }
        scope.setSort(reqVO.getSort() == null ? DEFAULT_SORT : reqVO.getSort());
        scope.setRemark(StrUtil.trimToNull(reqVO.getRemark()));
    }

    private void saveMembers(Long scopeId, List<SrmSupplierScopeSaveReqVO.Member> reqMembers) {
        if (CollUtil.isEmpty(reqMembers)) {
            return;
        }
        Map<Long, String> permissionByUser = new LinkedHashMap<>();
        for (SrmSupplierScopeSaveReqVO.Member reqMember : reqMembers) {
            if (reqMember.getUserId() == null) {
                continue;
            }
            permissionByUser.merge(reqMember.getUserId(), normalizePermission(reqMember.getPermissionLevel()),
                    this::higherPermission);
        }
        if (permissionByUser.isEmpty()) {
            return;
        }
        adminUserApi.validateUserList(permissionByUser.keySet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(permissionByUser.keySet());
        for (Map.Entry<Long, String> entry : permissionByUser.entrySet()) {
            SrmSupplierScopeMemberDO member = new SrmSupplierScopeMemberDO();
            member.setScopeId(scopeId);
            member.setUserId(entry.getKey());
            AdminUserRespDTO user = userMap.get(entry.getKey());
            member.setUserName(user == null ? String.valueOf(entry.getKey())
                    : StrUtil.blankToDefault(user.getNickname(), user.getUsername()));
            member.setPermissionLevel(entry.getValue());
            member.setTenantId(TenantContextHolder.getTenantId());
            memberMapper.insert(member);
        }
    }

    private Map<Long, Integer> countMembers(Collection<Long> scopeIds) {
        if (CollUtil.isEmpty(scopeIds)) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        memberMapper.selectListByScopeIds(scopeIds).forEach(member ->
                result.merge(member.getScopeId(), 1, Integer::sum));
        return result;
    }

    private SrmSupplierScopeRespVO buildScopeResp(SrmSupplierScopeDO scope, boolean includeMembers) {
        SrmSupplierScopeRespVO respVO = BeanUtils.toBean(scope, SrmSupplierScopeRespVO.class);
        if (includeMembers) {
            List<SrmSupplierScopeRespVO.Member> members = BeanUtils.toBean(
                    memberMapper.selectListByScopeId(scope.getId()), SrmSupplierScopeRespVO.Member.class);
            members.sort(Comparator.comparing(SrmSupplierScopeRespVO.Member::getPermissionLevel,
                    Comparator.nullsLast(String::compareTo)));
            respVO.setMembers(members);
            respVO.setMemberCount(members.size());
        }
        return respVO;
    }

    private SrmSupplierScopeDO validateScope(Long id) {
        SrmSupplierScopeDO scope = id == null ? null : scopeMapper.selectById(id);
        if (scope == null) {
            throw exception(SRM_SUPPLIER_SCOPE_NOT_EXISTS);
        }
        return scope;
    }

    private String buildScopeCode() {
        for (int index = 0; index < 5; index++) {
            String code = "SRM-SCOPE-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            if (scopeMapper.selectByCode(code) == null) {
                return code;
            }
        }
        throw exception(SRM_SUPPLIER_SCOPE_CODE_EXISTS);
    }

    private String normalizePermission(String permissionLevel) {
        String normalized = StrUtil.blankToDefault(permissionLevel, PERMISSION_MASKED).trim().toUpperCase();
        if (PERMISSION_EDIT.equals(normalized) || PERMISSION_FULL.equals(normalized)
                || PERMISSION_MASKED.equals(normalized)) {
            return normalized;
        }
        return PERMISSION_MASKED;
    }

    private String higherPermission(String left, String right) {
        return permissionRank(left) >= permissionRank(right) ? left : right;
    }

    private int permissionRank(String permission) {
        if (PERMISSION_EDIT.equals(permission)) {
            return 3;
        }
        if (PERMISSION_FULL.equals(permission)) {
            return 2;
        }
        return 1;
    }

    private record SupplierAccess(boolean visible, boolean canEdit, String permissionLevel) {
    }

}
