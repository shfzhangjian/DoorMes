package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopeAddReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsQualityStandardRoleScopeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardRoleScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardRoleScopeMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_ROLE_SCOPE_ROLE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_ROLE_SCOPE_STANDARD_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_ROLE_SCOPE_STANDARD_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCQUALITYSTANDARD_ROLE_SCOPE_TYPE_INVALID;

@Service
@Validated
public class QmsQualityStandardRoleScopeServiceImpl implements QmsQualityStandardRoleScopeService {

    private static final String SCOPE_INCOMING = "INCOMING";
    private static final String SCOPE_PROCESS = "PROCESS";
    private static final String SCOPE_FINISHED = "FINISHED";
    private static final String SCOPE_PACKAGING = "PACKAGING";
    private static final String SCOPE_PROCESS_GLUE_BOARD = "PROCESS_GLUE_BOARD";
    private static final Map<String, List<String>> SCOPE_APPLY_TYPES = new LinkedHashMap<>();

    static {
        SCOPE_APPLY_TYPES.put(SCOPE_INCOMING, List.of("IQC"));
        SCOPE_APPLY_TYPES.put(SCOPE_PROCESS, List.of("FAI", "IPQC"));
        SCOPE_APPLY_TYPES.put(SCOPE_FINISHED, List.of("FQC"));
        SCOPE_APPLY_TYPES.put(SCOPE_PACKAGING, List.of("OQC"));
        SCOPE_APPLY_TYPES.put(SCOPE_PROCESS_GLUE_BOARD, List.of("GLUE_BOARD_FAI"));
    }

