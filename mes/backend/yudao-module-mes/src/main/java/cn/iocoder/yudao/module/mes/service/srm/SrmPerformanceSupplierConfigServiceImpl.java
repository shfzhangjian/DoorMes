package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceSupplierConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceMetricCalcNodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceMetricCalcRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterEvalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceSupplierConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceSupplierConfigItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceMetricCalcNodeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceMetricCalcRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceQuarterEvalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceSupplierConfigItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceSupplierConfigMapper;
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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_SUPPLIER_CONFIG_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_SUPPLIER_CONFIG_IN_USE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_SUPPLIER_CONFIG_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_SUPPLIER_CONFIG_NO_EXISTS;

@Service
@Validated
public class SrmPerformanceSupplierConfigServiceImpl implements SrmPerformanceSupplierConfigService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String SCENE_QUARTER = "QUARTER";
    private static final String INDICATOR_MANUAL = "MANUAL_SCORE";
    private static final String INDICATOR_CALCULATED = "CALCULATED_SCORE";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";

    @Resource
    private SrmPerformanceSupplierConfigMapper supplierConfigMapper;
    @Resource
    private SrmPerformanceSupplierConfigItemMapper configItemMapper;
    @Resource
    private SrmPerformanceMetricCalcRuleMapper calcRuleMapper;
    @Resource
    private SrmPerformanceMetricCalcNodeMapper calcNodeMapper;
    @Resource
    private SrmPerformanceQuarterEvalMapper quarterEvalMapper;
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
    public Long createConfig(SrmPerformanceSupplierConfigSaveReqVO reqVO) {
        assertEvaluationAdmin();
        String configNo = StrUtil.blankToDefault(StrUtil.trim(reqVO.getConfigNo()), buildConfigNo());
        if (supplierConfigMapper.selectByConfigNo(configNo) != null) {
            throw exception(SRM_PERFORMANCE_SUPPLIER_CONFIG_NO_EXISTS);
        }
        TemplateSnapshot template = validatePublishedQuarterTemplate(reqVO.getCurrentTemplateVersionId());
        if (supplierConfigMapper.selectBySupplierAndTemplateVersion(reqVO.getSupplierId(), template.version().getId()) != null) {
            throw exception(SRM_PERFORMANCE_SUPPLIER_CONFIG_DUPLICATE);
        }
        SrmPerformanceSupplierConfigDO config = new SrmPerformanceSupplierConfigDO();
        copyConfigFields(reqVO, config, configNo);
        applyTemplateSnapshot(config, template);
        config.setVersion(0);
        supplierConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(SrmPerformanceSupplierConfigSaveReqVO reqVO) {
        assertEvaluationAdmin();
        SrmPerformanceSupplierConfigDO config = validateConfig(reqVO.getId());
        String configNo = StrUtil.blankToDefault(StrUtil.trim(reqVO.getConfigNo()), config.getConfigNo());
        SrmPerformanceSupplierConfigDO sameNo = supplierConfigMapper.selectByConfigNo(configNo);
        if (sameNo != null && !Objects.equals(sameNo.getId(), config.getId())) {
            throw exception(SRM_PERFORMANCE_SUPPLIER_CONFIG_NO_EXISTS);
        }
        TemplateSnapshot template = validatePublishedQuarterTemplate(reqVO.getCurrentTemplateVersionId());
        SrmPerformanceSupplierConfigDO sameConfig = supplierConfigMapper
                .selectBySupplierAndTemplateVersion(reqVO.getSupplierId(), template.version().getId());
        if (sameConfig != null && !Objects.equals(sameConfig.getId(), config.getId())) {
            throw exception(SRM_PERFORMANCE_SUPPLIER_CONFIG_DUPLICATE);
        }
        copyConfigFields(reqVO, config, configNo);
        applyTemplateSnapshot(config, template);
        supplierConfigMapper.updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        assertEvaluationAdmin();
        SrmPerformanceSupplierConfigDO config = validateConfig(id);
        Long usedCount = quarterEvalMapper.selectCount(new LambdaQueryWrapperX<SrmPerformanceQuarterEvalDO>()
                .eq(SrmPerformanceQuarterEvalDO::getSupplierId, config.getSupplierId())
                .eq(SrmPerformanceQuarterEvalDO::getTemplateVersionId, config.getCurrentTemplateVersionId()));
        if (usedCount != null && usedCount > 0) {
            throw exception(SRM_PERFORMANCE_SUPPLIER_CONFIG_IN_USE);
        }
        deleteCalcRulesByConfig(config.getId());
        configItemMapper.delete(new LambdaQueryWrapperX<SrmPerformanceSupplierConfigItemDO>()
                .eq(SrmPerformanceSupplierConfigItemDO::getConfigId, config.getId()));
        supplierConfigMapper.deleteById(config.getId());
    }

    @Override
    public SrmPerformanceSupplierConfigRespVO getConfig(Long id) {
        SrmPerformanceSupplierConfigDO config = validateConfig(id);
        SrmPerformanceSupplierConfigRespVO resp = BeanUtils.toBean(config, SrmPerformanceSupplierConfigRespVO.class);
        if (config.getCurrentTemplateVersionId() != null) {
            TemplateSnapshot template = validatePublishedQuarterTemplate(config.getCurrentTemplateVersionId());
            resp.setItems(buildConfigItems(config, template, configItemMapper
                    .selectListByConfigAndTemplateVersion(config.getId(), config.getCurrentTemplateVersionId())));
        }
        return resp;
    }

    @Override
    public PageResult<SrmPerformanceSupplierConfigRespVO> getConfigPage(SrmPerformanceSupplierConfigPageReqVO reqVO) {
        PageResult<SrmPerformanceSupplierConfigDO> page = supplierConfigMapper.selectPage(reqVO);
        return new PageResult<>(BeanUtils.toBean(page.getList(), SrmPerformanceSupplierConfigRespVO.class),
                page.getTotal());
    }

    @Override
    public List<SrmPerformanceSupplierConfigRespVO> getEnabledConfigList() {
        return BeanUtils.toBean(supplierConfigMapper.selectEnabledList(), SrmPerformanceSupplierConfigRespVO.class);
    }

    @Override
    public SrmPerformanceSupplierConfigRespVO getItemConfig(Long configId, Long templateVersionId) {
        SrmPerformanceSupplierConfigDO config = validateConfig(configId);
        TemplateSnapshot template = validatePublishedQuarterTemplate(templateVersionId);
        SrmPerformanceSupplierConfigRespVO resp = BeanUtils.toBean(config, SrmPerformanceSupplierConfigRespVO.class);
        resp.setItems(buildConfigItems(config, template,
                configItemMapper.selectListByConfigAndTemplateVersion(configId, templateVersionId)));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveItemConfig(SrmPerformanceSupplierConfigReqVO reqVO) {
        assertEvaluationAdmin();
        SrmPerformanceSupplierConfigDO config = validateConfig(reqVO.getConfigId());
        TemplateSnapshot template = validatePublishedQuarterTemplate(reqVO.getTemplateVersionId());
        applyTemplateSnapshot(config, template);
        supplierConfigMapper.updateById(config);

        Map<Long, SrmPerformanceSupplierConfigReqVO.Item> reqItemMap = reqVO.getItems().stream()
                .filter(item -> item.getTemplateItemId() != null)
                .collect(Collectors.toMap(SrmPerformanceSupplierConfigReqVO.Item::getTemplateItemId,
                        Function.identity(), (left, right) -> right));
        UserResolveContext userContext = buildUserResolveContext(reqItemMap, template.items());

        deleteCalcRulesByConfig(config.getId());
        configItemMapper.deleteByConfigAndTemplateVersion(config.getId(), template.version().getId());
        for (SrmEvaluationTemplateItemDO templateItem : template.items()) {
            SrmPerformanceSupplierConfigItemDO item = buildConfigItem(config.getId(), template, templateItem,
                    reqItemMap.get(templateItem.getId()), userContext);
            configItemMapper.insert(item);
            SrmPerformanceSupplierConfigReqVO.Item reqItem = reqItemMap.get(templateItem.getId());
            SrmPerformanceSupplierConfigReqVO.CalcRule reqRule = reqItem == null ? null : reqItem.getCalcRule();
            if (reqRule != null || INDICATOR_CALCULATED.equals(item.getIndicatorType())) {
                Long ruleId = saveCalcRule(config.getId(), item, templateItem, reqRule);
                item.setCalcRuleId(ruleId);
                configItemMapper.updateById(item);
            }
        }
    }

    private List<SrmPerformanceSupplierConfigRespVO.Item> buildConfigItems(
            SrmPerformanceSupplierConfigDO config, TemplateSnapshot template,
            List<SrmPerformanceSupplierConfigItemDO> savedItems) {
        Map<Long, SrmPerformanceSupplierConfigItemDO> itemMap = savedItems.stream()
                .collect(Collectors.toMap(SrmPerformanceSupplierConfigItemDO::getTemplateItemId,
                        Function.identity(), (left, right) -> right));
        Map<Long, SrmPerformanceMetricCalcRuleDO> ruleMap = calcRuleMapper.selectListByConfigId(config.getId()).stream()
                .collect(Collectors.toMap(SrmPerformanceMetricCalcRuleDO::getConfigItemId,
                        Function.identity(), (left, right) -> left));
        List<SrmPerformanceSupplierConfigRespVO.Item> result = new ArrayList<>();
        for (SrmEvaluationTemplateItemDO templateItem : template.items()) {
            SrmPerformanceSupplierConfigItemDO saved = itemMap.get(templateItem.getId());
            SrmPerformanceSupplierConfigRespVO.Item item = saved == null
                    ? buildDefaultRespItem(config, template, templateItem)
                    : BeanUtils.toBean(saved, SrmPerformanceSupplierConfigRespVO.Item.class);
            SrmPerformanceMetricCalcRuleDO rule = ruleMap.get(item.getId());
            if (rule != null) {
                item.setCalcRule(toCalcRuleResp(rule));
                item.setCalcRuleId(rule.getId());
            }
            result.add(item);
        }
        return result;
    }

    private SrmPerformanceSupplierConfigRespVO.Item buildDefaultRespItem(
            SrmPerformanceSupplierConfigDO config, TemplateSnapshot template, SrmEvaluationTemplateItemDO templateItem) {
        SrmPerformanceSupplierConfigRespVO.Item item = new SrmPerformanceSupplierConfigRespVO.Item();
        item.setConfigId(config.getId());
        item.setTemplateId(template.template().getId());
        item.setTemplateVersionId(template.version().getId());
        item.setTemplateItemId(templateItem.getId());
        item.setGroupCodeSnapshot(templateItem.getGroupCode());
        item.setGroupNameSnapshot(templateItem.getGroupName());
        item.setGroupSort(templateItem.getGroupSort());
        item.setGroupMaxScoreSnapshot(templateItem.getGroupMaxScore());
        item.setIndicatorCodeSnapshot(templateItem.getIndicatorCode());
        item.setIndicatorNameSnapshot(templateItem.getIndicatorName());
        item.setIndicatorSort(templateItem.getIndicatorSort());
        item.setScoringRuleSnapshot(templateItem.getScoringRule());
        item.setMaxScoreSnapshot(templateItem.getMaxScore());
        item.setDefaultDeptNames(templateItem.getDefaultDeptNames());
        item.setIndicatorType(defaultIndicatorType(templateItem));
        item.setScorerCandidateUserIds(templateItem.getDefaultScorerUserIds());
        item.setScorerCandidateUserNames(templateItem.getDefaultScorerUserNames());
        item.setScorerUserId(templateItem.getDefaultScorerUserId());
        item.setScorerUserName(templateItem.getDefaultScorerUserName());
        return item;
    }

    private SrmPerformanceSupplierConfigItemDO buildConfigItem(
            Long configId, TemplateSnapshot template, SrmEvaluationTemplateItemDO templateItem,
            SrmPerformanceSupplierConfigReqVO.Item reqItem, UserResolveContext userContext) {
        SrmPerformanceSupplierConfigItemDO item = new SrmPerformanceSupplierConfigItemDO();
        item.setConfigId(configId);
        item.setTemplateId(template.template().getId());
        item.setTemplateVersionId(template.version().getId());
        item.setTemplateItemId(templateItem.getId());
        item.setGroupCodeSnapshot(templateItem.getGroupCode());
        item.setGroupNameSnapshot(templateItem.getGroupName());
        item.setGroupSort(templateItem.getGroupSort());
        item.setGroupMaxScoreSnapshot(templateItem.getGroupMaxScore());
        item.setIndicatorCodeSnapshot(templateItem.getIndicatorCode());
        item.setIndicatorNameSnapshot(templateItem.getIndicatorName());
        item.setIndicatorSort(templateItem.getIndicatorSort());
        item.setScoringRuleSnapshot(templateItem.getScoringRule());
        item.setMaxScoreSnapshot(templateItem.getMaxScore());

        String indicatorType = StrUtil.blankToDefault(reqItem == null ? null : reqItem.getIndicatorType(),
                defaultIndicatorType(templateItem));
        item.setIndicatorType(indicatorType);
        item.setTargetValue(reqItem == null ? null : reqItem.getTargetValue());
        item.setTargetUnit(StrUtil.trim(reqItem == null ? null : reqItem.getTargetUnit()));
        item.setRedlineScore(reqItem == null ? null : reqItem.getRedlineScore());
        item.setRemark(StrUtil.trim(reqItem == null ? null : reqItem.getRemark()));

        UserAssignment scorer = resolveAssignment(reqItem == null ? null : reqItem.getScorerUserId(),
                reqItem == null ? List.of() : sanitizeUserIds(reqItem.getScorerCandidateUserIds()),
                templateItem.getDefaultScorerUserId(), parseLongCsv(templateItem.getDefaultScorerUserIds()),
                userContext);
        UserAssignment reporter = resolveAssignment(reqItem == null ? null : reqItem.getReporterUserId(),
                reqItem == null ? List.of() : sanitizeUserIds(reqItem.getReporterCandidateUserIds()),
                null, List.of(), userContext);
        item.setDefaultDeptNames(StrUtil.blankToDefault(scorer.deptName(), templateItem.getDefaultDeptNames()));
        item.setScorerCandidateUserIds(joinIds(scorer.candidateUserIds()));
        item.setScorerCandidateUserNames(resolveUserNames(scorer.candidateUserIds(), userContext.userMap()));
        item.setScorerUserId(scorer.selectedUserId());
        item.setScorerUserName(userName(userContext.userMap().get(scorer.selectedUserId())));
        item.setReporterCandidateUserIds(joinIds(reporter.candidateUserIds()));
        item.setReporterCandidateUserNames(resolveUserNames(reporter.candidateUserIds(), userContext.userMap()));
        item.setReporterUserId(reporter.selectedUserId());
        item.setReporterUserName(userName(userContext.userMap().get(reporter.selectedUserId())));
        return item;
    }

    private Long saveCalcRule(Long configId, SrmPerformanceSupplierConfigItemDO item,
                              SrmEvaluationTemplateItemDO templateItem,
                              SrmPerformanceSupplierConfigReqVO.CalcRule reqRule) {
        SrmPerformanceMetricCalcRuleDO rule = new SrmPerformanceMetricCalcRuleDO();
        rule.setConfigId(configId);
        rule.setConfigItemId(item.getId());
        rule.setTemplateItemId(templateItem.getId());
        rule.setRuleCode(StrUtil.blankToDefault(StrUtil.trim(reqRule == null ? null : reqRule.getRuleCode()),
                item.getIndicatorCodeSnapshot()));
        rule.setRuleName(StrUtil.blankToDefault(StrUtil.trim(reqRule == null ? null : reqRule.getRuleName()),
                item.getIndicatorNameSnapshot()));
        rule.setFormulaExpr(StrUtil.trim(reqRule == null ? null : reqRule.getFormulaExpr()));
        rule.setScoreFormulaExpr(StrUtil.trim(reqRule == null ? null : reqRule.getScoreFormulaExpr()));
        rule.setPeriodScope(StrUtil.blankToDefault(reqRule == null ? null : reqRule.getPeriodScope(), "QUARTER_MONTHS"));
        rule.setAggregateMethod(StrUtil.blankToDefault(reqRule == null ? null : reqRule.getAggregateMethod(), "SUM"));
        rule.setMissingPolicy(StrUtil.blankToDefault(reqRule == null ? null : reqRule.getMissingPolicy(), "BLOCK"));
        rule.setEnabled(reqRule == null || reqRule.getEnabled() == null || Boolean.TRUE.equals(reqRule.getEnabled()));
        rule.setRemark(StrUtil.trim(reqRule == null ? null : reqRule.getRemark()));
        calcRuleMapper.insert(rule);

        List<SrmPerformanceSupplierConfigReqVO.CalcNode> reqNodes = reqRule == null ? List.of() : reqRule.getNodes();
        if (CollUtil.isEmpty(reqNodes)) {
            SrmPerformanceMetricCalcNodeDO node = new SrmPerformanceMetricCalcNodeDO();
            node.setRuleId(rule.getId());
            node.setNodeKey(item.getIndicatorCodeSnapshot());
            node.setNodeName(item.getIndicatorNameSnapshot());
            node.setNodeType("SOURCE_METRIC");
            node.setSourceMetricCode(item.getIndicatorCodeSnapshot());
            node.setSourceMetricName(item.getIndicatorNameSnapshot());
            node.setAggregateMethod("SUM");
            node.setUnit(item.getTargetUnit());
            node.setSortNo(1);
            node.setRequiredFlag(Boolean.TRUE);
            calcNodeMapper.insert(node);
        } else {
            for (int i = 0; i < reqNodes.size(); i++) {
                SrmPerformanceSupplierConfigReqVO.CalcNode reqNode = reqNodes.get(i);
                SrmPerformanceMetricCalcNodeDO node = BeanUtils.toBean(reqNode, SrmPerformanceMetricCalcNodeDO.class);
                node.setId(null);
                node.setRuleId(rule.getId());
                node.setNodeKey(StrUtil.blankToDefault(StrUtil.trim(node.getNodeKey()), "node" + (i + 1)));
                node.setNodeName(StrUtil.blankToDefault(StrUtil.trim(node.getNodeName()), node.getNodeKey()));
                node.setNodeType(StrUtil.blankToDefault(node.getNodeType(), "SOURCE_METRIC"));
                node.setAggregateMethod(StrUtil.blankToDefault(node.getAggregateMethod(), "SUM"));
                node.setSortNo(node.getSortNo() == null ? i + 1 : node.getSortNo());
                node.setRequiredFlag(node.getRequiredFlag() == null || Boolean.TRUE.equals(node.getRequiredFlag()));
                calcNodeMapper.insert(node);
            }
        }
        return rule.getId();
    }

    private SrmPerformanceSupplierConfigRespVO.CalcRule toCalcRuleResp(SrmPerformanceMetricCalcRuleDO rule) {
        SrmPerformanceSupplierConfigRespVO.CalcRule resp =
                BeanUtils.toBean(rule, SrmPerformanceSupplierConfigRespVO.CalcRule.class);
        resp.setNodes(BeanUtils.toBean(calcNodeMapper.selectListByRuleId(rule.getId()),
                SrmPerformanceSupplierConfigRespVO.CalcNode.class));
        return resp;
    }

    private void deleteCalcRulesByConfig(Long configId) {
        List<SrmPerformanceMetricCalcRuleDO> rules = calcRuleMapper.selectListByConfigId(configId);
        for (SrmPerformanceMetricCalcRuleDO rule : rules) {
            calcNodeMapper.deleteByRuleId(rule.getId());
        }
        calcRuleMapper.deleteByConfigId(configId);
    }

    private TemplateSnapshot validatePublishedQuarterTemplate(Long versionId) {
        SrmEvaluationTemplateVersionDO version = versionId == null ? null : versionMapper.selectById(versionId);
        if (version == null) {
            throw exception(SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS);
        }
        if (!STATUS_PUBLISHED.equals(version.getStatus())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        SrmEvaluationTemplateDO template = templateMapper.selectById(version.getTemplateId());
        if (template == null || !STATUS_ENABLED.equals(template.getStatus())
                || !SCENE_QUARTER.equals(template.getSceneType())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        List<SrmEvaluationTemplateItemDO> items = templateItemMapper.selectListByVersionId(versionId);
        if (CollUtil.isEmpty(items)) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        return new TemplateSnapshot(template, version, items);
    }

    private void copyConfigFields(SrmPerformanceSupplierConfigSaveReqVO reqVO,
                                  SrmPerformanceSupplierConfigDO config,
                                  String configNo) {
        config.setConfigNo(configNo);
        config.setSupplierId(reqVO.getSupplierId());
        config.setSupplierCode(StrUtil.trim(reqVO.getSupplierCode()));
        config.setSupplierName(StrUtil.trim(reqVO.getSupplierName()));
        config.setSupplierSourceType(StrUtil.blankToDefault(reqVO.getSupplierSourceType(), "REGISTERED"));
        String status = StrUtil.blankToDefault(reqVO.getStatus(), STATUS_ENABLED);
        config.setStatus(STATUS_DISABLED.equals(status) ? STATUS_DISABLED : STATUS_ENABLED);
        config.setRemark(StrUtil.trim(reqVO.getRemark()));
    }

    private void applyTemplateSnapshot(SrmPerformanceSupplierConfigDO config, TemplateSnapshot template) {
        config.setCurrentTemplateId(template.template().getId());
        config.setCurrentTemplateVersionId(template.version().getId());
        config.setTemplateCodeSnapshot(template.template().getTemplateCode());
        config.setTemplateNameSnapshot(template.template().getTemplateName());
        config.setTemplateVersionNoSnapshot(template.version().getVersionNo());
    }

    private SrmPerformanceSupplierConfigDO validateConfig(Long id) {
        SrmPerformanceSupplierConfigDO config = id == null ? null : supplierConfigMapper.selectById(id);
        if (config == null) {
            throw exception(SRM_PERFORMANCE_SUPPLIER_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private UserResolveContext buildUserResolveContext(
            Map<Long, SrmPerformanceSupplierConfigReqVO.Item> reqItemMap,
            List<SrmEvaluationTemplateItemDO> templateItems) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (SrmEvaluationTemplateItemDO templateItem : templateItems) {
            parseLongCsv(templateItem.getDefaultScorerUserIds()).forEach(userIds::add);
            appendUserId(userIds, templateItem.getDefaultScorerUserId());
            SrmPerformanceSupplierConfigReqVO.Item reqItem = reqItemMap.get(templateItem.getId());
            if (reqItem == null) {
                continue;
            }
            sanitizeUserIds(reqItem.getScorerCandidateUserIds()).forEach(userIds::add);
            sanitizeUserIds(reqItem.getReporterCandidateUserIds()).forEach(userIds::add);
            appendUserId(userIds, reqItem.getScorerUserId());
            appendUserId(userIds, reqItem.getReporterUserId());
        }
        if (CollUtil.isEmpty(userIds)) {
            return new UserResolveContext(Map.of(), Map.of());
        }
        adminUserApi.validateUserList(userIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = userMap.values().stream().map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Map.of() : deptApi.getDeptMap(deptIds);
        return new UserResolveContext(userMap, deptMap);
    }

    private UserAssignment resolveAssignment(Long requestedSelectedUserId, List<Long> requestedCandidateUserIds,
                                             Long defaultSelectedUserId, List<Long> defaultCandidateUserIds,
                                             UserResolveContext userContext) {
        Long selectedUserId = positiveUserId(requestedSelectedUserId);
        List<Long> candidateUserIds = CollUtil.isEmpty(requestedCandidateUserIds)
                ? new ArrayList<>(defaultCandidateUserIds) : requestedCandidateUserIds;
        if (selectedUserId == null) {
            selectedUserId = positiveUserId(defaultSelectedUserId);
        }
        if (selectedUserId == null && CollUtil.isNotEmpty(candidateUserIds)) {
            selectedUserId = candidateUserIds.get(0);
        }
        candidateUserIds = mergeUserIds(candidateUserIds, List.of(), selectedUserId);
        if (selectedUserId != null && !userContext.userMap().containsKey(selectedUserId)) {
            selectedUserId = CollUtil.isEmpty(candidateUserIds) ? null : candidateUserIds.get(0);
        }
        String deptName = resolveDeptName(userContext.userMap().get(selectedUserId), userContext.deptMap());
        return new UserAssignment(selectedUserId, candidateUserIds, deptName);
    }

    private String defaultIndicatorType(SrmEvaluationTemplateItemDO templateItem) {
        String text = StrUtil.blankToDefault(templateItem.getScoringRule(), "") + " "
                + StrUtil.blankToDefault(templateItem.getIndicatorName(), "");
        if (containsAny(text, "计算", "实际值", "合格率", "达成率", "回复率", "次数")) {
            return INDICATOR_CALCULATED;
        }
        return INDICATOR_MANUAL;
    }

    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

    private String buildConfigNo() {
        return "SPC-" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss");
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

    private record UserResolveContext(Map<Long, AdminUserRespDTO> userMap,
                                      Map<Long, DeptRespDTO> deptMap) {
    }

    private record UserAssignment(Long selectedUserId, List<Long> candidateUserIds, String deptName) {
    }

}
