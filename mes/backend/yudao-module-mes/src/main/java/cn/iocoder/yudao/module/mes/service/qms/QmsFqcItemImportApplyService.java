package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSheetImportBatchDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSheetImportBatchMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * FQC 检验项明细导入确认应用服务。
 */
@Service
class QmsFqcItemImportApplyService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_INSPECTING = "INSPECTING";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String ROLE_QA = "QA";
    private static final String ENTRY_LAYOUT_PROGRAM_FORM = "PROGRAM_FORM";
    private static final String ENTRY_MODE_EXCEL_IMPORT = "EXCEL_IMPORT";
    private static final String IMPORT_STATUS_SUCCESS = "SUCCESS";
    private static final String IMPORT_STATUS_FAILED = "FAILED";
    private static final String IMPORT_USAGE_ITEM_OVERVIEW_BATCH = "ITEM_OVERVIEW_BATCH";
    private static final String ITEM_TYPE_QUANTITATIVE = "QUANTITATIVE";
    private static final String ITEM_TYPE_QUALITATIVE = "QUALITATIVE";
    private static final String TEMPLATE_SINGLE_VALUE = "SINGLE_VALUE";
    private static final String TEMPLATE_DENSITY_CALC = "DENSITY_CALC";
    private static final String TEMPLATE_COMPRESSION_CALC = "COMPRESSION_CALC";
    private static final String METRIC_RESULT_VALUE = "RESULT_VALUE";
    private static final String METRIC_DENSITY_VALUE = "DENSITY_VALUE";
    private static final String METRIC_COMPRESSION_RATE = "COMPRESSION_RATE";
    private static final String METRIC_COMPRESSION_ELASTICITY_RATE = "COMPRESSION_ELASTICITY_RATE";
    private static final String VALUE_SOURCE_ITEM_IMPORT = "ITEM_IMPORT";
    private static final String INPUT_STATUS_EMPTY = "EMPTY";
    private static final String INPUT_STATUS_FILLING = "FILLING";
    private static final String INPUT_STATUS_COMPLETE = "COMPLETE";
    private static final String INPUT_STATUS_ABNORMAL = "ABNORMAL";
    private static final BigDecimal DEFAULT_SAMPLE_DIAMETER_MM = BigDecimal.valueOf(39);
    private static final BigDecimal PI = BigDecimal.valueOf(3.14);
    private static final int CALC_SCALE = 6;

    @Resource
    private QmsFqcItemMapper qmsFqcItemMapper;
    @Resource
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Resource
    private QmsFqcSampleMapper qmsFqcSampleMapper;
    @Resource
    private QmsFqcSheetImportBatchMapper qmsFqcSheetImportBatchMapper;
    @Resource
    private QmsFqcItemWorkbookService qmsFqcItemWorkbookService;

    boolean applyItemImport(QmsFqcItemWorkbookService.ItemImportPlan plan, String importBatchNo) {
        if (plan.failureCount > 0 || plan.validRows.isEmpty()) {
            saveItemImportBatch(plan, importBatchNo, IMPORT_STATUS_FAILED);
            return false;
        }

        Map<Long, List<QmsFqcItemWorkbookService.ItemImportRow>> rowMap = plan.validRows.stream()
                .collect(Collectors.groupingBy(row -> row.item.getId(), LinkedHashMap::new, Collectors.toList()));
        List<Long> touchedItemIds = new ArrayList<>(rowMap.keySet());
        List<QmsFqcSampleDO> existingSamples = qmsFqcSampleMapper.selectListByFqcIdAndItemIdsAndRole(
                plan.order.getId(), touchedItemIds, ROLE_QA);
        Map<String, QmsFqcSampleDO> existingSampleMap = existingSamples.stream()
                .collect(Collectors.toMap(this::sampleKey, sample -> sample, (first, ignored) -> first));
        Map<String, QmsFqcSaveReqVO.FqcSample> allTouchedSampleMap = new LinkedHashMap<>();
        for (QmsFqcSampleDO existingSample : existingSamples) {
            allTouchedSampleMap.put(sampleKey(existingSample),
                    BeanUtils.toBean(existingSample, QmsFqcSaveReqVO.FqcSample.class));
        }
        for (QmsFqcItemWorkbookService.ItemImportRow row : plan.validRows) {
            QmsFqcSaveReqVO.FqcSample sample = qmsFqcItemWorkbookService.buildImportedSample(row, importBatchNo,
                    existingSampleMap.get(sampleKey(row.item.getId(), row.sampleSeq)));
            allTouchedSampleMap.put(sampleKey(row.item.getId(), row.sampleSeq), sample);
        }

        Map<Long, List<QmsFqcSaveReqVO.FqcSample>> sampleMap = groupSamplesByItem(allTouchedSampleMap);
        saveImportedSamples(plan, sampleMap, importBatchNo);
        updateImportProgress(plan.order, importBatchNo);
        saveItemImportBatch(plan, importBatchNo, IMPORT_STATUS_SUCCESS);
        return true;
    }

    private Map<Long, List<QmsFqcSaveReqVO.FqcSample>> groupSamplesByItem(
            Map<String, QmsFqcSaveReqVO.FqcSample> allTouchedSampleMap) {
        Map<Long, List<QmsFqcSaveReqVO.FqcSample>> sampleMap = new LinkedHashMap<>();
        for (Map.Entry<String, QmsFqcSaveReqVO.FqcSample> entry : allTouchedSampleMap.entrySet()) {
            Long itemId = Long.valueOf(entry.getKey().split("#")[0]);
            sampleMap.computeIfAbsent(itemId, key -> new ArrayList<>()).add(entry.getValue());
        }
        return sampleMap;
    }

    private void saveImportedSamples(QmsFqcItemWorkbookService.ItemImportPlan plan,
                                     Map<Long, List<QmsFqcSaveReqVO.FqcSample>> sampleMap,
                                     String importBatchNo) {
        for (Map.Entry<Long, List<QmsFqcSaveReqVO.FqcSample>> entry : sampleMap.entrySet()) {
            QmsFqcItemDO item = plan.itemMap.get(entry.getKey());
            if (item == null) {
                continue;
            }
            List<QmsFqcSaveReqVO.FqcSample> samples = entry.getValue().stream()
                    .sorted(Comparator.comparing(QmsFqcSaveReqVO.FqcSample::getSampleSeq))
                    .collect(Collectors.toList());
            applyItemStats(item, samples);
            qmsFqcItemMapper.updateById(item);
            for (QmsFqcSaveReqVO.FqcSample sample : samples) {
                QmsFqcSampleDO sampleDO = BeanUtils.toBean(sample, QmsFqcSampleDO.class);
                sampleDO.setFqcId(plan.order.getId());
                sampleDO.setFqcNo(plan.order.getFqcNo());
                sampleDO.setFqcItemId(item.getId());
                sampleDO.setSampleRole(ROLE_QA);
                sampleDO.setStepCode(defaultIfBlank(sampleDO.getStepCode(), item.getStepCode()));
                sampleDO.setMetricCode(defaultIfBlank(sampleDO.getMetricCode(), item.getMetricCode()));
                sampleDO.setMetricGroupCode(defaultIfBlank(sampleDO.getMetricGroupCode(), item.getMetricGroupCode()));
                sampleDO.setInputComponent(defaultIfBlank(sampleDO.getInputComponent(), item.getInputComponent()));
                sampleDO.setSheetSectionCode(defaultIfBlank(sampleDO.getSheetSectionCode(), item.getSheetSectionCode()));
                sampleDO.setSheetMetricCode(defaultIfBlank(sampleDO.getSheetMetricCode(), item.getSheetMetricCode()));
                sampleDO.setValueSource(defaultIfBlank(sampleDO.getValueSource(), VALUE_SOURCE_ITEM_IMPORT));
                sampleDO.setSampleResult(defaultIfBlank(sampleDO.getSampleResult(), JUDGMENT_PENDING));
                if (Objects.equals(sampleDO.getImportBatchNo(), importBatchNo) || sampleDO.getInputTime() == null) {
                    sampleDO.setInputTime(LocalDateTime.now());
                }
                if (sampleDO.getId() == null) {
                    qmsFqcSampleMapper.insert(sampleDO);
                } else {
                    qmsFqcSampleMapper.updateById(sampleDO);
                }
            }
        }
    }

    private void applyItemStats(QmsFqcItemDO item, List<QmsFqcSaveReqVO.FqcSample> samples) {
        int expectedSampleCount = resolveExpectedSampleCount(item);
        TemplateStats stats = calculateTemplateStats(item, samples, expectedSampleCount);
        item.setRequiredSampleCount(expectedSampleCount);
        item.setCellRequiredCount(expectedSampleCount);
        item.setCompletedSampleCount(stats.completedCount);
        item.setCellCompletedCount(stats.completedCount);
        item.setAbnormalSampleCount(stats.abnormalCount);
        item.setItemResult(stats.result);
        item.setQaResult(stats.result);
        item.setQaInspectorId(SecurityFrameworkUtils.getLoginUserId());
        item.setQaInspectorName(resolveLoginUserName());
        item.setQaTime(LocalDateTime.now());
        if (!stats.values.isEmpty()) {
            item.setMaxValue(stats.max);
            item.setMinValue(stats.min);
            item.setAverageValue(stats.avg);
            item.setQaMax(stats.max);
            item.setQaMin(stats.min);
            item.setQaAvg(stats.avg);
            item.setCalculatedMax(stats.max);
            item.setCalculatedMin(stats.min);
            item.setCalculatedAvg(stats.avg);
            item.setCalculatedStd(stats.std);
        }
        item.setInputStatus(stats.completedCount == 0 ? INPUT_STATUS_EMPTY
                : JUDGMENT_NG.equals(stats.result) ? INPUT_STATUS_ABNORMAL
                : JUDGMENT_OK.equals(stats.result) ? INPUT_STATUS_COMPLETE : INPUT_STATUS_FILLING);
    }

    private TemplateStats calculateTemplateStats(QmsFqcItemDO item, List<QmsFqcSaveReqVO.FqcSample> samples,
                                                 int expectedSampleCount) {
        TemplateStats stats = new TemplateStats();
        if (samples == null || samples.isEmpty()) {
            return stats;
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(item.getItemType())) {
            calculateQuantitativeSamples(item, samples, expectedSampleCount, stats);
        } else {
            calculateQualitativeSamples(samples, expectedSampleCount, stats);
        }
        return stats;
    }

    private void calculateQuantitativeSamples(QmsFqcItemDO item, List<QmsFqcSaveReqVO.FqcSample> samples,
                                              int expectedSampleCount, TemplateStats stats) {
        String template = resolveValueTemplate(item.getItemType(), item.getValueTemplate());
        item.setValueTemplate(template);
        if (!StringUtils.hasText(item.getValueTemplateName())) {
            item.setValueTemplateName(resolveValueTemplateName(template));
        }
        item.setJudgmentMetric(resolveJudgmentMetric(template, item.getJudgmentMetric()));
        boolean hasPending = samples.size() < expectedSampleCount;
        for (QmsFqcSaveReqVO.FqcSample sample : samples) {
            BigDecimal metricValue = calculateSampleMetric(item, sample, template);
            if (metricValue == null) {
                sample.setSampleResult(JUDGMENT_PENDING);
                hasPending = true;
                continue;
            }
            stats.values.add(metricValue);
            stats.completedCount++;
            sample.setSampleResult(isMetricValueNg(item, metricValue) ? JUDGMENT_NG : JUDGMENT_OK);
            if (JUDGMENT_NG.equals(sample.getSampleResult())) {
                stats.abnormalCount++;
            }
        }
        if (stats.values.size() < expectedSampleCount || hasPending) {
            stats.result = JUDGMENT_PENDING;
            return;
        }
        stats.recalculate();
        stats.result = samples.stream().anyMatch(sample -> JUDGMENT_NG.equals(sample.getSampleResult()))
                || isAggregateNg(item, stats.avg, stats.std) ? JUDGMENT_NG : JUDGMENT_OK;
    }

    private void calculateQualitativeSamples(List<QmsFqcSaveReqVO.FqcSample> samples, int expectedSampleCount,
                                             TemplateStats stats) {
        boolean hasPending = samples.size() < expectedSampleCount;
        for (QmsFqcSaveReqVO.FqcSample sample : samples) {
            String result = defaultIfBlank(sample.getQualitativeValue(), sample.getSampleResult());
            sample.setQualitativeValue(result);
            sample.setSampleResult(defaultIfBlank(result, JUDGMENT_PENDING));
            if (JUDGMENT_NG.equals(sample.getSampleResult())) {
                stats.completedCount++;
                stats.abnormalCount++;
                stats.result = JUDGMENT_NG;
            } else if (JUDGMENT_OK.equals(sample.getSampleResult())) {
                stats.completedCount++;
            } else {
                hasPending = true;
            }
        }
        if (JUDGMENT_NG.equals(stats.result)) {
            return;
        }
        stats.result = hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    private BigDecimal calculateSampleMetric(QmsFqcItemDO item, QmsFqcSaveReqVO.FqcSample sample, String template) {
        Map<String, Object> rawValues = new LinkedHashMap<>(parseRawValues(sample.getRawValuesJson()));
        if (QmsEntryRuleFormulaSupport.hasDataRule(item.getTemplateParams())) {
            try {
                QmsEntryRuleFormulaSupport.CalculationResult result =
                        QmsEntryRuleFormulaSupport.calculate(item.getTemplateParams(), rawValues);
                result.getResults().forEach(rawValues::put);
                if (!result.getResults().isEmpty()) {
                    sample.setRawValuesJson(JsonUtils.toJsonString(rawValues));
                }
                BigDecimal metric = result.getJudgmentValue();
                sample.setResultValue(metric);
                sample.setMeasuredValue(metric);
                sample.setDensityValue(result.getResults().getOrDefault("densityValue", sample.getDensityValue()));
                sample.setCompressionRate(result.getResults().getOrDefault("compressionRate", sample.getCompressionRate()));
                sample.setCompressionElasticityRate(result.getResults()
                        .getOrDefault("compressionElasticityRate", sample.getCompressionElasticityRate()));
                if (StringUtils.hasText(result.getJudgmentMetric())) {
                    item.setJudgmentMetric(result.getJudgmentMetric());
                }
                return metric;
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            BigDecimal thicknessMm = getDecimal(rawValues, "thicknessMm", null);
            BigDecimal weightG = getDecimal(rawValues, "weightG", null);
            BigDecimal diameterMm = getDecimal(rawValues, "diameterMm", DEFAULT_SAMPLE_DIAMETER_MM);
            if (isMissingOrZero(thicknessMm) || isMissingOrZero(diameterMm) || weightG == null) {
                return null;
            }
            BigDecimal denominator = thicknessMm
                    .divide(BigDecimal.TEN, CALC_SCALE, RoundingMode.HALF_UP)
                    .multiply(PI)
                    .multiply(diameterMm)
                    .multiply(diameterMm)
                    .divide(BigDecimal.valueOf(4), CALC_SCALE, RoundingMode.HALF_UP);
            if (isMissingOrZero(denominator)) {
                return null;
            }
            BigDecimal density = weightG.divide(denominator, CALC_SCALE, RoundingMode.HALF_UP);
            sample.setDensityValue(density);
            sample.setResultValue(density);
            sample.setMeasuredValue(density);
            return density;
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(template)) {
            BigDecimal t1Mm = getDecimal(rawValues, "t1Mm", null);
            BigDecimal t2Mm = getDecimal(rawValues, "t2Mm", null);
            BigDecimal t3Mm = getDecimal(rawValues, "t3Mm", null);
            if (isMissingOrZero(t1Mm) || t2Mm == null || t3Mm == null || t1Mm.compareTo(t2Mm) == 0) {
                return null;
            }
            BigDecimal compressionRate = t1Mm.subtract(t2Mm)
                    .divide(t1Mm, CALC_SCALE, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            BigDecimal compressionElasticityRate = t3Mm.subtract(t2Mm)
                    .divide(t1Mm.subtract(t2Mm), CALC_SCALE, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            sample.setCompressionRate(compressionRate);
            sample.setCompressionElasticityRate(compressionElasticityRate);
            BigDecimal metric = METRIC_COMPRESSION_ELASTICITY_RATE.equals(item.getJudgmentMetric())
                    ? compressionElasticityRate : compressionRate;
            sample.setResultValue(metric);
            sample.setMeasuredValue(metric);
            return metric;
        }
        BigDecimal value = sample.getMeasuredValue();
        if (value == null) {
            value = getDecimal(rawValues, "value", sample.getResultValue());
        }
        sample.setResultValue(value);
        sample.setMeasuredValue(value);
        return value;
    }

    private void updateImportProgress(QmsFqcOrderDO order, String importBatchNo) {
        List<QmsFqcItemDO> items = qmsFqcItemMapper.selectListByFqcId(order.getId());
        int required = items.size();
        int completed = (int) items.stream()
                .filter(item -> JUDGMENT_OK.equals(item.getItemResult()) || JUDGMENT_NG.equals(item.getItemResult()))
                .count();
        int abnormal = (int) items.stream().filter(item -> JUDGMENT_NG.equals(item.getItemResult())).count();
        QmsFqcOrderDO updateObj = new QmsFqcOrderDO();
        updateObj.setId(order.getId());
        updateObj.setEntryMode(ENTRY_MODE_EXCEL_IMPORT);
        updateObj.setEntryLayout(ENTRY_LAYOUT_PROGRAM_FORM);
        updateObj.setEntryProgress(required == 0 ? 0 : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(required), 0, RoundingMode.DOWN).intValue());
        updateObj.setRequiredItemCount(required);
        updateObj.setCompletedItemCount(completed);
        updateObj.setAbnormalItemCount(abnormal);
        updateObj.setLastSaveTime(LocalDateTime.now());
        updateObj.setLastImportBatchNo(importBatchNo);
        updateObj.setInspectorId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setInspectorName(resolveLoginUserName());
        updateObj.setInspectionTime(LocalDateTime.now());
        if (STATUS_PENDING.equals(order.getStatus()) || STATUS_SUSPENDED.equals(order.getStatus())) {
            updateObj.setStatus(STATUS_INSPECTING);
        } else {
            updateObj.setStatus(order.getStatus());
        }
        qmsFqcOrderMapper.updateById(updateObj);
    }

    private void saveItemImportBatch(QmsFqcItemWorkbookService.ItemImportPlan plan, String importBatchNo,
                                     String status) {
        QmsFqcSheetImportBatchDO batch = QmsFqcSheetImportBatchDO.builder()
                .fqcId(plan.order.getId())
                .targetFqcId(plan.order.getId())
                .targetFqcNo(plan.order.getFqcNo())
                .importBatchNo(importBatchNo)
                .fileName(plan.fileName)
                .sheetTemplateId(plan.order.getSheetTemplateId())
                .templateCode(plan.order.getSheetTemplateCode())
                .templateVersion(plan.order.getSheetTemplateVersion())
                .templateVersionHash(plan.templateVersionHash)
                .status(status)
                .importUsage(IMPORT_USAGE_ITEM_OVERVIEW_BATCH)
                .validateSummary(JsonUtils.toJsonString(plan.messages))
                .allowOverwrite(plan.allowOverwrite)
                .reviewStatus(JUDGMENT_PENDING)
                .successCount(plan.successCount)
                .failureCount(plan.failureCount)
                .warningCount(plan.warningCount)
                .errorSummary(JsonUtils.toJsonString(plan.messages))
                .importerId(SecurityFrameworkUtils.getLoginUserId())
                .importerName(resolveLoginUserName())
                .importTime(LocalDateTime.now())
                .build();
        qmsFqcSheetImportBatchMapper.insert(batch);
    }

    private boolean isMetricValueNg(QmsFqcItemDO item, BigDecimal value) {
        return (item.getMinValueLimit() != null && value.compareTo(item.getMinValueLimit()) < 0)
                || (item.getMaxValueLimit() != null && value.compareTo(item.getMaxValueLimit()) > 0);
    }

    private boolean isAggregateNg(QmsFqcItemDO item, BigDecimal avg, BigDecimal std) {
        return (item.getAvgMinLimit() != null && avg.compareTo(item.getAvgMinLimit()) < 0)
                || (item.getAvgMaxLimit() != null && avg.compareTo(item.getAvgMaxLimit()) > 0)
                || (item.getStdMinLimit() != null && std.compareTo(item.getStdMinLimit()) < 0)
                || (item.getStdMaxLimit() != null && std.compareTo(item.getStdMaxLimit()) > 0);
    }

    private int resolveExpectedSampleCount(QmsFqcItemDO item) {
        int baseFallback = item.getSampleSize() == null || item.getSampleSize() < 1 ? 1 : item.getSampleSize();
        Map<String, Object> templateParams = parseRawValues(item.getTemplateParams());
        Integer sampleSize = toPositiveInt(templateParams.get("sampleSize"));
        if (sampleSize != null) {
            return sampleSize;
        }
        Object positions = templateParams.get("positions");
        if (positions instanceof List<?> collection && !collection.isEmpty()) {
            Integer repeatCount = toPositiveInt(templateParams.get("repeatCount"));
            return collection.size() * (repeatCount == null ? 1 : repeatCount);
        }
        return baseFallback;
    }

    private String resolveValueTemplate(String itemType, String valueTemplate) {
        if (!ITEM_TYPE_QUANTITATIVE.equals(itemType)) {
            return null;
        }
        String template = defaultIfBlank(valueTemplate, TEMPLATE_SINGLE_VALUE);
        return TEMPLATE_DENSITY_CALC.equals(template) || TEMPLATE_COMPRESSION_CALC.equals(template)
                ? template : TEMPLATE_SINGLE_VALUE;
    }

    private String resolveValueTemplateName(String valueTemplate) {
        if (TEMPLATE_DENSITY_CALC.equals(valueTemplate)) {
            return "密度计算模板";
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(valueTemplate)) {
            return "压缩性能计算模板";
        }
        if (TEMPLATE_SINGLE_VALUE.equals(valueTemplate)) {
            return "单值实测模板";
        }
        return null;
    }

    private String resolveJudgmentMetric(String valueTemplate, String judgmentMetric) {
        if (StringUtils.hasText(judgmentMetric)) {
            return judgmentMetric;
        }
        if (TEMPLATE_DENSITY_CALC.equals(valueTemplate)) {
            return METRIC_DENSITY_VALUE;
        }
        if (TEMPLATE_COMPRESSION_CALC.equals(valueTemplate)) {
            return METRIC_COMPRESSION_RATE;
        }
        return METRIC_RESULT_VALUE;
    }

    private Map<String, Object> parseRawValues(String rawValuesJson) {
        if (!StringUtils.hasText(rawValuesJson)) {
            return Collections.emptyMap();
        }
        Map<String, Object> rawValues = JsonUtils.parseObjectQuietly(rawValuesJson,
                new TypeReference<Map<String, Object>>() {});
        return rawValues == null ? Collections.emptyMap() : rawValues;
    }

    private BigDecimal getDecimal(Map<String, Object> values, String key, BigDecimal defaultValue) {
        Object value = values.get(key);
        if (value == null || "".equals(value)) {
            return defaultValue;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Integer toPositiveInt(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        try {
            int result = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
            return result > 0 ? result : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isMissingOrZero(BigDecimal value) {
        return value == null || BigDecimal.ZERO.compareTo(value) == 0;
    }

    private String sampleKey(QmsFqcSampleDO sample) {
        return sampleKey(sample.getFqcItemId(), sample.getSampleSeq());
    }

    private String sampleKey(Long itemId, Integer sampleSeq) {
        return itemId + "#" + sampleSeq;
    }

    private String resolveLoginUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? "当前用户" : String.valueOf(loginUserId);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) && !"-".equals(value) ? value : defaultValue;
    }

    private static class TemplateStats {
        private final List<BigDecimal> values = new ArrayList<>();
        private BigDecimal max;
        private BigDecimal min;
        private BigDecimal avg;
        private BigDecimal std;
        private String result = JUDGMENT_PENDING;
        private int completedCount;
        private int abnormalCount;

        private void recalculate() {
            max = Collections.max(values);
            min = Collections.min(values);
            BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avg = total.divide(BigDecimal.valueOf(values.size()), CALC_SCALE, RoundingMode.HALF_UP);
            std = calculateSampleStd(values, avg);
        }

        private static BigDecimal calculateSampleStd(List<BigDecimal> values, BigDecimal avg) {
            if (values.size() <= 1) {
                return BigDecimal.ZERO.setScale(CALC_SCALE, RoundingMode.HALF_UP);
            }
            double squareSum = values.stream()
                    .map(value -> value.subtract(avg).doubleValue())
                    .mapToDouble(diff -> diff * diff)
                    .sum();
            double sampleVariance = squareSum / (values.size() - 1);
            return BigDecimal.valueOf(Math.sqrt(sampleVariance)).setScale(CALC_SCALE, RoundingMode.HALF_UP);
        }
    }
}
