package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryProjectScorerConfigReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryProjectDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryProjectScorerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryEvaluationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryProjectMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPreliminaryProjectScorerMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_ADMIN_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_PROJECT_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_PROJECT_IN_USE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PRELIMINARY_PROJECT_NOT_EXISTS;

@Service
@Validated
public class SrmPreliminaryProjectServiceImpl implements SrmPreliminaryProjectService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";

    @Resource
    private SrmPreliminaryProjectMapper projectMapper;
    @Resource
    private SrmPreliminaryProjectScorerMapper projectScorerMapper;
    @Resource
    private SrmPreliminaryEvaluationMapper evaluationMapper;
    @Resource
    private SrmEvaluationTemplateMapper templateMapper;
    @Resource
    private SrmEvaluationTemplateVersionMapper versionMapper;
    @Resource
    private SrmEvaluationTemplateItemMapper templateItemMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(SrmPreliminaryProjectSaveReqVO reqVO) {
        assertEvaluationAdmin();
        if (projectMapper.selectByProjectCode(StrUtil.trim(reqVO.getProjectCode())) != null) {
            throw exception(SRM_PRELIMINARY_PROJECT_CODE_EXISTS);
        }
        SrmPreliminaryProjectDO project = new SrmPreliminaryProjectDO();
        copyProjectFields(reqVO, project);
        project.setVersion(0);
        projectMapper.insert(project);
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(SrmPreliminaryProjectSaveReqVO reqVO) {
        assertEvaluationAdmin();
        SrmPreliminaryProjectDO project = validateProject(reqVO.getId());
        SrmPreliminaryProjectDO sameCode = projectMapper.selectByProjectCode(StrUtil.trim(reqVO.getProjectCode()));
        if (sameCode != null && !Objects.equals(sameCode.getId(), project.getId())) {
            throw exception(SRM_PRELIMINARY_PROJECT_CODE_EXISTS);
        }
        copyProjectFields(reqVO, project);
        projectMapper.updateById(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        assertEvaluationAdmin();
        SrmPreliminaryProjectDO project = validateProject(id);
        Long usedCount = evaluationMapper.selectCount(new LambdaQueryWrapperX<SrmPreliminaryEvaluationDO>()
                .eq(SrmPreliminaryEvaluationDO::getProjectId, project.getId()));
        if (usedCount != null && usedCount > 0) {
            throw exception(SRM_PRELIMINARY_PROJECT_IN_USE);
        }
        projectScorerMapper.delete(new LambdaQueryWrapperX<SrmPreliminaryProjectScorerDO>()
                .eq(SrmPreliminaryProjectScorerDO::getProjectId, project.getId()));
        projectMapper.deleteById(project.getId());
    }

    @Override
    public SrmPreliminaryProjectRespVO getProject(Long id) {
        SrmPreliminaryProjectDO project = validateProject(id);
        SrmPreliminaryProjectRespVO resp = BeanUtils.toBean(project, SrmPreliminaryProjectRespVO.class);
        if (project.getCurrentTemplateVersionId() != null) {
            TemplateSnapshot template = validatePublishedTemplate(project.getCurrentTemplateVersionId());
            List<SrmPreliminaryProjectScorerDO> configs = projectScorerMapper
                    .selectListByProjectAndTemplateVersion(project.getId(), project.getCurrentTemplateVersionId());
            resp.setScorerItems(buildScorerItems(template, configs));
        }
        return resp;
    }

    @Override
    public PageResult<SrmPreliminaryProjectRespVO> getProjectPage(SrmPreliminaryProjectPageReqVO reqVO) {
        PageResult<SrmPreliminaryProjectDO> page = projectMapper.selectPage(reqVO);
        return new PageResult<>(BeanUtils.toBean(page.getList(), SrmPreliminaryProjectRespVO.class), page.getTotal());
    }

    @Override
    public List<SrmPreliminaryProjectRespVO> getEnabledProjectList() {
        return BeanUtils.toBean(projectMapper.selectEnabledList(), SrmPreliminaryProjectRespVO.class);
    }

    @Override
    public SrmPreliminaryProjectRespVO getScorerConfig(Long projectId, Long templateVersionId) {
        SrmPreliminaryProjectDO project = validateProject(projectId);
        TemplateSnapshot template = validatePublishedTemplate(templateVersionId);
        SrmPreliminaryProjectRespVO resp = BeanUtils.toBean(project, SrmPreliminaryProjectRespVO.class);
        List<SrmPreliminaryProjectScorerDO> configs = projectScorerMapper
                .selectListByProjectAndTemplateVersion(projectId, templateVersionId);
        resp.setScorerItems(buildScorerItems(template, configs));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScorerConfig(SrmPreliminaryProjectScorerConfigReqVO reqVO) {
        assertEvaluationAdmin();
        SrmPreliminaryProjectDO project = validateProject(reqVO.getProjectId());
        TemplateSnapshot template = validatePublishedTemplate(reqVO.getTemplateVersionId());
        applyTemplateSnapshot(project, template);
        projectMapper.updateById(project);
        Map<Long, SrmPreliminaryProjectScorerConfigReqVO.Item> reqItemMap = reqVO.getItems().stream()
                .filter(item -> item.getTemplateItemId() != null)
                .collect(Collectors.toMap(SrmPreliminaryProjectScorerConfigReqVO.Item::getTemplateItemId,
                        Function.identity(), (left, right) -> right));
        ScorerResolveContext scorerContext = buildScorerResolveContext(reqItemMap, template.items());
        projectScorerMapper.deleteByProjectAndTemplateVersion(project.getId(), template.version().getId());
        for (SrmEvaluationTemplateItemDO templateItem : template.items()) {
            SrmPreliminaryProjectScorerDO config = buildProjectScorer(project.getId(), template, templateItem,
                    reqItemMap.get(templateItem.getId()), scorerContext);
            projectScorerMapper.insert(config);
        }
    }

    private List<SrmPreliminaryProjectRespVO.ScorerConfigItem> buildScorerItems(
            TemplateSnapshot template, List<SrmPreliminaryProjectScorerDO> configs) {
        Map<Long, SrmPreliminaryProjectScorerDO> configMap = configs.stream()
                .collect(Collectors.toMap(SrmPreliminaryProjectScorerDO::getTemplateItemId,
                        Function.identity(), (left, right) -> right));
        List<SrmPreliminaryProjectRespVO.ScorerConfigItem> result = new ArrayList<>();
        for (SrmEvaluationTemplateItemDO templateItem : template.items()) {
            SrmPreliminaryProjectScorerDO config = configMap.get(templateItem.getId());
            SrmPreliminaryProjectRespVO.ScorerConfigItem item = config == null
                    ? new SrmPreliminaryProjectRespVO.ScorerConfigItem()
                    : BeanUtils.toBean(config, SrmPreliminaryProjectRespVO.ScorerConfigItem.class);
            item.setTemplateId(template.template().getId());
            item.setTemplateVersionId(template.version().getId());
            item.setTemplateItemId(templateItem.getId());
            item.setGroupCodeSnapshot(templateItem.getGroupCode());
            item.setGroupNameSnapshot(templateItem.getGroupName());
            item.setGroupSort(templateItem.getGroupSort());
            item.setIndicatorCodeSnapshot(templateItem.getIndicatorCode());
            item.setIndicatorNameSnapshot(templateItem.getIndicatorName());
            item.setIndicatorSort(templateItem.getIndicatorSort());
            if (config == null) {
                item.setDefaultDeptNames(templateItem.getDefaultDeptNames());
                item.setScorerCandidateUserIds(templateItem.getDefaultScorerUserIds());
                item.setScorerCandidateUserNames(templateItem.getDefaultScorerUserNames());
                item.setScorerUserId(templateItem.getDefaultScorerUserId());
                item.setScorerUserName(templateItem.getDefaultScorerUserName());
            }
            result.add(item);
        }
        return result;
    }

    private SrmPreliminaryProjectScorerDO buildProjectScorer(Long projectId, TemplateSnapshot template,
                                                             SrmEvaluationTemplateItemDO templateItem,
                                                             SrmPreliminaryProjectScorerConfigReqVO.Item reqItem,
                                                             ScorerResolveContext scorerContext) {
        Long selectedUserId = positiveUserId(reqItem == null ? null : reqItem.getScorerUserId());
        List<Long> requestedCandidateIds = reqItem == null ? List.of() : sanitizeUserIds(reqItem.getScorerCandidateUserIds());
        List<Long> candidateUserIds = CollUtil.isEmpty(requestedCandidateIds)
                ? parseLongCsv(templateItem.getDefaultScorerUserIds()) : requestedCandidateIds;
        if (selectedUserId == null) {
            selectedUserId = positiveUserId(templateItem.getDefaultScorerUserId());
        }
        if (selectedUserId == null && CollUtil.isNotEmpty(candidateUserIds)) {
            selectedUserId = candidateUserIds.get(0);
        }
        candidateUserIds = mergeUserIds(candidateUserIds, List.of(), selectedUserId);
        Long validSelectedUserId = selectedUserId != null && scorerContext.userMap().containsKey(selectedUserId)
                ? selectedUserId : (CollUtil.isEmpty(candidateUserIds) ? null : candidateUserIds.get(0));
        AdminUserRespDTO selectedUser = validSelectedUserId == null ? null : scorerContext.userMap().get(validSelectedUserId);
        String deptName = resolveDeptName(selectedUser, scorerContext.deptMap());

        SrmPreliminaryProjectScorerDO config = new SrmPreliminaryProjectScorerDO();
        config.setProjectId(projectId);
        config.setTemplateId(template.template().getId());
        config.setTemplateVersionId(template.version().getId());
        config.setTemplateItemId(templateItem.getId());
        config.setGroupCodeSnapshot(templateItem.getGroupCode());
        config.setGroupNameSnapshot(templateItem.getGroupName());
        config.setGroupSort(templateItem.getGroupSort());
        config.setIndicatorCodeSnapshot(templateItem.getIndicatorCode());
        config.setIndicatorNameSnapshot(templateItem.getIndicatorName());
        config.setIndicatorSort(templateItem.getIndicatorSort());
        config.setDefaultDeptNames(StrUtil.blankToDefault(deptName, templateItem.getDefaultDeptNames()));
        config.setScorerCandidateUserIds(joinIds(candidateUserIds));
        config.setScorerCandidateUserNames(resolveUserNames(candidateUserIds, scorerContext.userMap()));
        config.setScorerUserId(validSelectedUserId);
        config.setScorerUserName(userName(selectedUser));
        return config;
    }

    private ScorerResolveContext buildScorerResolveContext(
            Map<Long, SrmPreliminaryProjectScorerConfigReqVO.Item> reqItemMap,
            List<SrmEvaluationTemplateItemDO> templateItems) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (SrmEvaluationTemplateItemDO templateItem : templateItems) {
            SrmPreliminaryProjectScorerConfigReqVO.Item reqItem = reqItemMap.get(templateItem.getId());
            if (reqItem == null || CollUtil.isEmpty(reqItem.getScorerCandidateUserIds())) {
                parseLongCsv(templateItem.getDefaultScorerUserIds()).forEach(userIds::add);
                appendUserId(userIds, templateItem.getDefaultScorerUserId());
            } else {
                sanitizeUserIds(reqItem.getScorerCandidateUserIds()).forEach(userIds::add);
                appendUserId(userIds, reqItem.getScorerUserId());
            }
        }
        if (CollUtil.isEmpty(userIds)) {
            return new ScorerResolveContext(Map.of(), Map.of());
        }
        adminUserApi.validateUserList(userIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = userMap.values().stream().map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Map.of() : deptApi.getDeptMap(deptIds);
        return new ScorerResolveContext(userMap, deptMap);
    }

    private TemplateSnapshot validatePublishedTemplate(Long versionId) {
        SrmEvaluationTemplateVersionDO version = versionId == null ? null : versionMapper.selectById(versionId);
        if (version == null) {
            throw exception(SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS);
        }
        if (!STATUS_PUBLISHED.equals(version.getStatus())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        SrmEvaluationTemplateDO template = templateMapper.selectById(version.getTemplateId());
        if (template == null || !STATUS_ENABLED.equals(template.getStatus())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        List<SrmEvaluationTemplateItemDO> items = templateItemMapper.selectListByVersionId(versionId);
        if (CollUtil.isEmpty(items)) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        return new TemplateSnapshot(template, version, items);
    }

    private void copyProjectFields(SrmPreliminaryProjectSaveReqVO reqVO, SrmPreliminaryProjectDO project) {
        project.setProjectCode(StrUtil.trim(reqVO.getProjectCode()));
        project.setProjectName(StrUtil.trim(reqVO.getProjectName()));
        applyTemplateSnapshot(project, reqVO.getCurrentTemplateVersionId());
        String status = StrUtil.blankToDefault(reqVO.getStatus(), STATUS_ENABLED);
        project.setStatus(STATUS_DISABLED.equals(status) ? STATUS_DISABLED : STATUS_ENABLED);
        project.setRemark(StrUtil.trim(reqVO.getRemark()));
    }

    private void applyTemplateSnapshot(SrmPreliminaryProjectDO project, Long templateVersionId) {
        if (templateVersionId == null) {
            project.setCurrentTemplateId(null);
            project.setCurrentTemplateVersionId(null);
            project.setTemplateCodeSnapshot(null);
            project.setTemplateNameSnapshot(null);
            project.setTemplateVersionNoSnapshot(null);
            return;
        }
        applyTemplateSnapshot(project, validatePublishedTemplate(templateVersionId));
    }

    private void applyTemplateSnapshot(SrmPreliminaryProjectDO project, TemplateSnapshot template) {
        project.setCurrentTemplateId(template.template().getId());
        project.setCurrentTemplateVersionId(template.version().getId());
        project.setTemplateCodeSnapshot(template.template().getTemplateCode());
        project.setTemplateNameSnapshot(template.template().getTemplateName());
        project.setTemplateVersionNoSnapshot(template.version().getVersionNo());
    }

    private SrmPreliminaryProjectDO validateProject(Long id) {
        SrmPreliminaryProjectDO project = id == null ? null : projectMapper.selectById(id);
        if (project == null) {
            throw exception(SRM_PRELIMINARY_PROJECT_NOT_EXISTS);
        }
        return project;
    }

    private boolean isEvaluationAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE, EVALUATION_ADMIN_ROLE)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private void assertEvaluationAdmin() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (!isEvaluationAdmin(userId)) {
            throw exception(SRM_EVALUATION_TEMPLATE_ADMIN_REQUIRED);
        }
    }

    private String resolveDeptName(AdminUserRespDTO user, Map<Long, DeptRespDTO> deptMap) {
        if (user == null || user.getDeptId() == null) {
            return null;
        }
        DeptRespDTO dept = deptMap.get(user.getDeptId());
        return dept == null ? null : dept.getName();
    }

    private List<Long> parseLongCsv(String text) {
        List<Long> result = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return result;
        }
        for (String part : text.split("[,，;；\\s]+")) {
            if (StrUtil.isBlank(part)) {
                continue;
            }
            try {
                appendUserId(result, Long.valueOf(part.trim()));
            } catch (NumberFormatException ignored) {
                // 忽略历史脏值
            }
        }
        return result;
    }

    private List<Long> sanitizeUserIds(List<Long> userIds) {
        List<Long> result = new ArrayList<>();
        if (CollUtil.isEmpty(userIds)) {
            return result;
        }
        userIds.forEach(userId -> appendUserId(result, userId));
        return result;
    }

    private List<Long> mergeUserIds(List<Long> originalUserIds, List<Long> appendedUserIds, Long selectedUserId) {
        List<Long> result = new ArrayList<>();
        appendUserIds(result, originalUserIds);
        appendUserIds(result, appendedUserIds);
        appendUserId(result, selectedUserId);
        return result;
    }

    private void appendUserIds(List<Long> target, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        userIds.forEach(userId -> appendUserId(target, userId));
    }

    private void appendUserId(List<Long> target, Long userId) {
        if (positiveUserId(userId) != null && !target.contains(userId)) {
            target.add(userId);
        }
    }

    private void appendUserId(Set<Long> target, Long userId) {
        if (positiveUserId(userId) != null) {
            target.add(userId);
        }
    }

    private Long positiveUserId(Long userId) {
        return userId != null && userId > 0 ? userId : null;
    }

    private String joinIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return null;
        }
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }

    private String joinNames(List<String> names) {
        if (CollUtil.isEmpty(names)) {
            return null;
        }
        return String.join("、", names);
    }

    private String userName(AdminUserRespDTO user) {
        return user == null ? null : StrUtil.blankToDefault(user.getNickname(), user.getUsername());
    }

    private String resolveUserNames(List<Long> userIds, Map<Long, AdminUserRespDTO> userMap) {
        if (CollUtil.isEmpty(userIds)) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Long userId : userIds) {
            AdminUserRespDTO user = userMap.get(userId);
            names.add(user == null ? String.valueOf(userId) : StrUtil.blankToDefault(user.getNickname(), user.getUsername()));
        }
        return joinNames(names);
    }

    private record TemplateSnapshot(SrmEvaluationTemplateDO template,
                                    SrmEvaluationTemplateVersionDO version,
                                    List<SrmEvaluationTemplateItemDO> items) {
    }

    private record ScorerResolveContext(Map<Long, AdminUserRespDTO> userMap,
                                        Map<Long, DeptRespDTO> deptMap) {
    }

}
