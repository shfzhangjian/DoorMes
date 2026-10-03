package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateVersionMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_CODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_ADMIN_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_SCORE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_VERSION_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS;

@Service
@Validated
public class SrmEvaluationTemplateServiceImpl implements SrmEvaluationTemplateService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING_AUDIT = "PENDING_AUDIT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";

    @Resource
    private SrmEvaluationTemplateMapper templateMapper;
    @Resource
    private SrmEvaluationTemplateVersionMapper versionMapper;
    @Resource
    private SrmEvaluationTemplateItemMapper itemMapper;
    @Resource
    private SrmEvaluationTemplateLogMapper logMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(SrmEvaluationTemplateSaveReqVO reqVO) {
        assertEvaluationAdmin();
        if (templateMapper.selectByCode(reqVO.getTemplateCode()) != null) {
            throw exception(SRM_EVALUATION_TEMPLATE_CODE_EXISTS);
        }
        validateItems(reqVO);
        SrmEvaluationTemplateDO template = new SrmEvaluationTemplateDO();
        copyTemplateFields(reqVO, template);
        template.setStatus(StrUtil.blankToDefault(reqVO.getTemplateStatus(), "ENABLED"));
        template.setVersion(0);
        templateMapper.insert(template);

        SrmEvaluationTemplateVersionDO version = buildVersion(reqVO, template.getId());
        versionMapper.insert(version);
        saveItems(version.getId(), reqVO.getItems());
        writeLog(template.getId(), version.getId(), "CREATE", "创建模板草稿 " + version.getVersionNo(), null,
                snapshot(template, version));
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(SrmEvaluationTemplateSaveReqVO reqVO) {
        assertEvaluationAdmin();
        SrmEvaluationTemplateDO template = validateTemplate(reqVO.getId());
        SrmEvaluationTemplateVersionDO version = validateVersion(reqVO.getVersionId());
        if (!Objects.equals(version.getTemplateId(), template.getId())
                || !(STATUS_DRAFT.equals(version.getStatus()) || STATUS_REJECTED.equals(version.getStatus()))) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        SrmEvaluationTemplateDO sameCode = templateMapper.selectByCode(reqVO.getTemplateCode());
        if (sameCode != null && !Objects.equals(sameCode.getId(), template.getId())) {
            throw exception(SRM_EVALUATION_TEMPLATE_CODE_EXISTS);
        }
        validateItems(reqVO);
        String before = snapshot(template, version);
        copyTemplateFields(reqVO, template);
        template.setStatus(StrUtil.blankToDefault(reqVO.getTemplateStatus(), template.getStatus()));
        templateMapper.updateById(template);

        version.setVersionNo(reqVO.getVersionNo());
        version.setStatus(STATUS_DRAFT);
        version.setTotalScore(reqVO.getTotalScore());
        version.setQualificationScore(reqVO.getQualificationScore());
        version.setChangeSummary(reqVO.getChangeSummary());
        version.setRemark(reqVO.getVersionRemark());
        versionMapper.updateById(version);
        itemMapper.deleteByVersionId(version.getId());
        saveItems(version.getId(), reqVO.getItems());
        writeLog(template.getId(), version.getId(), "UPDATE", "更新模板草稿 " + version.getVersionNo(), before,
                snapshot(template, version));
    }

    @Override
    public SrmEvaluationTemplateRespVO getTemplate(Long id, Long versionId) {
        SrmEvaluationTemplateDO template = validateTemplate(id);
        return buildResp(template, versionId, true);
    }

    @Override
    public PageResult<SrmEvaluationTemplateRespVO> getTemplatePage(SrmEvaluationTemplatePageReqVO reqVO) {
        PageResult<SrmEvaluationTemplateDO> page = templateMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(template -> buildResp(template, null, false)).toList(),
                page.getTotal());
    }

    @Override
    public List<SrmEvaluationTemplateRespVO> getPublishedTemplateList(String sceneType) {
        SrmEvaluationTemplatePageReqVO reqVO = new SrmEvaluationTemplatePageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(1000);
        reqVO.setSceneType(sceneType);
        reqVO.setStatus("ENABLED");
        return templateMapper.selectPage(reqVO).getList().stream()
                .filter(template -> template.getCurrentVersionId() != null)
                .map(template -> buildResp(template, template.getCurrentVersionId(), false))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAudit(SrmEvaluationTemplateActionReqVO reqVO) {
        assertEvaluationAdmin();
        SrmEvaluationTemplateVersionDO version = validateVersion(reqVO.getVersionId());
        if (!(STATUS_DRAFT.equals(version.getStatus()) || STATUS_REJECTED.equals(version.getStatus()))) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        if (CollUtil.isEmpty(itemMapper.selectListByVersionId(version.getId()))) {
            throw exception(SRM_EVALUATION_TEMPLATE_SCORE_INVALID, "至少需要一条评估指标");
        }
        UserSnapshot user = currentUser();
        version.setStatus(STATUS_PENDING_AUDIT);
        version.setSubmitterId(user.id());
        version.setSubmitterName(user.name());
        version.setSubmitTime(LocalDateTime.now());
        versionMapper.updateById(version);
        writeLog(version.getTemplateId(), version.getId(), "SUBMIT", "提交版本审核", null, reqVO.getOpinion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(SrmEvaluationTemplateActionReqVO reqVO) {
        assertEvaluationAdmin();
        SrmEvaluationTemplateVersionDO version = validateVersion(reqVO.getVersionId());
        if (!STATUS_PENDING_AUDIT.equals(version.getStatus()) || reqVO.getApproved() == null) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        UserSnapshot user = currentUser();
        version.setStatus(Boolean.TRUE.equals(reqVO.getApproved()) ? STATUS_APPROVED : STATUS_REJECTED);
        version.setAuditorId(user.id());
        version.setAuditorName(user.name());
        version.setAuditOpinion(reqVO.getOpinion());
        version.setAuditTime(LocalDateTime.now());
        versionMapper.updateById(version);
        writeLog(version.getTemplateId(), version.getId(), "AUDIT",
                Boolean.TRUE.equals(reqVO.getApproved()) ? "审核通过" : "审核驳回", null, reqVO.getOpinion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(SrmEvaluationTemplateActionReqVO reqVO) {
        assertEvaluationAdmin();
        SrmEvaluationTemplateVersionDO version = validateVersion(reqVO.getVersionId());
        if (!STATUS_APPROVED.equals(version.getStatus())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        SrmEvaluationTemplateDO template = validateTemplate(version.getTemplateId());
        UserSnapshot user = currentUser();
        version.setStatus(STATUS_PUBLISHED);
        version.setPublisherId(user.id());
        version.setPublisherName(user.name());
        version.setPublishTime(LocalDateTime.now());
        versionMapper.updateById(version);
        template.setCurrentVersionId(version.getId());
        template.setCurrentVersionNo(version.getVersionNo());
        template.setStatus("ENABLED");
        templateMapper.updateById(template);
        writeLog(template.getId(), version.getId(), "PUBLISH", "发布模板版本 " + version.getVersionNo(), null,
                reqVO.getOpinion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upgrade(SrmEvaluationTemplateActionReqVO reqVO) {
        assertEvaluationAdmin();
        SrmEvaluationTemplateVersionDO source = validateVersion(reqVO.getVersionId());
        if (!STATUS_PUBLISHED.equals(source.getStatus())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        String nextVersionNo = nextVersionNo(source.getTemplateId());
        if (versionMapper.selectByTemplateIdAndNo(source.getTemplateId(), nextVersionNo) != null) {
            throw exception(SRM_EVALUATION_TEMPLATE_VERSION_EXISTS);
        }
        SrmEvaluationTemplateVersionDO target = BeanUtils.toBean(source, SrmEvaluationTemplateVersionDO.class);
        target.setId(null);
        target.setVersionNo(nextVersionNo);
        target.setStatus(STATUS_DRAFT);
        target.setPreviousVersionId(source.getId());
        target.setChangeSummary(StrUtil.blankToDefault(reqVO.getChangeSummary(), "基于" + source.getVersionNo() + "升级"));
        target.setSubmitterId(null);
        target.setSubmitterName(null);
        target.setSubmitTime(null);
        target.setAuditorId(null);
        target.setAuditorName(null);
        target.setAuditOpinion(null);
        target.setAuditTime(null);
        target.setPublisherId(null);
        target.setPublisherName(null);
        target.setPublishTime(null);
        target.setVersion(0);
        versionMapper.insert(target);
        for (SrmEvaluationTemplateItemDO sourceItem : itemMapper.selectListByVersionId(source.getId())) {
            SrmEvaluationTemplateItemDO targetItem = BeanUtils.toBean(sourceItem, SrmEvaluationTemplateItemDO.class);
            targetItem.setId(null);
            targetItem.setVersionId(target.getId());
            itemMapper.insert(targetItem);
        }
        writeLog(source.getTemplateId(), target.getId(), "UPGRADE",
                "由 " + source.getVersionNo() + " 升级生成 " + nextVersionNo, null, reqVO.getChangeSummary());
        return target.getId();
    }

    private SrmEvaluationTemplateRespVO buildResp(SrmEvaluationTemplateDO template, Long versionId, boolean includeHistory) {
        SrmEvaluationTemplateRespVO resp = BeanUtils.toBean(template, SrmEvaluationTemplateRespVO.class);
        List<SrmEvaluationTemplateVersionDO> versions = versionMapper.selectListByTemplateId(template.getId());
        Long selectedVersionId = versionId != null ? versionId
                : (versions.isEmpty() ? null : versions.get(0).getId());
        if (selectedVersionId != null) {
            SrmEvaluationTemplateVersionDO selected = versions.stream()
                    .filter(item -> Objects.equals(item.getId(), selectedVersionId)).findFirst()
                    .orElseGet(() -> validateVersion(selectedVersionId));
            resp.setCurrentVersion(buildVersionResp(selected, true));
        }
        if (includeHistory) {
            resp.setVersions(versions.stream().map(version -> buildVersionResp(version, false)).toList());
            resp.setLogs(BeanUtils.toBean(logMapper.selectListByTemplateId(template.getId()),
                    SrmEvaluationTemplateRespVO.Log.class));
        }
        return resp;
    }

    private SrmEvaluationTemplateRespVO.Version buildVersionResp(SrmEvaluationTemplateVersionDO version,
                                                                  boolean includeItems) {
        SrmEvaluationTemplateRespVO.Version resp = BeanUtils.toBean(version, SrmEvaluationTemplateRespVO.Version.class);
        if (includeItems) {
            resp.setItems(BeanUtils.toBean(itemMapper.selectListByVersionId(version.getId()),
                    SrmEvaluationTemplateRespVO.Item.class));
        }
        return resp;
    }

    private void validateItems(SrmEvaluationTemplateSaveReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(SRM_EVALUATION_TEMPLATE_SCORE_INVALID, "指标不能为空");
        }
        BigDecimal itemTotal = reqVO.getItems().stream().map(SrmEvaluationTemplateSaveReqVO.Item::getMaxScore)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (itemTotal.compareTo(reqVO.getTotalScore()) != 0) {
            throw exception(SRM_EVALUATION_TEMPLATE_SCORE_INVALID,
                    "指标满分合计" + itemTotal.stripTrailingZeros().toPlainString()
                            + "与模板总分" + reqVO.getTotalScore().stripTrailingZeros().toPlainString() + "不一致");
        }
        Map<String, List<SrmEvaluationTemplateSaveReqVO.Item>> groups = new LinkedHashMap<>();
        for (SrmEvaluationTemplateSaveReqVO.Item item : reqVO.getItems()) {
            if (item.getMaxScore() == null || item.getMaxScore().compareTo(BigDecimal.ZERO) < 0) {
                throw exception(SRM_EVALUATION_TEMPLATE_SCORE_INVALID, item.getIndicatorName() + "满分无效");
            }
            groups.computeIfAbsent(item.getGroupCode(), ignored -> new ArrayList<>()).add(item);
        }
        for (List<SrmEvaluationTemplateSaveReqVO.Item> groupItems : groups.values()) {
            SrmEvaluationTemplateSaveReqVO.Item first = groupItems.get(0);
            BigDecimal groupItemTotal = groupItems.stream().map(SrmEvaluationTemplateSaveReqVO.Item::getMaxScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (first.getGroupMaxScore() == null || groupItemTotal.compareTo(first.getGroupMaxScore()) != 0) {
                throw exception(SRM_EVALUATION_TEMPLATE_SCORE_INVALID,
                        first.getGroupName() + "指标合计与维度满分不一致");
            }
        }
        if (reqVO.getQualificationScore().compareTo(reqVO.getTotalScore()) > 0) {
            throw exception(SRM_EVALUATION_TEMPLATE_SCORE_INVALID, "合格线不能高于模板总分");
        }
    }

    private void copyTemplateFields(SrmEvaluationTemplateSaveReqVO reqVO, SrmEvaluationTemplateDO template) {
        template.setTemplateCode(StrUtil.trim(reqVO.getTemplateCode()));
        template.setTemplateName(StrUtil.trim(reqVO.getTemplateName()));
        template.setSceneType(reqVO.getSceneType());
        template.setMaterialType(reqVO.getMaterialType());
        template.setRemark(reqVO.getTemplateRemark());
    }

    private SrmEvaluationTemplateVersionDO buildVersion(SrmEvaluationTemplateSaveReqVO reqVO, Long templateId) {
        if (versionMapper.selectByTemplateIdAndNo(templateId, reqVO.getVersionNo()) != null) {
            throw exception(SRM_EVALUATION_TEMPLATE_VERSION_EXISTS);
        }
        SrmEvaluationTemplateVersionDO version = new SrmEvaluationTemplateVersionDO();
        version.setTemplateId(templateId);
        version.setVersionNo(reqVO.getVersionNo());
        version.setStatus(STATUS_DRAFT);
        version.setTotalScore(reqVO.getTotalScore());
        version.setQualificationScore(reqVO.getQualificationScore());
        version.setChangeSummary(reqVO.getChangeSummary());
        version.setRemark(reqVO.getVersionRemark());
        version.setVersion(0);
        return version;
    }

    private void saveItems(Long versionId, List<SrmEvaluationTemplateSaveReqVO.Item> items) {
        for (SrmEvaluationTemplateSaveReqVO.Item reqItem : items) {
            SrmEvaluationTemplateItemDO item = BeanUtils.toBean(reqItem, SrmEvaluationTemplateItemDO.class);
            item.setId(null);
            item.setVersionId(versionId);
            item.setVetoResult(StrUtil.blankToDefault(item.getVetoResult(), "UNQUALIFIED"));
            item.setAttachmentRequired(Boolean.TRUE.equals(item.getAttachmentRequired()));
            normalizeDefaultScorers(item);
            itemMapper.insert(item);
        }
    }

    private void normalizeDefaultScorers(SrmEvaluationTemplateItemDO item) {
        List<Long> scorerIds = parseLongCsv(item.getDefaultScorerUserIds());
        if (item.getDefaultScorerUserId() != null && !scorerIds.contains(item.getDefaultScorerUserId())) {
            scorerIds.add(0, item.getDefaultScorerUserId());
        }
        List<String> scorerNames = splitNameList(item.getDefaultScorerUserNames());
        if (StrUtil.isNotBlank(item.getDefaultScorerUserName())
                && !scorerNames.contains(item.getDefaultScorerUserName())) {
            scorerNames.add(0, item.getDefaultScorerUserName());
        }
        item.setDefaultScorerUserIds(joinIds(scorerIds));
        item.setDefaultScorerUserNames(joinNames(scorerNames));
        if (item.getDefaultScorerUserId() == null && CollUtil.isNotEmpty(scorerIds)) {
            item.setDefaultScorerUserId(scorerIds.get(0));
        }
        if (StrUtil.isBlank(item.getDefaultScorerUserName()) && CollUtil.isNotEmpty(scorerNames)) {
            item.setDefaultScorerUserName(scorerNames.get(0));
        }
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
            Long value = Long.valueOf(part.trim());
            if (!result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private List<String> splitNameList(String text) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isBlank(text)) {
            return result;
        }
        for (String part : text.split("[,，;；、\\n\\r]+")) {
            String value = StrUtil.trim(part);
            if (StrUtil.isNotBlank(value) && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
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

    private SrmEvaluationTemplateDO validateTemplate(Long id) {
        SrmEvaluationTemplateDO template = id == null ? null : templateMapper.selectById(id);
        if (template == null) {
            throw exception(SRM_EVALUATION_TEMPLATE_NOT_EXISTS);
        }
        return template;
    }

    private SrmEvaluationTemplateVersionDO validateVersion(Long id) {
        SrmEvaluationTemplateVersionDO version = id == null ? null : versionMapper.selectById(id);
        if (version == null) {
            throw exception(SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS);
        }
        return version;
    }

    private String nextVersionNo(Long templateId) {
        int max = versionMapper.selectListByTemplateId(templateId).stream()
                .map(SrmEvaluationTemplateVersionDO::getVersionNo)
                .filter(Objects::nonNull)
                .map(value -> value.replaceAll("\\D", ""))
                .filter(StrUtil::isNotBlank)
                .mapToInt(Integer::parseInt)
                .max().orElse(0);
        return "V" + (max + 1);
    }

    private String snapshot(SrmEvaluationTemplateDO template, SrmEvaluationTemplateVersionDO version) {
        return JsonUtils.toJsonString(Map.of("template", template, "version", version,
                "items", itemMapper.selectListByVersionId(version.getId())));
    }

    private void writeLog(Long templateId, Long versionId, String action, String description,
                          String beforeJson, String afterJson) {
        UserSnapshot user = currentUser();
        SrmEvaluationTemplateLogDO log = new SrmEvaluationTemplateLogDO();
        log.setTemplateId(templateId);
        log.setVersionId(versionId);
        log.setAction(action);
        log.setActionDescription(description);
        log.setOperatorId(user.id());
        log.setOperatorName(user.name());
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        logMapper.insert(log);
    }

    private UserSnapshot currentUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        if (userId != null && StrUtil.isBlank(userName)) {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            userName = user == null ? null : user.getNickname();
        }
        return new UserSnapshot(userId, StrUtil.blankToDefault(userName, "系统用户"));
    }

    private void assertEvaluationAdmin() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (!isEvaluationAdmin(userId)) {
            throw exception(SRM_EVALUATION_TEMPLATE_ADMIN_REQUIRED);
        }
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

    private record UserSnapshot(Long id, String name) {
    }

}
