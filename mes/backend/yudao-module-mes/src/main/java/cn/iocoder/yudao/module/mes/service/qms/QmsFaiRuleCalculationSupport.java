package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.util.StringUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_VALUE_TEMPLATE_CALC_INVALID;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCFAI_VALUE_TEMPLATE_REQUIRED;

/**
 * FAI 规则计算支撑：收口检测数、模板参数解析和实测值计算。
 */
final class QmsFaiRuleCalculationSupport {

    private static final String ITEM_TYPE_QUANTITATIVE = "QUANTITATIVE";
    private static final String TEMPLATE_SINGLE_VALUE = "SINGLE_VALUE";
    private static final String TEMPLATE_DENSITY_CALC = "DENSITY_CALC";
    private static final String TEMPLATE_COMPRESSION_CALC = "COMPRESSION_CALC";
    private static final String METRIC_RESULT_VALUE = "RESULT_VALUE";
    private static final String METRIC_DENSITY_VALUE = "DENSITY_VALUE";
    private static final String METRIC_COMPRESSION_RATE = "COMPRESSION_RATE";
    private static final String METRIC_COMPRESSION_ELASTICITY_RATE = "COMPRESSION_ELASTICITY_RATE";
    private static final BigDecimal DEFAULT_SAMPLE_DIAMETER_MM = BigDecimal.valueOf(39);
    private static final BigDecimal PI = BigDecimal.valueOf(3.14);
    private static final int CALC_SCALE = 6;

    private QmsFaiRuleCalculationSupport() {
    }