    @Resource
    private QmsQualityStandardRoleScopeMapper roleScopeMapper;
    @Resource
    private QmsQualityStandardMapper qualityStandardMapper;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public CurrentUserStandardScope getCurrentUserStandardScope() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || SecurityFrameworkUtils.skipPermissionCheck()
                || permissionApi.getSuperAdminUserIdList().contains(loginUserId)) {
            return new CurrentUserStandardScope(true, Collections.emptySet());
        }
        Set<Long> roleIds = permissionApi.getRoleIdListByUserId(loginUserId);
        if (roleIds == null || roleIds.isEmpty()) {
            return new CurrentUserStandardScope(false, Collections.emptySet());
        }
        Set<Long> standardIds = roleScopeMapper.selectListByRoleIds(roleIds).stream()
                .map(QmsQualityStandardRoleScopeDO::getStandardId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return new CurrentUserStandardScope(false, standardIds);
    }

    @Override
    public PageResult<QmsQualityStandardRoleScopeRespVO> getScopePage(QmsQualityStandardRoleScopePageReqVO pageReqVO) {
        validateRoleId(pageReqVO.getRoleId());
        String scopeType = normalizeScopeType(pageReqVO.getScopeType());
        pageReqVO.setScopeType(scopeType);
        List<QmsQualityStandardRoleScopeDO> scopes = roleScopeMapper.selectListByRoleIdAndScopeType(
                pageReqVO.getRoleId(), scopeType);
        if (scopes.isEmpty()) {
            return PageResult.empty();
        }
        Map<Long, QmsQualityStandardRoleScopeDO> scopeMap = scopes.stream()
                .collect(Collectors.toMap(QmsQualityStandardRoleScopeDO::getStandardId, Function.identity(), (a, b) -> a));
        LambdaQueryWrapperX<QmsQualityStandardDO> wrapper = buildCandidateWrapper(pageReqVO, SCOPE_APPLY_TYPES.get(scopeType))
                .in(QmsQualityStandardDO::getId, scopeMap.keySet())
                .orderByDesc(QmsQualityStandardDO::getId);
        PageResult<QmsQualityStandardDO> pageResult = qualityStandardMapper.selectPage(pageReqVO, wrapper);
        List<QmsQualityStandardRoleScopeRespVO> rows = pageResult.getList().stream()
                .map(standard -> buildResp(scopeMap.get(standard.getId()), standard))
                .toList();
        return new PageResult<>(rows, pageResult.getTotal());
    }

    @Override
    public PageResult<QmsQualityStandardRoleScopeRespVO> getCandidatePage(QmsQualityStandardRoleScopePageReqVO pageReqVO) {
        validateRoleId(pageReqVO.getRoleId());
        String scopeType = normalizeScopeType(pageReqVO.getScopeType());
        pageReqVO.setScopeType(scopeType);
        Set<Long> assignedStandardIds = CollectionUtils.convertSet(
                roleScopeMapper.selectListByRoleId(pageReqVO.getRoleId()),
                QmsQualityStandardRoleScopeDO::getStandardId);
        LambdaQueryWrapperX<QmsQualityStandardDO> wrapper = buildCandidateWrapper(pageReqVO, SCOPE_APPLY_TYPES.get(scopeType));
        if (!assignedStandardIds.isEmpty()) {
            wrapper.notIn(QmsQualityStandardDO::getId, assignedStandardIds);
        }
        wrapper.orderByDesc(QmsQualityStandardDO::getId);
        PageResult<QmsQualityStandardDO> pageResult = qualityStandardMapper.selectPage(pageReqVO, wrapper);
        List<QmsQualityStandardRoleScopeRespVO> rows = pageResult.getList().stream()
                .map(standard -> buildCandidateResp(scopeType, standard))
                .toList();
        return new PageResult<>(rows, pageResult.getTotal());
    }

    @Override
    public Map<String, Long> getScopeCount(Long roleId) {
        validateRoleId(roleId);
        Map<String, Long> countMap = SCOPE_APPLY_TYPES.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> 0L, (a, b) -> a, LinkedHashMap::new));
        roleScopeMapper.selectListByRoleId(roleId).forEach(scope ->
                countMap.computeIfPresent(scope.getScopeType(), (key, count) -> count + 1));
        return countMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addScopes(QmsQualityStandardRoleScopeAddReqVO reqVO) {
        validateRoleId(reqVO.getRoleId());
        String scopeType = normalizeScopeType(reqVO.getScopeType());
        List<Long> standardIds = reqVO.getStandardIds() == null ? Collections.emptyList() : reqVO.getStandardIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (standardIds.isEmpty()) {
            throw exception(HCQUALITYSTANDARD_ROLE_SCOPE_STANDARD_REQUIRED);
        }

        Map<Long, QmsQualityStandardDO> standardMap = loadStandardMap(standardIds);
        if (standardMap.size() != standardIds.size()) {
            throw exception(HCQUALITYSTANDARD_ROLE_SCOPE_STANDARD_INVALID);
        }

        List<String> allowedApplyTypes = SCOPE_APPLY_TYPES.get(scopeType);
        for (Long standardId : standardIds) {
            QmsQualityStandardDO standard = standardMap.get(standardId);
            String applyType = normalizeText(standard.getApplyType());
            if (!allowedApplyTypes.contains(applyType)) {
                throw exception(HCQUALITYSTANDARD_ROLE_SCOPE_STANDARD_INVALID);
            }
        }

        Map<Long, QmsQualityStandardRoleScopeDO> existingMap = roleScopeMapper
                .selectListByRoleIdAndStandardIds(reqVO.getRoleId(), standardIds)
                .stream()
                .collect(Collectors.toMap(QmsQualityStandardRoleScopeDO::getStandardId, Function.identity(), (a, b) -> a));
        List<QmsQualityStandardRoleScopeDO> inserts = new ArrayList<>();
        for (Long standardId : standardIds) {
            QmsQualityStandardDO standard = standardMap.get(standardId);
            QmsQualityStandardRoleScopeDO existing = existingMap.get(standardId);
            if (existing != null) {
                if (!scopeType.equals(existing.getScopeType())) {
                    existing.setScopeType(scopeType);
                    existing.setStandardApplyType(normalizeText(standard.getApplyType()));
                    roleScopeMapper.updateById(existing);
                }
                continue;
            }
            inserts.add(QmsQualityStandardRoleScopeDO.builder()
                    .roleId(reqVO.getRoleId())
                    .scopeType(scopeType)
                    .standardId(standardId)
                    .standardApplyType(normalizeText(standard.getApplyType()))
                    .build());
        }
        inserts.forEach(roleScopeMapper::insert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeScopes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> deleteIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (deleteIds.isEmpty()) {
            return;
        }
        roleScopeMapper.deleteBatchIds(deleteIds);
    }

    private LambdaQueryWrapperX<QmsQualityStandardDO> buildCandidateWrapper(QmsQualityStandardRoleScopePageReqVO reqVO,
                                                                           Collection<String> applyTypes) {
        LambdaQueryWrapperX<QmsQualityStandardDO> wrapper = new LambdaQueryWrapperX<QmsQualityStandardDO>()
                .in(QmsQualityStandardDO::getApplyType, applyTypes)
                .likeIfPresent(QmsQualityStandardDO::getStandardName, reqVO.getStandardName())
                .likeIfPresent(QmsQualityStandardDO::getStandardNo, reqVO.getStandardNo())
                .likeIfPresent(QmsQualityStandardDO::getMaterialCode, reqVO.getMaterialCode());
        List<String> standardKeywords = parseStandardKeywords(reqVO.getStandardKeyword());
        if (!standardKeywords.isEmpty()) {
            wrapper.and(query -> {
                boolean first = true;
                for (String keyword : standardKeywords) {
                    if (!first) {
                        query.or();
                    }
                    query.like(QmsQualityStandardDO::getStandardName, keyword)
                            .or()
                            .like(QmsQualityStandardDO::getStandardNo, keyword)
                            .or()
                            .like(QmsQualityStandardDO::getMaterialCode, keyword);
                    first = false;
                }
            });
        }
        List<String> modelKeywords = parseCombinedKeywords(reqVO.getModelKeyword());
        if (!modelKeywords.isEmpty()) {
            wrapper.and(query -> {
                boolean first = true;
                for (String keyword : modelKeywords) {
                    if (!first) {
                        query.or();
                    }
                    query.like(QmsQualityStandardDO::getProductModelCode, keyword)
                            .or()
                            .like(QmsQualityStandardDO::getProductModelName, keyword)
                            .or()
                            .like(QmsQualityStandardDO::getGlueBoardModel, keyword)
                            .or()
                            .like(QmsQualityStandardDO::getSpecification, keyword);
                    first = false;
                }
            });
        }
        List<String> processKeywords = parseCombinedKeywords(reqVO.getProcessKeyword());
        if (!processKeywords.isEmpty()) {
            wrapper.and(query -> {
                boolean first = true;
                for (String keyword : processKeywords) {
                    if (!first) {
                        query.or();
                    }
                    query.like(QmsQualityStandardDO::getProcessCode, keyword)
                            .or()
                            .like(QmsQualityStandardDO::getProcessName, keyword);
                    first = false;
                }
            });
        }
        return wrapper;
    }

    private List<String> parseStandardKeywords(String keyword) {
        return parseKeywords(keyword, "[,，;；]+");
    }

    private List<String> parseCombinedKeywords(String keyword) {
        return parseKeywords(keyword, "[,，;；/／]+");
    }

    private List<String> parseKeywords(String keyword, String delimiterRegex) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        return List.of(keyword.split(delimiterRegex)).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private Map<Long, QmsQualityStandardDO> loadStandardMap(Collection<Long> standardIds) {
        if (standardIds == null || standardIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return qualityStandardMapper.selectBatchIds(standardIds).stream()
                .collect(Collectors.toMap(QmsQualityStandardDO::getId, Function.identity(), (a, b) -> a));
    }

    private QmsQualityStandardRoleScopeRespVO buildResp(QmsQualityStandardRoleScopeDO scope, QmsQualityStandardDO standard) {
        QmsQualityStandardRoleScopeRespVO respVO = new QmsQualityStandardRoleScopeRespVO();
        respVO.setId(scope.getId());
        respVO.setRoleId(scope.getRoleId());
        respVO.setScopeType(scope.getScopeType());
        respVO.setStandardId(scope.getStandardId());
        respVO.setStandardApplyType(scope.getStandardApplyType());
        respVO.setCreateTime(scope.getCreateTime());
        fillStandard(respVO, standard);
        return respVO;
    }

    private QmsQualityStandardRoleScopeRespVO buildCandidateResp(String scopeType, QmsQualityStandardDO standard) {
        QmsQualityStandardRoleScopeRespVO respVO = new QmsQualityStandardRoleScopeRespVO();
        respVO.setScopeType(scopeType);
        respVO.setStandardId(standard.getId());
        respVO.setStandardApplyType(standard.getApplyType());
        fillStandard(respVO, standard);
        return respVO;
    }

    private void fillStandard(QmsQualityStandardRoleScopeRespVO respVO, QmsQualityStandardDO standard) {
        if (standard == null) {
            return;
        }
        respVO.setStandardNo(standard.getStandardNo());
        respVO.setStandardName(standard.getStandardName());
        respVO.setGlueBoardModel(standard.getGlueBoardModel());
        respVO.setMaterialCode(standard.getMaterialCode());
        respVO.setMaterialName(standard.getMaterialName());
        respVO.setSpecification(standard.getSpecification());
        respVO.setProductModelCode(standard.getProductModelCode());
        respVO.setProductModelName(standard.getProductModelName());
        respVO.setProdTypeName(standard.getProdTypeName());
        respVO.setProcessCode(standard.getProcessCode());
        respVO.setProcessName(standard.getProcessName());
        respVO.setVersion(standard.getVersion());
        respVO.setStatus(standard.getStatus());
        respVO.setAuditStatus(standard.getAuditStatus());
    }

    private void validateRoleId(Long roleId) {
        if (roleId == null) {
            throw exception(HCQUALITYSTANDARD_ROLE_SCOPE_ROLE_REQUIRED);
        }
    }

    private String normalizeScopeType(String scopeType) {
        String normalized = normalizeText(scopeType);
        if (!SCOPE_APPLY_TYPES.containsKey(normalized)) {
            throw exception(HCQUALITYSTANDARD_ROLE_SCOPE_TYPE_INVALID);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }
}
