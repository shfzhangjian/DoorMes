package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetCellValueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetStatResultDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetCellValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetSectionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetStatResultMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * FAI 样本结果统计、工作簿单元格和统计快照反写支撑。
 */
@Service
class QmsFaiSampleResultWritebackService {

    private static final String JUDGMENT_PENDING = "PENDING";
    private static final String JUDGMENT_OK = "OK";
    private static final String JUDGMENT_NG = "NG";
    private static final String ROLE_OPERATOR = "OPERATOR";
    private static final String ROLE_QA = "QA";
    private static final String ITEM_TYPE_QUANTITATIVE = "QUANTITATIVE";
    private static final String ITEM_TYPE_QUALITATIVE = "QUALITATIVE";
    private static final String VALUE_SOURCE_MANUAL = "MANUAL";
    private static final String VALUE_SOURCE_HISTORICAL_IMPORT = "HISTORICAL_IMPORT";
    private static final String VALUE_SOURCE_CALCULATED = "CALCULATED";
    private static final String CELL_STATUS_FILLED = "FILLED";
    private static final String CELL_STATUS_CALCULATED = "CALCULATED";
    private static final String FIELD_ROLE_INPUT = "INPUT";
    private static final String FIELD_ROLE_CALCULATED = "CALCULATED";
    private static final String SECTION_TYPE_GRID_SAMPLE = "GRID_SAMPLE";
    private static final String INPUT_STATUS_EMPTY = "EMPTY";
    private static final String INPUT_STATUS_FILLING = "FILLING";
    private static final String INPUT_STATUS_COMPLETE = "COMPLETE";
    private static final String INPUT_STATUS_ABNORMAL = "ABNORMAL";
    private static final long FIXED_TEMPLATE_SINGLE_ID = -1001L;
    private static final long FIXED_TEMPLATE_DENSITY_ID = -1002L;
    private static final long FIXED_TEMPLATE_COMPRESSION_ID = -1003L;
    private static final int CALC_SCALE = 6;

    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private QmsFaiSheetSectionMapper qmsFaiSheetSectionMapper;
    @Resource
    private QmsFaiSheetCellValueMapper qmsFaiSheetCellValueMapper;
    @Resource
    private QmsFaiSheetStatResultMapper qmsFaiSheetStatResultMapper;

    void applyRoleStats(QmsFaiItemDO itemDO, List<QmsFaiSaveReqVO.FaiSample> samples, String role) {
        List<QmsFaiSaveReqVO.FaiSample> roleSamples = filterSamples(samples, role);
        TemplateStats stats = calculateTemplateStats(itemDO, roleSamples);
        if (!stats.values.isEmpty()) {
            if (ROLE_OPERATOR.equals(role)) {
                itemDO.setOperatorMax(stats.max);
                itemDO.setOperatorMin(stats.min);
                itemDO.setOperatorAvg(stats.avg);
            } else {
                itemDO.setQaMax(stats.max);
                itemDO.setQaMin(stats.min);
                itemDO.setQaAvg(stats.avg);
            }
            itemDO.setCalculatedAvg(stats.avg);
            itemDO.setCalculatedStd(stats.std);
            itemDO.setCalculatedMin(stats.min);
            itemDO.setCalculatedMax(stats.max);
            itemDO.setCellRequiredCount(resolveExpectedSampleCount(itemDO));
            itemDO.setCellCompletedCount(stats.values.size());
            itemDO.setRequiredSampleCount(resolveExpectedSampleCount(itemDO));
            itemDO.setCompletedSampleCount(stats.values.size());
            itemDO.setAbnormalSampleCount((int) samples.stream()
                    .filter(sample -> role.equals(sample.getSampleRole()))
                    .filter(sample -> JUDGMENT_NG.equals(sample.getSampleResult()))
                    .count());
            itemDO.setInputStatus(JUDGMENT_NG.equals(stats.result) ? INPUT_STATUS_ABNORMAL : INPUT_STATUS_COMPLETE);
        } else {
            itemDO.setRequiredSampleCount(resolveExpectedSampleCount(itemDO));
            int qualitativeCompleted = ITEM_TYPE_QUALITATIVE.equals(itemDO.getItemType())
                    ? (int) roleSamples.stream().filter(this::isQualitativeSampleCompleted).count() : 0;
            itemDO.setCompletedSampleCount(qualitativeCompleted);
            itemDO.setAbnormalSampleCount(ITEM_TYPE_QUALITATIVE.equals(itemDO.getItemType())
                    ? (int) roleSamples.stream().filter(this::isQualitativeSampleNg).count() : 0);
            itemDO.setInputStatus(roleSamples.isEmpty() ? INPUT_STATUS_EMPTY
                    : JUDGMENT_PENDING.equals(stats.result) ? INPUT_STATUS_FILLING
                    : JUDGMENT_NG.equals(stats.result) ? INPUT_STATUS_ABNORMAL : INPUT_STATUS_COMPLETE);
        }
        if (ROLE_OPERATOR.equals(role)) {
            itemDO.setOperatorResult(stats.result);
        } else {
            itemDO.setQaResult(stats.result);
        }
    }

