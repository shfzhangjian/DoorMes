package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 检验标准内容哈希。仅比较会固化到检验记录中的执行字段，不依赖标准ID或业务版本号。
 */
@Service
public class QmsQualityStandardContentHashService {

    public String hashIqcStandardItems(List<QmsQualityStandardItemDO> items) {
        List<Map<String, Object>> comparable = new ArrayList<>();
        List<QmsQualityStandardItemDO> sorted = sortedStandards(items);
        for (int index = 0; index < sorted.size(); index++) {
            QmsQualityStandardItemDO item = sorted.get(index);
            Map<String, Object> row = commonStandardRow(item, effectiveSort(item.getSort(), index));
            row.put("expiryDays", item.getExpiryDays());
            row.put("entryRuleType", text(item.getEntryRuleType()));
            comparable.add(row);
        }
        return sha256(JsonUtils.toJsonString(comparable));
    }

    public String hashIqcSnapshotItems(List<QmsIqcItemDO> items) {
        List<Map<String, Object>> comparable = new ArrayList<>();
        sortedIqcItems(items).forEach(item -> {
            Map<String, Object> row = commonSnapshotRow(item.getSort(), item.getInspectionItem(),
                    item.getItemType(), item.getAttachmentEnabled(), item.getTargetValue(),
                    item.getStandardDesc(), item.getUnit(), item.getInspectionMethod(),
                    item.getTestFrequencyJudgement(), item.getRuleDescription(), item.getValueTemplate(),
                    item.getTemplateParams(), item.getTestTool(), item.getSampleSize(),
                    item.getMinValueLimit(), item.getMaxValueLimit(), item.getIsSpc());
            row.put("expiryDays", item.getExpiryDays());
            row.put("entryRuleType", text(item.getEntryRuleType()));
            comparable.add(row);
        });
        return sha256(JsonUtils.toJsonString(comparable));
    }

    public String hashFaiStandardItems(List<QmsQualityStandardItemDO> items) {
        List<Map<String, Object>> comparable = new ArrayList<>();
        List<QmsQualityStandardItemDO> sorted = sortedStandards(items);
        for (int index = 0; index < sorted.size(); index++) {
            QmsQualityStandardItemDO item = sorted.get(index);
            Map<String, Object> row = commonStandardRow(item, effectiveSort(item.getSort(), index));
            String template = QmsFaiRuleCalculationSupport.resolveValueTemplate(
                    item.getItemType(), item.getValueTemplate());
            row.put("valueTemplate", text(template));
            row.put("judgmentMetric", text(QmsFaiRuleCalculationSupport.resolveJudgmentMetric(
                    template, item.getJudgmentMetric())));
            row.put("avgMinLimit", decimal(item.getAvgMinLimit()));
            row.put("avgMaxLimit", decimal(item.getAvgMaxLimit()));
            row.put("stdMinLimit", decimal(item.getStdMinLimit()));
            row.put("stdMaxLimit", decimal(item.getStdMaxLimit()));
            row.put("sheetSectionCode", text(item.getSheetSectionCode()));
            row.put("sheetMetricCode", text(item.getSheetMetricCode()));
            row.put("sheetFieldCode", text(item.getSheetFieldCode()));
            comparable.add(row);
        }
        return sha256(JsonUtils.toJsonString(comparable));
    }

    public String hashFaiSnapshotItems(List<QmsFaiItemDO> items) {
        List<Map<String, Object>> comparable = new ArrayList<>();
        sortedFaiItems(items).forEach(item -> {
            Map<String, Object> row = commonSnapshotRow(item.getSort(), item.getInspectionItem(),
                    item.getItemType(), item.getAttachmentEnabled(), item.getTargetValue(),
                    item.getStandardDesc(), item.getUnit(), item.getInspectionMethod(),
                    item.getTestFrequencyJudgement(), item.getRuleDescription(), item.getValueTemplate(),
                    item.getTemplateParams(), item.getTestTool(), item.getSampleSize(),
                    item.getMinValueLimit(), item.getMaxValueLimit(), item.getIsSpc());
            row.put("judgmentMetric", text(item.getJudgmentMetric()));
            row.put("avgMinLimit", decimal(item.getAvgMinLimit()));
            row.put("avgMaxLimit", decimal(item.getAvgMaxLimit()));
            row.put("stdMinLimit", decimal(item.getStdMinLimit()));
            row.put("stdMaxLimit", decimal(item.getStdMaxLimit()));
            row.put("sheetSectionCode", text(item.getSheetSectionCode()));
            row.put("sheetMetricCode", text(item.getSheetMetricCode()));
            row.put("sheetFieldCode", text(item.getSheetFieldCode()));
            comparable.add(row);
        });
        return sha256(JsonUtils.toJsonString(comparable));
    }

