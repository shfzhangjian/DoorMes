package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportAvailableSupplierRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceMetricCalcNodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceMetricCalcRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceActualReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceActualValueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceSupplierConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceSupplierConfigItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceActualReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceActualValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceMetricCalcNodeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceMetricCalcRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceSupplierConfigItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceSupplierConfigMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_SUPPLIER_CONFIG_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_ACTUAL_REPORT_CONFIRM_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_ACTUAL_REPORT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_ACTUAL_REPORT_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID;

@Service
@Validated
public class SrmPerformanceActualReportServiceImpl implements SrmPerformanceActualReportService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String PERIOD_MONTH = "MONTH";
    private static final String PERIOD_QUARTER = "QUARTER";
    private static final String INDICATOR_CALCULATED = "CALCULATED_SCORE";
    private static final String INDICATOR_MIXED = "MIXED";
    private static final String NODE_SOURCE_METRIC = "SOURCE_METRIC";
    private static final String VALUE_STATUS_DRAFT = "DRAFT";
    private static final String VALUE_STATUS_FILLED = "FILLED";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";

    @Resource
    private SrmPerformanceActualReportMapper reportMapper;
    @Resource
    private SrmPerformanceActualValueMapper valueMapper;
    @Resource
    private SrmPerformanceSupplierConfigMapper supplierConfigMapper;
    @Resource
    private SrmPerformanceSupplierConfigItemMapper supplierConfigItemMapper;
    @Resource
    private SrmPerformanceMetricCalcRuleMapper calcRuleMapper;
    @Resource
    private SrmPerformanceMetricCalcNodeMapper calcNodeMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(SrmPerformanceActualReportSaveReqVO reqVO) {
        normalizeSavePeriod(reqVO);
        String reportNo = StrUtil.blankToDefault(StrUtil.trim(reqVO.getReportNo()), buildReportNo());
        if (reportMapper.selectByReportNo(reportNo) != null) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_NO_EXISTS);
        }
        SrmPerformanceSupplierConfigDO config = supplierConfigMapper.selectEnabledBySupplierId(reqVO.getSupplierId());
        UserSnapshot user = currentUser();
        SrmPerformanceActualReportDO report = new SrmPerformanceActualReportDO();
        copyReportFields(reqVO, report, reportNo);
        report.setStatus(STATUS_DRAFT);
        report.setReporterUserId(user.id());
        report.setReporterUserName(user.name());
        report.setVersion(0);
        reportMapper.insert(report);
        if (CollUtil.isEmpty(reqVO.getValues())) {
            createValuesFromTemplate(report.getId(), config);
        } else {
            saveValues(report.getId(), reqVO.getValues());
        }
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReport(SrmPerformanceActualReportSaveReqVO reqVO) {
        SrmPerformanceActualReportDO report = validateReport(reqVO.getId());
        assertEditable(report);
        report.setStatus(STATUS_DRAFT);
        reportMapper.updateById(report);
        updateValues(report.getId(), reqVO.getValues());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long id) {
        SrmPerformanceActualReportDO report = validateReport(id);
        assertEditable(report);
        valueMapper.deleteByReportId(id);
        reportMapper.deleteById(id);
    }

    @Override
    public SrmPerformanceActualReportRespVO getReport(Long id) {
        UserSnapshot user = currentUser();
        boolean admin = isEvaluationAdmin(user.id());
        SrmPerformanceActualReportDO report = validateReport(id);
        assertReadable(report, user, admin);
        return buildResp(report, user, admin);
    }

    @Override
    public PageResult<SrmPerformanceActualReportRespVO> getReportPage(SrmPerformanceActualReportPageReqVO reqVO) {
        UserSnapshot user = currentUser();
        boolean admin = isEvaluationAdmin(user.id());
        if (!admin) {
            reqVO.setReporterUserId(user.id());
        }
        PageResult<SrmPerformanceActualReportDO> page = reportMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(report -> buildResp(report, user, admin)).toList(),
                page.getTotal());
    }

    @Override
    public List<SrmPerformanceActualReportAvailableSupplierRespVO> getAvailableSuppliers(
            String periodType, Integer evalYear, Integer evalQuarter, Integer evalMonth, String supplierInfo) {
        PeriodSelection period = normalizePeriod(periodType, evalYear, evalQuarter, evalMonth);
        String keyword = StrUtil.trim(supplierInfo);
        List<SrmPerformanceActualReportDO> existingReports = reportMapper.selectListByPeriod(period.periodType(),
                period.evalYear(), period.evalQuarter(), period.evalMonth());
        Set<Long> existingSupplierIds = existingReports.stream()
                .map(SrmPerformanceActualReportDO::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> existingSupplierBusinessKeys = existingReports.stream()
                .map(this::buildReportSupplierBusinessKey)
                .collect(Collectors.toSet());
        return supplierConfigMapper.selectEnabledList().stream()
                .filter(config -> config.getSupplierId() != null)
                .filter(config -> StrUtil.isBlank(keyword)
                        || StrUtil.containsIgnoreCase(config.getSupplierCode(), keyword)
                        || StrUtil.containsIgnoreCase(config.getSupplierName(), keyword))
                .filter(config -> !existingSupplierIds.contains(config.getSupplierId()))
                .filter(config -> !existingSupplierBusinessKeys.contains(buildConfigSupplierBusinessKey(config)))
                .collect(Collectors.toMap(this::buildAvailableSupplierGroupKey, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new))
                .values().stream()
                .map(this::toAvailableSupplierResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateReports(SrmPerformanceActualReportBatchCreateReqVO reqVO) {
        PeriodSelection period = normalizePeriod(reqVO.getPeriodType(), reqVO.getEvalYear(), reqVO.getEvalQuarter(),
                reqVO.getEvalMonth());
        Set<Long> supplierIds = reqVO.getSupplierIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(supplierIds)) {
            return List.of();
        }
        Map<Long, SrmPerformanceSupplierConfigDO> configMap = supplierConfigMapper.selectEnabledList().stream()
                .filter(config -> supplierIds.contains(config.getSupplierId()))
                .collect(Collectors.toMap(SrmPerformanceSupplierConfigDO::getSupplierId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        List<SrmPerformanceActualReportDO> existingReports = reportMapper.selectListByPeriod(period.periodType(),
                period.evalYear(), period.evalQuarter(), period.evalMonth());
        Set<Long> existingSupplierIds = existingReports.stream()
                .map(SrmPerformanceActualReportDO::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> existingSupplierBusinessKeys = existingReports.stream()
                .map(this::buildReportSupplierBusinessKey)
                .collect(Collectors.toSet());
        UserSnapshot user = currentUser();
        List<Long> ids = new ArrayList<>();
        for (Long supplierId : supplierIds) {
            SrmPerformanceSupplierConfigDO config = configMap.get(supplierId);
            if (config == null
                    || existingSupplierIds.contains(supplierId)
                    || existingSupplierBusinessKeys.contains(buildConfigSupplierBusinessKey(config))) {
                continue;
            }
            SrmPerformanceActualReportDO report = new SrmPerformanceActualReportDO();
            report.setReportNo(buildReportNo());
            report.setSupplierId(config.getSupplierId());
            report.setSupplierCode(config.getSupplierCode());
            report.setSupplierName(config.getSupplierName());
            report.setSupplierSourceType(StrUtil.blankToDefault(config.getSupplierSourceType(), "REGISTERED"));
            report.setPeriodType(period.periodType());
            report.setEvalYear(period.evalYear());
            report.setEvalQuarter(period.evalQuarter());
            report.setEvalMonth(period.evalMonth());
            report.setStatus(STATUS_DRAFT);
            report.setReporterUserId(user.id());
            report.setReporterUserName(user.name());
            report.setVersion(0);
            reportMapper.insert(report);
            createValuesFromTemplate(report.getId(), config);
            ids.add(report.getId());
            existingSupplierIds.add(supplierId);
            existingSupplierBusinessKeys.add(buildConfigSupplierBusinessKey(config));
        }
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReport(Long id) {
        SrmPerformanceActualReportDO report = validateReport(id);
        assertEditable(report);
        report.setStatus(STATUS_SUBMITTED);
        report.setSubmitTime(LocalDateTime.now());
        reportMapper.updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReport(SrmPerformanceActualReportActionReqVO.Confirm reqVO) {
        assertEvaluationAdmin();
        SrmPerformanceActualReportDO report = validateReport(reqVO.getReportId());
        assertStatus(report, STATUS_SUBMITTED);
        UserSnapshot user = currentUser();
        report.setStatus(STATUS_CONFIRMED);
        report.setConfirmUserId(user.id());
        report.setConfirmUserName(user.name());
        report.setConfirmTime(LocalDateTime.now());
        report.setConfirmOpinion(StrUtil.trim(reqVO.getConfirmOpinion()));
        reportMapper.updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReport(SrmPerformanceActualReportActionReqVO.Confirm reqVO) {
        assertEvaluationAdmin();
        SrmPerformanceActualReportDO report = validateReport(reqVO.getReportId());
        assertStatus(report, STATUS_SUBMITTED);
        UserSnapshot user = currentUser();
        report.setStatus(STATUS_REJECTED);
        report.setConfirmUserId(user.id());
        report.setConfirmUserName(user.name());
        report.setConfirmTime(LocalDateTime.now());
        report.setConfirmOpinion(StrUtil.trim(reqVO.getConfirmOpinion()));
        reportMapper.updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pullMonthlyValues(Long id) {
        SrmPerformanceActualReportDO report = validateReport(id);
        assertEditable(report);
        if (!PERIOD_QUARTER.equals(report.getPeriodType())) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID);
        }
        List<SrmPerformanceActualReportDO> monthlyReports = reportMapper.selectConfirmedMonthlyByQuarter(
                report.getSupplierId(), report.getEvalYear(), report.getEvalQuarter());
        if (CollUtil.isEmpty(monthlyReports)) {
            return;
        }
        Map<Long, SrmPerformanceActualReportDO> monthlyReportMap = monthlyReports.stream()
                .collect(Collectors.toMap(SrmPerformanceActualReportDO::getId, Function.identity()));
        List<SrmPerformanceActualValueDO> monthlyValues = actualValueByReports(monthlyReportMap);
        Map<String, List<SrmPerformanceActualValueDO>> valuesByKey = new HashMap<>();
        Map<String, List<SrmPerformanceActualValueDO>> valuesByMetricCode = new HashMap<>();
        for (SrmPerformanceActualValueDO value : monthlyValues) {
            if (value.getNumericValue() == null) {
                continue;
            }
            valuesByKey.computeIfAbsent(actualValueSourceKey(value.getMetricCode(), value.getSourceNodeKey()),
                    key -> new ArrayList<>()).add(value);
            valuesByMetricCode.computeIfAbsent(cleanKeyPart(value.getMetricCode()), key -> new ArrayList<>()).add(value);
        }
        SrmPerformanceSupplierConfigDO config = supplierConfigMapper.selectEnabledBySupplierId(report.getSupplierId());
        Map<String, ActualValueSource> sourceMap = buildActualValueSourceMap(resolveActualValueSources(config));
        List<SrmPerformanceActualValueDO> quarterValues = valueMapper.selectListByReportId(report.getId());
        for (SrmPerformanceActualValueDO quarterValue : quarterValues) {
            String key = actualValueSourceKey(quarterValue.getMetricCode(), quarterValue.getSourceNodeKey());
            List<SrmPerformanceActualValueDO> sourceValues = valuesByKey.get(key);
            if (CollUtil.isEmpty(sourceValues)) {
                sourceValues = valuesByMetricCode.get(cleanKeyPart(quarterValue.getMetricCode()));
            }
            if (CollUtil.isEmpty(sourceValues)) {
                continue;
            }
            ActualValueSource source = sourceMap.get(key);
            BigDecimal aggregateValue = aggregateActualValues(sourceValues,
                    source == null ? null : source.aggregateMethod());
            SrmPerformanceActualValueDO updateValue = new SrmPerformanceActualValueDO();
            updateValue.setId(quarterValue.getId());
            updateValue.setNumericValue(aggregateValue);
            updateValue.setValueStatus(VALUE_STATUS_FILLED);
            updateValue.setRemark(buildMonthlyPullRemark(sourceValues, monthlyReportMap));
            valueMapper.updateById(updateValue);
        }
    }

    private void saveValues(Long reportId, List<SrmPerformanceActualReportSaveReqVO.Value> values) {
        if (CollUtil.isEmpty(values)) {
            return;
        }
        for (SrmPerformanceActualReportSaveReqVO.Value valueReq : values) {
            SrmPerformanceActualValueDO value = BeanUtils.toBean(valueReq, SrmPerformanceActualValueDO.class);
            value.setId(null);
            value.setReportId(reportId);
            value.setMetricCode(StrUtil.trim(valueReq.getMetricCode()));
            value.setMetricName(StrUtil.trim(valueReq.getMetricName()));
            value.setValueType(StrUtil.blankToDefault(valueReq.getValueType(), "NUMBER"));
            value.setEvidenceRequired(Boolean.TRUE.equals(valueReq.getEvidenceRequired()));
            value.setValueStatus(StrUtil.blankToDefault(valueReq.getValueStatus(), VALUE_STATUS_DRAFT));
            valueMapper.insert(value);
        }
    }

    private void updateValues(Long reportId, List<SrmPerformanceActualReportSaveReqVO.Value> values) {
        if (CollUtil.isEmpty(values)) {
            return;
        }
        Map<Long, SrmPerformanceActualValueDO> existingMap = valueMapper.selectListByReportId(reportId).stream()
                .filter(value -> value.getId() != null)
                .collect(Collectors.toMap(SrmPerformanceActualValueDO::getId, Function.identity()));
        for (SrmPerformanceActualReportSaveReqVO.Value valueReq : values) {
            SrmPerformanceActualValueDO existing = existingMap.get(valueReq.getId());
            if (existing == null) {
                continue;
            }
            SrmPerformanceActualValueDO updateValue = new SrmPerformanceActualValueDO();
            updateValue.setId(existing.getId());
            updateValue.setNumericValue(valueReq.getNumericValue());
            updateValue.setTextValue(StrUtil.trim(valueReq.getTextValue()));
            updateValue.setRemark(StrUtil.trim(valueReq.getRemark()));
            updateValue.setValueStatus(resolveValueStatus(valueReq));
            valueMapper.updateById(updateValue);
        }
    }

    private SrmPerformanceActualReportRespVO buildResp(SrmPerformanceActualReportDO report, UserSnapshot user,
                                                       boolean admin) {
        SrmPerformanceActualReportRespVO resp = BeanUtils.toBean(report, SrmPerformanceActualReportRespVO.class);
        resp.setValues(buildValueResp(report, valueMapper.selectListByReportId(report.getId())));
        boolean editable = STATUS_DRAFT.equals(report.getStatus()) || STATUS_REJECTED.equals(report.getStatus());
        boolean canMaintain = canMaintainReport(report, user, admin);
        resp.setCanEdit(editable && canMaintain);
        resp.setCanSubmit(editable && canMaintain);
        resp.setCanConfirm(admin && STATUS_SUBMITTED.equals(report.getStatus()));
        return resp;
    }

    private List<SrmPerformanceActualReportRespVO.Value> buildValueResp(SrmPerformanceActualReportDO report,
                                                                        List<SrmPerformanceActualValueDO> values) {
        SrmPerformanceSupplierConfigDO config = supplierConfigMapper.selectEnabledBySupplierId(report.getSupplierId());
        Map<String, ActualValueSource> sourceMap = buildActualValueSourceMap(resolveActualValueSources(config));
        return values.stream().map(value -> {
            SrmPerformanceActualReportRespVO.Value resp =
                    BeanUtils.toBean(value, SrmPerformanceActualReportRespVO.Value.class);
            ActualValueSource source = sourceMap.get(actualValueSourceKey(value.getMetricCode(), value.getSourceNodeKey()));
            if (source == null) {
                source = sourceMap.get(actualValueSourceKey(value.getMetricCode(), null));
            }
            if (source != null) {
                resp.setMetricName(StrUtil.blankToDefault(source.metricName(), resp.getMetricName()));
                resp.setUnit(StrUtil.blankToDefault(source.unit(), resp.getUnit()));
                resp.setSourceIndicatorCode(source.sourceIndicatorCode());
                resp.setSourceIndicatorName(source.sourceIndicatorName());
                resp.setCalcDescription(source.calcDescription());
                resp.setFormulaExpr(source.formulaExpr());
                resp.setScoreFormulaExpr(source.scoreFormulaExpr());
            }
            return resp;
        }).toList();
    }

    private void createValuesFromTemplate(Long reportId, SrmPerformanceSupplierConfigDO config) {
        if (config == null) {
            throw exception(SRM_PERFORMANCE_SUPPLIER_CONFIG_NOT_EXISTS);
        }
        List<ActualValueSource> sources = resolveActualValueSources(config);
        for (ActualValueSource source : sources) {
            SrmPerformanceActualValueDO value = new SrmPerformanceActualValueDO();
            value.setReportId(reportId);
            value.setMetricCode(source.metricCode());
            value.setMetricName(source.metricName());
            value.setValueType("NUMBER");
            value.setUnit(source.unit());
            value.setSourceNodeKey(source.sourceNodeKey());
            value.setEvidenceRequired(Boolean.TRUE);
            value.setValueStatus(VALUE_STATUS_DRAFT);
            valueMapper.insert(value);
        }
    }

    private List<ActualValueSource> resolveActualValueSources(SrmPerformanceSupplierConfigDO config) {
        if (config == null || config.getId() == null) {
            return List.of();
        }
        List<SrmPerformanceSupplierConfigItemDO> configItems = supplierConfigItemMapper
                .selectListByConfigAndTemplateVersion(config.getId(), config.getCurrentTemplateVersionId());
        Map<Long, SrmPerformanceSupplierConfigItemDO> configItemMap = configItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(SrmPerformanceSupplierConfigItemDO::getId, Function.identity(),
                        (left, right) -> left));
        List<SrmPerformanceMetricCalcRuleDO> rules = calcRuleMapper.selectListByConfigId(config.getId()).stream()
                .filter(rule -> rule.getEnabled() == null || Boolean.TRUE.equals(rule.getEnabled()))
                .toList();
        List<ActualValueSource> sources = new ArrayList<>();
        for (SrmPerformanceMetricCalcRuleDO rule : rules) {
            SrmPerformanceSupplierConfigItemDO item = configItemMap.get(rule.getConfigItemId());
            if (item == null || !isActualRequiredItem(item)) {
                continue;
            }
            List<SrmPerformanceMetricCalcNodeDO> nodes = calcNodeMapper.selectListByRuleId(rule.getId()).stream()
                    .filter(node -> NODE_SOURCE_METRIC.equals(StrUtil.blankToDefault(node.getNodeType(), NODE_SOURCE_METRIC)))
                    .toList();
            if (CollUtil.isEmpty(nodes)) {
                nodes = List.of(defaultSourceNode(item));
            }
            for (SrmPerformanceMetricCalcNodeDO node : nodes) {
                String metricCode = StrUtil.blankToDefault(StrUtil.trim(node.getSourceMetricCode()),
                        item.getIndicatorCodeSnapshot());
                sources.add(new ActualValueSource(
                        actualValueSourceKey(metricCode, node.getNodeKey()),
                        metricCode,
                        StrUtil.blankToDefault(StrUtil.trim(node.getSourceMetricName()),
                                StrUtil.blankToDefault(StrUtil.trim(node.getNodeName()), item.getIndicatorNameSnapshot())),
                        StrUtil.trim(node.getNodeKey()),
                        StrUtil.trim(node.getUnit()),
                        item.getIndicatorCodeSnapshot(),
                        item.getIndicatorNameSnapshot(),
                        buildCalcDescription(item, rule, node),
                        StrUtil.blankToDefault(StrUtil.trim(node.getAggregateMethod()),
                                StrUtil.blankToDefault(StrUtil.trim(rule.getAggregateMethod()), "SUM")),
                        rule.getFormulaExpr(),
                        rule.getScoreFormulaExpr(),
                        item.getGroupSort() == null ? 0 : item.getGroupSort(),
                        item.getIndicatorSort() == null ? 0 : item.getIndicatorSort(),
                        node.getSortNo() == null ? 0 : node.getSortNo()));
            }
        }
        return sources.stream()
                .sorted(Comparator.comparing(ActualValueSource::groupSort)
                        .thenComparing(ActualValueSource::indicatorSort)
                        .thenComparing(ActualValueSource::nodeSort))
                .collect(Collectors.toMap(ActualValueSource::key, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new))
                .values().stream()
                .toList();
    }

    private Map<String, ActualValueSource> buildActualValueSourceMap(List<ActualValueSource> sources) {
        Map<String, ActualValueSource> result = new HashMap<>();
        for (ActualValueSource source : sources) {
            result.put(source.key(), source);
            result.putIfAbsent(actualValueSourceKey(source.metricCode(), null), source);
        }
        return result;
    }

    private SrmPerformanceMetricCalcNodeDO defaultSourceNode(SrmPerformanceSupplierConfigItemDO item) {
        SrmPerformanceMetricCalcNodeDO node = new SrmPerformanceMetricCalcNodeDO();
        node.setNodeKey(item.getIndicatorCodeSnapshot());
        node.setNodeName(item.getIndicatorNameSnapshot());
        node.setNodeType(NODE_SOURCE_METRIC);
        node.setSourceMetricCode(item.getIndicatorCodeSnapshot());
        node.setSourceMetricName(item.getIndicatorNameSnapshot());
        node.setAggregateMethod("SUM");
        node.setUnit(item.getTargetUnit());
        node.setSortNo(1);
        node.setRequiredFlag(Boolean.TRUE);
        return node;
    }

    private boolean isActualRequiredItem(SrmPerformanceSupplierConfigItemDO item) {
        return INDICATOR_CALCULATED.equals(item.getIndicatorType()) || INDICATOR_MIXED.equals(item.getIndicatorType());
    }

    private String buildCalcDescription(SrmPerformanceSupplierConfigItemDO item,
                                        SrmPerformanceMetricCalcRuleDO rule,
                                        SrmPerformanceMetricCalcNodeDO node) {
        return StrUtil.isBlank(item.getScoringRuleSnapshot())
                ? null
                : "评分说明：" + item.getScoringRuleSnapshot();
    }

    private void copyReportFields(SrmPerformanceActualReportSaveReqVO reqVO,
                                  SrmPerformanceActualReportDO report,
                                  String reportNo) {
        report.setReportNo(reportNo);
        report.setSupplierId(reqVO.getSupplierId());
        report.setSupplierCode(StrUtil.trim(reqVO.getSupplierCode()));
        report.setSupplierName(StrUtil.trim(reqVO.getSupplierName()));
        report.setSupplierSourceType(StrUtil.blankToDefault(reqVO.getSupplierSourceType(), "REGISTERED"));
        String periodType = StrUtil.blankToDefault(reqVO.getPeriodType(), reqVO.getEvalMonth() == null
                ? PERIOD_QUARTER : PERIOD_MONTH);
        report.setPeriodType(PERIOD_MONTH.equals(periodType) ? PERIOD_MONTH : PERIOD_QUARTER);
        report.setEvalYear(reqVO.getEvalYear());
        report.setEvalQuarter(reqVO.getEvalQuarter());
        report.setEvalMonth(reqVO.getEvalMonth());
        report.setRemark(StrUtil.trim(reqVO.getRemark()));
    }

    private void normalizeSavePeriod(SrmPerformanceActualReportSaveReqVO reqVO) {
        PeriodSelection period = normalizePeriod(reqVO.getPeriodType(), reqVO.getEvalYear(), reqVO.getEvalQuarter(),
                reqVO.getEvalMonth());
        reqVO.setPeriodType(period.periodType());
        reqVO.setEvalYear(period.evalYear());
        reqVO.setEvalQuarter(period.evalQuarter());
        reqVO.setEvalMonth(period.evalMonth());
    }

    private PeriodSelection normalizePeriod(String periodType, Integer evalYear, Integer evalQuarter,
                                            Integer evalMonth) {
        String normalizedPeriodType = PERIOD_QUARTER.equals(periodType) ? PERIOD_QUARTER : PERIOD_MONTH;
        if (evalYear == null) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID);
        }
        if (PERIOD_MONTH.equals(normalizedPeriodType)) {
            if (evalMonth == null || evalMonth < 1 || evalMonth > 12) {
                throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID);
            }
            Integer normalizedQuarter = (evalMonth + 2) / 3;
            return new PeriodSelection(normalizedPeriodType, evalYear, normalizedQuarter, evalMonth);
        }
        if (evalQuarter == null || evalQuarter < 1 || evalQuarter > 4) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID);
        }
        return new PeriodSelection(normalizedPeriodType, evalYear, evalQuarter, null);
    }

    private List<SrmPerformanceActualValueDO> actualValueByReports(
            Map<Long, SrmPerformanceActualReportDO> reportMap) {
        if (reportMap.isEmpty()) {
            return List.of();
        }
        return valueMapper.selectListByReportIds(new ArrayList<>(reportMap.keySet()));
    }

    private BigDecimal aggregateActualValues(List<SrmPerformanceActualValueDO> values, String aggregateMethod) {
        if (CollUtil.isEmpty(values)) {
            return null;
        }
        String method = StrUtil.blankToDefault(StrUtil.trim(aggregateMethod), "SUM");
        if ("AVG".equals(method)) {
            return values.stream().map(SrmPerformanceActualValueDO::getNumericValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
        }
        if ("MAX".equals(method)) {
            return values.stream().map(SrmPerformanceActualValueDO::getNumericValue)
                    .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        }
        if ("MIN".equals(method)) {
            return values.stream().map(SrmPerformanceActualValueDO::getNumericValue)
                    .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        }
        return values.stream().map(SrmPerformanceActualValueDO::getNumericValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildMonthlyPullRemark(List<SrmPerformanceActualValueDO> values,
                                          Map<Long, SrmPerformanceActualReportDO> reportMap) {
        String months = values.stream()
                .map(value -> reportMap.get(value.getReportId()))
                .filter(Objects::nonNull)
                .map(SrmPerformanceActualReportDO::getEvalMonth)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .map(month -> month + "月")
                .collect(Collectors.joining("、"));
        return StrUtil.isBlank(months) ? "已汇总已确认月度实际值" : "已汇总已确认月度实际值：" + months;
    }

    private String resolveValueStatus(SrmPerformanceActualReportSaveReqVO.Value valueReq) {
        if (valueReq.getNumericValue() != null || StrUtil.isNotBlank(valueReq.getTextValue())) {
            return VALUE_STATUS_FILLED;
        }
        return VALUE_STATUS_DRAFT;
    }

    private SrmPerformanceActualReportAvailableSupplierRespVO toAvailableSupplierResp(
            SrmPerformanceSupplierConfigDO config) {
        SrmPerformanceActualReportAvailableSupplierRespVO resp =
                new SrmPerformanceActualReportAvailableSupplierRespVO();
        resp.setSupplierId(config.getSupplierId());
        resp.setSupplierCode(config.getSupplierCode());
        resp.setSupplierName(config.getSupplierName());
        resp.setSupplierSourceType(StrUtil.blankToDefault(config.getSupplierSourceType(), "REGISTERED"));
        resp.setConfigId(config.getId());
        resp.setTemplateVersionId(config.getCurrentTemplateVersionId());
        resp.setTemplateCodeSnapshot(config.getTemplateCodeSnapshot());
        resp.setTemplateNameSnapshot(config.getTemplateNameSnapshot());
        resp.setTemplateVersionNoSnapshot(config.getTemplateVersionNoSnapshot());
        return resp;
    }

    private String buildAvailableSupplierGroupKey(SrmPerformanceSupplierConfigDO config) {
        return String.join("|",
                cleanKeyPart(config.getSupplierCode()),
                cleanKeyPart(config.getSupplierName()));
    }

    private String buildConfigSupplierBusinessKey(SrmPerformanceSupplierConfigDO config) {
        return String.join("|",
                cleanKeyPart(config.getSupplierCode()),
                cleanKeyPart(config.getSupplierName()));
    }

    private String buildReportSupplierBusinessKey(SrmPerformanceActualReportDO report) {
        return String.join("|",
                cleanKeyPart(report.getSupplierCode()),
                cleanKeyPart(report.getSupplierName()));
    }

    private String actualValueSourceKey(String metricCode, String sourceNodeKey) {
        return cleanKeyPart(metricCode) + "|" + cleanKeyPart(sourceNodeKey);
    }

    private String cleanKeyPart(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "");
    }

    private void assertEditable(SrmPerformanceActualReportDO report) {
        if (!STATUS_DRAFT.equals(report.getStatus()) && !STATUS_REJECTED.equals(report.getStatus())) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID);
        }
        UserSnapshot user = currentUser();
        if (!canMaintainReport(report, user, isEvaluationAdmin(user.id()))) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID);
        }
    }

    private void assertStatus(SrmPerformanceActualReportDO report, String expected) {
        if (!expected.equals(report.getStatus())) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_STATUS_INVALID);
        }
    }

    private SrmPerformanceActualReportDO validateReport(Long id) {
        SrmPerformanceActualReportDO report = id == null ? null : reportMapper.selectById(id);
        if (report == null) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_NOT_EXISTS);
        }
        return report;
    }

    private void assertReadable(SrmPerformanceActualReportDO report, UserSnapshot user, boolean admin) {
        if (!canMaintainReport(report, user, admin)) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_NOT_EXISTS);
        }
    }

    private void assertEvaluationAdmin() {
        if (!isEvaluationAdmin(currentUser().id())) {
            throw exception(SRM_PERFORMANCE_ACTUAL_REPORT_CONFIRM_REQUIRED);
        }
    }

    private boolean canMaintainReport(SrmPerformanceActualReportDO report, UserSnapshot user, boolean admin) {
        return admin || (user.id() != null && Objects.equals(report.getReporterUserId(), user.id()));
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

    private UserSnapshot currentUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        if (userId != null && StrUtil.isBlank(userName)) {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            userName = user == null ? null : user.getNickname();
        }
        return new UserSnapshot(userId, StrUtil.blankToDefault(userName, "系统用户"));
    }

    private String buildReportNo() {
        return "SPA-" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS") + "-"
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    private record UserSnapshot(Long id, String name) {
    }

    private record PeriodSelection(String periodType, Integer evalYear, Integer evalQuarter, Integer evalMonth) {
    }

    private record ActualValueSource(String key,
                                     String metricCode,
                                     String metricName,
                                     String sourceNodeKey,
                                     String unit,
                                     String sourceIndicatorCode,
                                     String sourceIndicatorName,
                                     String calcDescription,
                                     String aggregateMethod,
                                     String formulaExpr,
                                     String scoreFormulaExpr,
                                     Integer groupSort,
                                     Integer indicatorSort,
                                     Integer nodeSort) {
    }

}