    void saveSheetCells(QmsFaiOrderDO order, QmsFaiItemDO itemDO, List<QmsFaiSampleDO> samples,
                        String fallbackValueSource) {
        if (order.getSheetTemplateId() == null || itemDO == null || samples == null || samples.isEmpty()) {
            return;
        }
        List<QmsFaiSheetCellValueDO> cells = new ArrayList<>();
        for (QmsFaiSampleDO sample : samples) {
            String valueSource = resolveValueSource(sample, fallbackValueSource);
            Map<String, Object> rawValues = QmsFaiRuleCalculationSupport.parseRawValues(sample.getRawValuesJson());
            rawValues.forEach((fieldCode, rawValue) -> {
                BigDecimal numericValue = QmsFaiRuleCalculationSupport.toDecimal(rawValue);
                cells.add(buildSheetCell(order, itemDO, sample, fieldCode, fieldCode,
                        FIELD_ROLE_INPUT, rawValue == null ? null : String.valueOf(rawValue),
                        numericValue, numericValue == null ? String.valueOf(rawValue) : null,
                        valueSource, CELL_STATUS_FILLED));
            });
            addCalculatedCellIfPresent(cells, order, itemDO, sample, "resultValue", "结果值",
                    sample.getResultValue(), valueSource);
            addCalculatedCellIfPresent(cells, order, itemDO, sample, "densityValue", "密度",
                    sample.getDensityValue(), valueSource);
            addCalculatedCellIfPresent(cells, order, itemDO, sample, "compressionRate", "压缩率",
                    sample.getCompressionRate(), valueSource);
            addCalculatedCellIfPresent(cells, order, itemDO, sample, "compressionElasticityRate", "压缩弹性率",
                    sample.getCompressionElasticityRate(), valueSource);
            QmsEntryRuleFormulaSupport.CalculationResult result =
                    QmsFaiRuleCalculationSupport.calculateDataRuleQuietly(
                            itemDO.getTemplateParams(), rawValues, itemDO.getValueTemplate());
            result.getResults().forEach((fieldCode, value) ->
                    addCalculatedCellIfPresent(cells, order, itemDO, sample, fieldCode, fieldCode, value, valueSource));
            if (ITEM_TYPE_QUALITATIVE.equals(itemDO.getItemType()) && StringUtils.hasText(sample.getQualitativeValue())) {
                cells.add(buildSheetCell(order, itemDO, sample, "qualitativeValue", "定性判定",
                        FIELD_ROLE_INPUT, sample.getQualitativeValue(), null, sample.getQualitativeValue(),
                        valueSource, CELL_STATUS_FILLED));
            }
        }
        if (!cells.isEmpty()) {
            qmsFaiSheetCellValueMapper.insertBatch(cells);
        }
    }