    private Map<String, Object> commonStandardRow(QmsQualityStandardItemDO item, Integer sort) {
        return commonSnapshotRow(sort, item.getInspectionItem(), item.getItemType(),
                item.getAttachmentEnabled(), item.getTargetValue(), item.getStandardDesc(), item.getUnit(),
                item.getInspectionMethod(), item.getTestFrequencyJudgement(), item.getRuleDescription(),
                item.getValueTemplate(), item.getTemplateParams(), item.getTestTool(), item.getSampleSize(),
                item.getMinValue(), item.getMaxValue(), item.getIsSpc());
    }

    private Integer effectiveSort(Integer sort, int zeroBasedIndex) {
        return sort == null ? (zeroBasedIndex + 1) * 10 : sort;
    }

    private Map<String, Object> commonSnapshotRow(Integer sort, String inspectionItem, String itemType,
                                                   Boolean attachmentEnabled, BigDecimal targetValue,
                                                   String standardDesc, String unit, String inspectionMethod,
                                                   String testFrequencyJudgement, String ruleDescription,
                                                   String valueTemplate, String templateParams, String testTool,
                                                   Integer sampleSize, BigDecimal minValue, BigDecimal maxValue,
                                                   Boolean isSpc) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sort", sort);
        row.put("inspectionItem", text(inspectionItem));
        row.put("itemType", text(itemType));
        row.put("attachmentEnabled", Boolean.TRUE.equals(attachmentEnabled));
        row.put("targetValue", decimal(targetValue));
        row.put("standardDesc", text(standardDesc));
        row.put("unit", text(unit));
        row.put("inspectionMethod", text(inspectionMethod));
        row.put("testFrequencyJudgement", text(testFrequencyJudgement));
        row.put("ruleDescription", text(ruleDescription));
        row.put("valueTemplate", text(valueTemplate));
        row.put("templateParams", canonicalJson(templateParams));
        row.put("testTool", text(testTool));
        row.put("sampleSize", sampleSize == null || sampleSize < 1 ? 1 : sampleSize);
        row.put("minValue", decimal(minValue));
        row.put("maxValue", decimal(maxValue));
        row.put("isSpc", Boolean.TRUE.equals(isSpc));
        return row;
    }

    private List<QmsQualityStandardItemDO> sortedStandards(List<QmsQualityStandardItemDO> items) {
        List<QmsQualityStandardItemDO> sorted = new ArrayList<>(items == null ? List.of() : items);
        sorted.sort(Comparator.comparing(QmsQualityStandardItemDO::getSort,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(QmsQualityStandardItemDO::getInspectionItem,
                        Comparator.nullsLast(String::compareTo)));
        return sorted;
    }

    private List<QmsIqcItemDO> sortedIqcItems(List<QmsIqcItemDO> items) {
        List<QmsIqcItemDO> sorted = new ArrayList<>(items == null ? List.of() : items);
        sorted.sort(Comparator.comparing(QmsIqcItemDO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(QmsIqcItemDO::getInspectionItem, Comparator.nullsLast(String::compareTo)));
        return sorted;
    }

    private List<QmsFaiItemDO> sortedFaiItems(List<QmsFaiItemDO> items) {
        List<QmsFaiItemDO> sorted = new ArrayList<>(items == null ? List.of() : items);
        sorted.sort(Comparator.comparing(QmsFaiItemDO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(QmsFaiItemDO::getInspectionItem, Comparator.nullsLast(String::compareTo)));
        return sorted;
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String canonicalJson(String json) {
        if (!StringUtils.hasText(json)) {
            return "";
        }
        try {
            return JsonUtils.toJsonString(JsonUtils.parseTree(json));
        } catch (Exception ignored) {
            return text(json);
        }
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
