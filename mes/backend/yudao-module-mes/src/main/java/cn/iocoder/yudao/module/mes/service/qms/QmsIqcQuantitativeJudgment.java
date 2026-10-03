package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** IQC 定量判定只使用单据标准快照，不接受客户端传入的合格结论。 */
final class QmsIqcQuantitativeJudgment {
    static final String SKIP = "SKIP";

    private QmsIqcQuantitativeJudgment() { }

    static String evaluate(QmsIqcItemDO item, List<QmsIqcSaveReqVO.IqcSample> samples) {
        boolean hasSingleLimit = item.getMinValueLimit() != null || item.getMaxValueLimit() != null;
        boolean hasAverageLimit = item.getAvgMinLimit() != null || item.getAvgMaxLimit() != null;
        if (reversed(item.getMinValueLimit(), item.getMaxValueLimit())
                || reversed(item.getAvgMinLimit(), item.getAvgMaxLimit())) {
            item.setJudgmentReason("判定标准配置错误：下限大于上限");
            return "PENDING";
        }
        int expected = item.getSampleSize() == null || item.getSampleSize() <= 0 ? 1 : item.getSampleSize();
        Set<Integer> sequences = new HashSet<>();
        boolean complete = samples != null && samples.size() == expected;
        BigDecimal sum = BigDecimal.ZERO;
        List<String> reasons = new ArrayList<>();
        if (samples != null) {
            for (QmsIqcSaveReqVO.IqcSample sample : samples) {
                Integer seq = sample.getSampleSeq();
                BigDecimal value = sample.getResultValue() != null ? sample.getResultValue() : sample.getMeasuredValue();
                if (seq == null || seq < 1 || seq > expected || !sequences.add(seq) || value == null) {
                    complete = false;
                }
                if (value == null) {
                    sample.setSampleResult("PENDING");
                    continue;
                }
                sum = sum.add(value);
                String violation = violation(value, item.getMinValueLimit(), item.getMaxValueLimit());
                sample.setSampleResult(hasSingleLimit ? (violation == null ? "OK" : "NG") : SKIP);
                if (violation != null && reasons.isEmpty()) {
                    reasons.add("样本" + seq + "实测值 " + number(value) + unit(item) + "，" + violation);
                }
            }
        }
        // 不用部分样本平均值判断，也不让缺测数据成为最终合格/不合格结论。
        if (!complete) {
            item.setJudgmentReason("检测数据未填写完整或样本序号重复，请按要求填写 " + expected + " 个样本");
            return "PENDING";
        }
        if (!hasSingleLimit && !hasAverageLimit) {
            item.setJudgmentReason("未配置数值判定标准，仅记录检测数据，不参与合格判定");
            samples.forEach(sample -> sample.setSampleResult(SKIP));
            return SKIP;
        }
        BigDecimal count = BigDecimal.valueOf(expected);
        // 以总和对比限值乘样本数，避免平均值显示舍入掩盖边界超限。
        String avgViolation = null;
        if (item.getAvgMinLimit() != null && sum.compareTo(item.getAvgMinLimit().multiply(count)) < 0) {
            avgViolation = "低于平均值内控下限 " + number(item.getAvgMinLimit());
        } else if (item.getAvgMaxLimit() != null && sum.compareTo(item.getAvgMaxLimit().multiply(count)) > 0) {
            avgViolation = "超过平均值内控上限 " + number(item.getAvgMaxLimit());
        }
        if (avgViolation != null) {
            reasons.add("平均值 " + number(sum.divide(count, 12, RoundingMode.HALF_UP)) + unit(item) + "，" + avgViolation);
        }
        item.setJudgmentReason(String.join("；", reasons));
        return reasons.isEmpty() ? "OK" : "NG";
    }

    static String range(BigDecimal min, BigDecimal max) {
        return (min == null ? "未设置" : number(min)) + " ～ " + (max == null ? "未设置" : number(max));
    }

    private static boolean reversed(BigDecimal min, BigDecimal max) {
        return min != null && max != null && min.compareTo(max) > 0;
    }

    private static String violation(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (min != null && value.compareTo(min) < 0) return "低于单次下限 " + number(min);
        if (max != null && value.compareTo(max) > 0) return "超过单次上限 " + number(max);
        return null;
    }

    private static String number(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String unit(QmsIqcItemDO item) {
        return item.getUnit() == null ? "" : " " + item.getUnit();
    }
}