    void saveSheetStat(QmsFaiOrderDO order, QmsFaiItemDO itemDO, List<QmsFaiSaveReqVO.FaiSample> samples) {
        if (order.getSheetTemplateId() == null || itemDO == null || !ITEM_TYPE_QUANTITATIVE.equals(itemDO.getItemType())) {
            return;
        }
        Integer sampleCount = samples == null ? 0 : (int) samples.stream()
                .filter(sample -> ROLE_QA.equals(sample.getSampleRole()))
                .filter(sample -> sample.getResultValue() != null || sample.getMeasuredValue() != null)
                .count();
        QmsFaiSheetStatResultDO stat = QmsFaiSheetStatResultDO.builder()
                .faiId(order.getId())
                .faiItemId(itemDO.getId())
                .templateId(order.getSheetTemplateId())
                .sectionCode(defaultIfBlank(itemDO.getSheetSectionCode(), "UNMAPPED"))
                .metricCode(defaultIfBlank(itemDO.getSheetMetricCode(),
                        defaultIfBlank(itemDO.getInspectionItem(), "UNMAPPED")))
                .statFieldCode(defaultIfBlank(itemDO.getSheetFieldCode(), "resultValue"))
                .sampleCount(sampleCount)
                .avgValue(itemDO.getCalculatedAvg())
                .stdValue(itemDO.getCalculatedStd())
                .minValue(itemDO.getCalculatedMin() == null ? itemDO.getQaMin() : itemDO.getCalculatedMin())
                .maxValue(itemDO.getCalculatedMax() == null ? itemDO.getQaMax() : itemDO.getCalculatedMax())
                .avgMinLimit(itemDO.getAvgMinLimit())
                .avgMaxLimit(itemDO.getAvgMaxLimit())
                .stdMinLimit(itemDO.getStdMinLimit())
                .stdMaxLimit(itemDO.getStdMaxLimit())
                .judgmentResult(defaultIfBlank(itemDO.getQaResult(), JUDGMENT_PENDING))
                .calculateTime(LocalDateTime.now())
                .build();
        qmsFaiSheetStatResultMapper.insert(stat);
    }