    static BigDecimal calculateSampleMetric(QmsFaiItemDO itemDO, QmsFaiSaveReqVO.FaiSample sample, String template) {
        Map<String, Object> rawValues = parseRawValues(sample.getRawValuesJson());
        // 压缩性能的单组展示精度为 1 位，但均值、标准差和判定必须使用原始计算精度。
        // 因此不能走 dataRule 中 precision=1 的通用取整逻辑。
        if (TEMPLATE_COMPRESSION_CALC.equals(template)) {
            return calculateCompressionMetric(itemDO, sample, rawValues);
        }
        if (QmsEntryRuleFormulaSupport.hasDataRule(itemDO.getTemplateParams())) {
            try {
                QmsEntryRuleFormulaSupport.CalculationResult result =
                        QmsEntryRuleFormulaSupport.calculate(itemDO.getTemplateParams(), rawValues);
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
                    itemDO.setJudgmentMetric(result.getJudgmentMetric());
                }
                return metric;
            } catch (IllegalArgumentException ex) {
                throw exception(HCFAI_VALUE_TEMPLATE_CALC_INVALID);
            }
        }
        if (TEMPLATE_DENSITY_CALC.equals(template)) {
            BigDecimal thicknessMm = getDecimal(rawValues, "thicknessMm", null);
            BigDecimal weightG = getDecimal(rawValues, "weightG", null);
            BigDecimal diameterMm = getDecimal(rawValues, "diameterMm",
                    getTemplateParam(itemDO, "diameterMm", DEFAULT_SAMPLE_DIAMETER_MM));
            if (isMissingOrZero(thicknessMm) || isMissingOrZero(diameterMm) || weightG == null) {
                throw exception(HCFAI_VALUE_TEMPLATE_CALC_INVALID);
            }
            BigDecimal denominator = thicknessMm
                    .divide(BigDecimal.TEN, CALC_SCALE, RoundingMode.HALF_UP)
                    .multiply(PI)
                    .multiply(diameterMm)
                    .multiply(diameterMm)
                    .divide(BigDecimal.valueOf(4), CALC_SCALE, RoundingMode.HALF_UP);
            if (isMissingOrZero(denominator)) {
                throw exception(HCFAI_VALUE_TEMPLATE_CALC_INVALID);
            }
            BigDecimal density = weightG.divide(denominator, CALC_SCALE, RoundingMode.HALF_UP);
            sample.setDensityValue(density);
            sample.setResultValue(density);
            sample.setMeasuredValue(density);
            return density;
        }
        BigDecimal value = sample.getMeasuredValue();
        if (value == null) {
            value = getDecimal(rawValues, "value", sample.getResultValue());
        }
        sample.setResultValue(value);
        sample.setMeasuredValue(value);
        return value;
    }

    static QmsEntryRuleFormulaSupport.CalculationResult calculateDataRuleQuietly(
            String templateParams, Map<String, Object> rawValues, String valueTemplate) {
        // 压缩性能计算值已按 6 位精度写入 sample，避免再次按 dataRule 的展示精度覆盖。
        if (TEMPLATE_COMPRESSION_CALC.equals(valueTemplate)) {
            return QmsEntryRuleFormulaSupport.CalculationResult.empty();
        }
        if (!QmsEntryRuleFormulaSupport.hasDataRule(templateParams)) {
            return QmsEntryRuleFormulaSupport.CalculationResult.empty();
        }
        try {
            return QmsEntryRuleFormulaSupport.calculate(templateParams, rawValues);
        } catch (IllegalArgumentException ex) {
            return QmsEntryRuleFormulaSupport.CalculationResult.empty();
        }
    }

    private static BigDecimal calculateCompressionMetric(QmsFaiItemDO itemDO, QmsFaiSaveReqVO.FaiSample sample,
                                                         Map<String, Object> rawValues) {
        BigDecimal t1Mm = getDecimal(rawValues, "t1Mm", null);
        BigDecimal t2Mm = getDecimal(rawValues, "t2Mm", null);
        BigDecimal t3Mm = getDecimal(rawValues, "t3Mm", null);
        if (isMissingOrZero(t1Mm) || t2Mm == null || t3Mm == null || t1Mm.compareTo(t2Mm) == 0) {
            throw exception(HCFAI_VALUE_TEMPLATE_CALC_INVALID);
        }
        BigDecimal compressionRate = calculatePercentage(t1Mm.subtract(t2Mm), t1Mm);
        BigDecimal compressionElasticityRate = calculatePercentage(t3Mm.subtract(t2Mm), t1Mm.subtract(t2Mm));
        sample.setCompressionRate(compressionRate);
        sample.setCompressionElasticityRate(compressionElasticityRate);
        String judgmentMetric = QmsEntryRuleFormulaSupport.resolveJudgmentMetric(
                itemDO.getTemplateParams(), itemDO.getJudgmentMetric());
        if (StringUtils.hasText(judgmentMetric)) {
            itemDO.setJudgmentMetric(judgmentMetric);
        }
        BigDecimal metric = isCompressionElasticityMetric(judgmentMetric)
                ? compressionElasticityRate : compressionRate;
        sample.setResultValue(metric);
        sample.setMeasuredValue(metric);
        return metric;
    }

    private static boolean isCompressionElasticityMetric(String judgmentMetric) {
        return METRIC_COMPRESSION_ELASTICITY_RATE.equals(judgmentMetric)
                || "compressionElasticityRate".equals(judgmentMetric);
    }

    private static BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        return numerator.divide(denominator, CALC_SCALE + 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(CALC_SCALE, RoundingMode.HALF_UP);
    }

    static boolean isMetricValueNg(QmsFaiItemDO itemDO, BigDecimal value) {
        return (itemDO.getMinValueLimit() != null && value.compareTo(itemDO.getMinValueLimit()) < 0)
                || (itemDO.getMaxValueLimit() != null && value.compareTo(itemDO.getMaxValueLimit()) > 0);
    }

    static boolean isAggregateNg(QmsFaiItemDO itemDO, BigDecimal avg, BigDecimal std) {
        return (itemDO.getAvgMinLimit() != null && avg.compareTo(itemDO.getAvgMinLimit()) < 0)
                || (itemDO.getAvgMaxLimit() != null && avg.compareTo(itemDO.getAvgMaxLimit()) > 0)
                || (itemDO.getStdMinLimit() != null && std.compareTo(itemDO.getStdMinLimit()) < 0)
                || (itemDO.getStdMaxLimit() != null && std.compareTo(itemDO.getStdMaxLimit()) > 0);
    }

    static String resolveValueTemplate(String itemType, String valueTemplate) {
        if (!ITEM_TYPE_QUANTITATIVE.equals(itemType)) {
            return null;
        }
        String template = defaultIfBlank(valueTemplate, TEMPLATE_SINGLE_VALUE);
        if (!TEMPLATE_SINGLE_VALUE.equals(template)
                && !TEMPLATE_DENSITY_CALC.equals(template)
                && !TEMPLATE_COMPRESSION_CALC.equals(template)) {
            throw exception(HCFAI_VALUE_TEMPLATE_REQUIRED);
        }
        return template;
    }

    static String resolveJudgmentMetric(String valueTemplate, String judgmentMetric) {
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

    static String resolveValueTemplateName(String valueTemplate) {
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

    static Map<String, Object> parseRawValues(String rawValuesJson) {
        if (!StringUtils.hasText(rawValuesJson)) {
            return Collections.emptyMap();
        }
        Map<String, Object> rawValues = JsonUtils.parseObjectQuietly(rawValuesJson,
                new TypeReference<Map<String, Object>>() {});
        return rawValues == null ? Collections.emptyMap() : rawValues;
    }

    static BigDecimal getTemplateParam(QmsFaiItemDO itemDO, String key, BigDecimal defaultValue) {
        return getDecimal(parseRawValues(itemDO.getTemplateParams()), key, defaultValue);
    }

    static BigDecimal getDecimal(Map<String, Object> values, String key, BigDecimal defaultValue) {
        Object value = values.get(key);
        if (value == null || "".equals(value)) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw exception(HCFAI_VALUE_TEMPLATE_CALC_INVALID);
        }
    }

    static BigDecimal toDecimal(Object value) {
        if (value == null || "".equals(value)) {
            return null;
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
            return null;
        }
    }

    static boolean isMissingOrZero(BigDecimal value) {
        return value == null || BigDecimal.ZERO.compareTo(value) == 0;
    }

    static int resolveTemplateExpectedSampleCount(QmsFaiItemDO itemDO, int fallback) {
        Map<String, Object> templateParams = parseRawValues(itemDO.getTemplateParams());
        Integer sampleSize = toPositiveInt(templateParams.get("sampleSize"));
        if (sampleSize != null) {
            return sampleSize;
        }
        Object positions = templateParams.get("positions");
        if (positions instanceof Collection<?> collection && !collection.isEmpty()) {
            Integer repeatCount = toPositiveInt(templateParams.get("repeatCount"));
            return collection.size() * (repeatCount == null ? 1 : repeatCount);
        }
        return fallback;
    }

    static Integer toPositiveInt(Object value) {
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

    private static String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