    void rewriteSheetCellsAndStats(QmsFaiOrderDO order, Map<Long, QmsFaiItemDO> itemMap, List<Long> touchedItemIds,
                                   List<QmsFaiSampleDO> samplesForCells, String fallbackValueSource) {
        qmsFaiSheetCellValueMapper.deleteByFaiIdAndItemIds(order.getId(), touchedItemIds);
        qmsFaiSheetStatResultMapper.deleteByFaiIdAndItemIds(order.getId(), touchedItemIds);
        for (Map.Entry<Long, List<QmsFaiSampleDO>> entry : samplesForCells.stream()
                .collect(Collectors.groupingBy(QmsFaiSampleDO::getFaiItemId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()) {
            QmsFaiItemDO item = itemMap.get(entry.getKey());
            saveSheetCells(order, item, entry.getValue(), fallbackValueSource);
            saveSheetStat(order, item, BeanUtils.toBean(entry.getValue(), QmsFaiSaveReqVO.FaiSample.class));
        }
    }

    private TemplateStats calculateTemplateStats(QmsFaiItemDO itemDO, List<QmsFaiSaveReqVO.FaiSample> samples) {
        TemplateStats stats = new TemplateStats();
        if (samples == null || samples.isEmpty()) {
            stats.result = JUDGMENT_PENDING;
            return stats;
        }
        int expectedSampleCount = resolveExpectedSampleCount(itemDO);
        if (samples.size() != expectedSampleCount) {
            stats.result = JUDGMENT_PENDING;
            return stats;
        }
        if (ITEM_TYPE_QUANTITATIVE.equals(itemDO.getItemType())) {
            calculateQuantitativeSamples(itemDO, samples, stats);
            return stats;
        }
        stats.result = calculateQualitativeResult(samples);
        return stats;
    }

    private void calculateQuantitativeSamples(QmsFaiItemDO itemDO, List<QmsFaiSaveReqVO.FaiSample> samples,
                                              TemplateStats stats) {
        String template = QmsFaiRuleCalculationSupport.resolveValueTemplate(itemDO.getItemType(), itemDO.getValueTemplate());
        itemDO.setValueTemplate(template);
        if (!StringUtils.hasText(itemDO.getValueTemplateName())) {
            itemDO.setValueTemplateName(QmsFaiRuleCalculationSupport.resolveValueTemplateName(template));
        }
        itemDO.setJudgmentMetric(QmsFaiRuleCalculationSupport.resolveJudgmentMetric(template, itemDO.getJudgmentMetric()));
        boolean hasPending = false;
        for (QmsFaiSaveReqVO.FaiSample sample : samples) {
            BigDecimal metricValue = QmsFaiRuleCalculationSupport.calculateSampleMetric(itemDO, sample, template);
            if (metricValue == null) {
                hasPending = true;
                sample.setSampleResult(JUDGMENT_PENDING);
                continue;
            }
            stats.values.add(metricValue);
            sample.setSampleResult(QmsFaiRuleCalculationSupport.isMetricValueNg(itemDO, metricValue)
                    ? JUDGMENT_NG : JUDGMENT_OK);
        }
        if (stats.values.size() != resolveExpectedSampleCount(itemDO) || hasPending) {
            stats.result = JUDGMENT_PENDING;
            return;
        }
        stats.recalculate();
        stats.result = samples.stream().anyMatch(sample -> JUDGMENT_NG.equals(sample.getSampleResult()))
                || QmsFaiRuleCalculationSupport.isAggregateNg(itemDO, stats.avg, stats.std) ? JUDGMENT_NG : JUDGMENT_OK;
    }

    private void addCalculatedCellIfPresent(List<QmsFaiSheetCellValueDO> cells, QmsFaiOrderDO order,
                                            QmsFaiItemDO itemDO, QmsFaiSampleDO sample, String fieldCode,
                                            String fieldName, BigDecimal value, String originalSource) {
        if (value == null) {
            return;
        }
        cells.add(buildSheetCell(order, itemDO, sample, fieldCode, fieldName, FIELD_ROLE_CALCULATED,
                value.toPlainString(), value, null,
                VALUE_SOURCE_HISTORICAL_IMPORT.equals(originalSource) ? VALUE_SOURCE_HISTORICAL_IMPORT : VALUE_SOURCE_CALCULATED,
                CELL_STATUS_CALCULATED));
    }

    private QmsFaiSheetCellValueDO buildSheetCell(QmsFaiOrderDO order, QmsFaiItemDO itemDO, QmsFaiSampleDO sample,
                                                  String fieldCode, String fieldName, String fieldRole,
                                                  String rawValue, BigDecimal numericValue, String textValue,
                                                  String valueSource, String cellStatus) {
        return QmsFaiSheetCellValueDO.builder()
                .faiId(order.getId())
                .faiItemId(itemDO.getId())
                .templateId(order.getSheetTemplateId())
                .templateVersion(defaultIfBlank(order.getSheetTemplateVersion(), "-"))
                .sectionCode(defaultIfBlank(sample.getSheetSectionCode(),
                        defaultIfBlank(itemDO.getSheetSectionCode(), "UNMAPPED")))
                .sectionName(itemDO.getSheetSectionName())
                .metricCode(defaultIfBlank(sample.getSheetMetricCode(),
                        defaultIfBlank(itemDO.getSheetMetricCode(), "UNMAPPED")))
                .metricName(itemDO.getSheetMetricName())
                .fieldCode(fieldCode)
                .fieldName(fieldName)
                .fieldRole(fieldRole)
                .rowNo(sample.getSampleGroupNo() == null ? sample.getSampleSeq() : sample.getSampleGroupNo())
                .columnNo(sample.getSampleColumnNo() == null ? 1 : sample.getSampleColumnNo())
                .axisCode(sample.getSampleAxis())
                .sampleNo(sample.getSampleSeq())
                .displayLabel(sample.getSamplePosition())
                .rawValue(rawValue)
                .numericValue(numericValue)
                .textValue(textValue)
                .valueSource(valueSource)
                .cellStatus(cellStatus)
                .judgmentResult(sample.getSampleResult())
                .importBatchNo(sample.getImportBatchNo())
                .inputUserId(SecurityFrameworkUtils.getLoginUserId())
                .inputUserName(resolveLoginUserName())
                .inputTime(LocalDateTime.now())
                .build();
    }

    private List<QmsFaiSaveReqVO.FaiSample> filterSamples(List<QmsFaiSaveReqVO.FaiSample> samples, String role) {
        if (samples == null || samples.isEmpty()) {
            return Collections.emptyList();
        }
        return samples.stream()
                .filter(sample -> role.equals(sample.getSampleRole()))
                .collect(Collectors.toList());
    }

    private boolean isQualitativeSampleCompleted(QmsFaiSaveReqVO.FaiSample sample) {
        return JUDGMENT_OK.equals(sample.getSampleResult())
                || JUDGMENT_NG.equals(sample.getSampleResult())
                || JUDGMENT_OK.equals(sample.getQualitativeValue())
                || JUDGMENT_NG.equals(sample.getQualitativeValue());
    }

    private boolean isQualitativeSampleNg(QmsFaiSaveReqVO.FaiSample sample) {
        return JUDGMENT_NG.equals(sample.getSampleResult()) || JUDGMENT_NG.equals(sample.getQualitativeValue());
    }

    private String calculateQualitativeResult(List<QmsFaiSaveReqVO.FaiSample> samples) {
        boolean hasPending = false;
        for (QmsFaiSaveReqVO.FaiSample sample : samples) {
            String sampleResult = sample.getSampleResult();
            if (JUDGMENT_NG.equals(sampleResult) || JUDGMENT_NG.equals(sample.getQualitativeValue())) {
                return JUDGMENT_NG;
            }
            if (!JUDGMENT_OK.equals(sampleResult) && !JUDGMENT_OK.equals(sample.getQualitativeValue())) {
                hasPending = true;
            }
        }
        return hasPending ? JUDGMENT_PENDING : JUDGMENT_OK;
    }

    private int resolveExpectedSampleCount(QmsFaiItemDO itemDO) {
        int baseFallback = itemDO.getSampleSize() == null || itemDO.getSampleSize() < 1 ? 1 : itemDO.getSampleSize();
        final int fallback = QmsFaiRuleCalculationSupport.resolveTemplateExpectedSampleCount(itemDO, baseFallback);
        if (!ITEM_TYPE_QUANTITATIVE.equals(itemDO.getItemType())
                || !StringUtils.hasText(itemDO.getSheetSectionCode())
                || itemDO.getFaiId() == null) {
            return fallback;
        }
        QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(itemDO.getFaiId());
        if (order == null || order.getSheetTemplateId() == null) {
            return fallback;
        }
        FixedSection fixedSection = findFixedSection(order.getSheetTemplateId(), itemDO.getSheetSectionCode());
        if (fixedSection != null) {
            return resolveSectionExpectedSampleCount(fixedSection.sectionType, fixedSection.expectedRows,
                    fixedSection.expectedColumns, fallback);
        }
        return qmsFaiSheetSectionMapper.selectListByTemplateId(order.getSheetTemplateId())
                .stream()
                .filter(section -> itemDO.getSheetSectionCode().equals(section.getSectionCode()))
                .findFirst()
                .map(section -> resolveSectionExpectedSampleCount(section.getSectionType(),
                        section.getExpectedRows(), section.getExpectedColumns(), fallback))
                .orElse(fallback);
    }

    private FixedSection findFixedSection(Long templateId, String sectionCode) {
        if (FIXED_TEMPLATE_SINGLE_ID == templateId && "SINGLE_VALUE".equals(sectionCode)) {
            return new FixedSection("NORMAL_SAMPLE", 15, 1);
        }
        if (FIXED_TEMPLATE_DENSITY_ID == templateId && "DENSITY".equals(sectionCode)) {
            return new FixedSection("CALC_SAMPLE", 15, 2);
        }
        if (FIXED_TEMPLATE_COMPRESSION_ID == templateId && "COMPRESSION".equals(sectionCode)) {
            return new FixedSection("CALC_SAMPLE", 15, 3);
        }
        return null;
    }

    private int resolveSectionExpectedSampleCount(String sectionType, Integer expectedRows,
                                                  Integer expectedColumns, int fallback) {
        int rows = expectedRows == null || expectedRows < 1 ? fallback : expectedRows;
        int columns = expectedColumns == null || expectedColumns < 1 ? 1 : expectedColumns;
        return SECTION_TYPE_GRID_SAMPLE.equals(sectionType) ? rows * columns : rows;
    }

    private String resolveValueSource(QmsFaiSampleDO sample, String fallbackValueSource) {
        if (StringUtils.hasText(sample.getValueSource())) {
            return sample.getValueSource();
        }
        if (StringUtils.hasText(sample.getImportBatchNo())) {
            return VALUE_SOURCE_HISTORICAL_IMPORT;
        }
        return defaultIfBlank(fallbackValueSource, VALUE_SOURCE_MANUAL);
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

    private record FixedSection(String sectionType, Integer expectedRows, Integer expectedColumns) {
    }

    private static class TemplateStats {
        private final List<BigDecimal> values = new ArrayList<>();
        private BigDecimal max;
        private BigDecimal min;
        private BigDecimal avg;
        private BigDecimal std;
        private String result = JUDGMENT_PENDING;

        private void recalculate() {
            max = Collections.max(values);
            min = Collections.min(values);
            BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avg = total.divide(BigDecimal.valueOf(values.size()), CALC_SCALE, RoundingMode.HALF_UP);
            std = calculateSampleStd(values, avg);
        }

    }

    static BigDecimal calculateSampleStd(List<BigDecimal> values, BigDecimal avg) {
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
