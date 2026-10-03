package cn.iocoder.yudao.module.mes.service.hc.qtimeconfig;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeEvaluationRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.qtimeconfig.HcQtimeConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.qtimeconfig.HcQtimeConfigMapper;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcQtimeEvaluationService {

    public static final String RULE_FORMULA_TO_WET = "FORMULA_END_TO_WET_START";
    public static final String RULE_WET_TO_GRINDING = "WET_END_TO_GRINDING_START";
    public static final String RULE_FIRST_GRINDING_TO_SECOND_GRINDING = "FIRST_GRINDING_END_TO_SECOND_GRINDING_START";
    public static final String RULE_GRINDING_TO_ADHESIVE = "GRINDING_END_TO_ADHESIVE_START";
    public static final String RULE_ADHESIVE_TO_SLITTING = "ADHESIVE_END_TO_SLITTING_START";
    public static final String RULE_SLITTING_TO_PRESS_SLOT = "SLITTING_END_TO_PRESS_SLOT_START";
    public static final String RULE_PRESS_SLOT_TO_ADHESIVE2 = "PRESS_SLOT_END_TO_ADHESIVE2_START";
    public static final String RULE_ADHESIVE2_TO_CUT_ROUND = "ADHESIVE2_END_TO_CUT_ROUND_START";
    public static final String RULE_CUT_ROUND_TO_FQC = "CUT_ROUND_END_TO_FQC_START";

    @Resource
    private HcQtimeConfigMapper hcQtimeConfigMapper;

    public HcQtimeEvaluationRespVO evaluateFormulaToWet(LocalDateTime formulaEndTime,
                                                        LocalDateTime wetStartTime,
                                                        String... modelCandidates) {
        return evaluate(RULE_FORMULA_TO_WET, formulaEndTime, wetStartTime, modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluateWetToGrinding(LocalDateTime wetEndTime,
                                                         LocalDateTime grindingStartTime,
                                                         String... modelCandidates) {
        return evaluate(RULE_WET_TO_GRINDING, wetEndTime, grindingStartTime, modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluateFirstGrindingToSecondGrinding(LocalDateTime firstGrindingEndTime,
                                                                           LocalDateTime secondGrindingStartTime,
                                                                           String... modelCandidates) {
        return evaluate(RULE_FIRST_GRINDING_TO_SECOND_GRINDING, firstGrindingEndTime, secondGrindingStartTime,
                modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluateGrindingToAdhesive(LocalDateTime grindingEndTime,
                                                              LocalDateTime adhesiveStartTime,
                                                              String... modelCandidates) {
        return evaluate(RULE_GRINDING_TO_ADHESIVE, grindingEndTime, adhesiveStartTime, modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluateAdhesiveToSlitting(LocalDateTime adhesiveEndTime,
                                                               LocalDateTime slittingStartTime,
                                                               String... modelCandidates) {
        return evaluate(RULE_ADHESIVE_TO_SLITTING, adhesiveEndTime, slittingStartTime, modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluateSlittingToPressSlot(LocalDateTime slittingEndTime,
                                                                LocalDateTime pressSlotStartTime,
                                                                String... modelCandidates) {
        return evaluate(RULE_SLITTING_TO_PRESS_SLOT, slittingEndTime, pressSlotStartTime, modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluatePressSlotToAdhesive2(LocalDateTime pressSlotEndTime,
                                                                 LocalDateTime adhesive2StartTime,
                                                                 String... modelCandidates) {
        return evaluate(RULE_PRESS_SLOT_TO_ADHESIVE2, pressSlotEndTime, adhesive2StartTime, modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluateAdhesive2ToCutRound(LocalDateTime adhesive2EndTime,
                                                                LocalDateTime cutRoundStartTime,
                                                                String... modelCandidates) {
        return evaluate(RULE_ADHESIVE2_TO_CUT_ROUND, adhesive2EndTime, cutRoundStartTime, modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluateCutRoundToFqc(LocalDateTime cutRoundEndTime,
                                                          LocalDateTime fqcStartTime,
                                                          String... modelCandidates) {
        return evaluate(RULE_CUT_ROUND_TO_FQC, cutRoundEndTime, fqcStartTime, modelCandidates);
    }

    public HcQtimeEvaluationRespVO evaluate(String ruleType,
                                            LocalDateTime sourceEndTime,
                                            LocalDateTime targetStartTime,
                                            String... modelCandidates) {
        HcQtimeEvaluationRespVO result = new HcQtimeEvaluationRespVO();
        result.setRuleType(ruleType);
        result.setSourceEndTime(sourceEndTime);
        result.setTargetStartTime(targetStartTime);
        result.setTargetStarted(targetStartTime != null);
        result.setTimeout(false);

        HcQtimeConfigDO config = matchConfig(modelCandidates);
        Integer standardMinutes = resolveStandardMinutes(config, ruleType);
        if (config == null || standardMinutes == null || standardMinutes <= 0) {
            result.setStatus("NO_RULE");
            result.setMessage("未配置额定 QTIME");
            return result;
        }
        result.setModelPrefix(config.getModelPrefix());
        result.setStandardMinutes(standardMinutes);
        if (sourceEndTime == null) {
            if (targetStartTime != null) {
                result.setElapsedMinutes(0);
                result.setStatus("WAITING_SOURCE_FINISH");
                result.setMessage("下道已开工，等待上道完工后回算 QTIME");
                return result;
            }
            result.setStatus("MISSING_SOURCE_TIME");
            result.setMessage("上道完工时间缺失");
            return result;
        }

        LocalDateTime targetTime = targetStartTime == null ? LocalDateTime.now() : targetStartTime;
        long elapsedSeconds = Math.max(0L, Duration.between(sourceEndTime, targetTime).getSeconds());
        long elapsed = elapsedSeconds / 60;
        result.setElapsedMinutes((int) Math.min(Integer.MAX_VALUE, elapsed));
        boolean timeout = elapsed > standardMinutes;
        result.setTimeout(timeout);
        result.setStatus(timeout ? "TIMEOUT" : "NORMAL");
        result.setMessage(buildMessage(ruleType, timeout, targetStartTime != null, result.getElapsedMinutes(), standardMinutes));
        return result;
    }

    private HcQtimeConfigDO matchConfig(String... candidates) {
        List<HcQtimeConfigDO> configs = hcQtimeConfigMapper.selectEnabledList().stream()
                .filter(item -> StrUtil.isNotBlank(item.getModelPrefix()))
                .sorted(Comparator
                        .comparingInt((HcQtimeConfigDO item) -> item.getModelPrefix().length())
                        .reversed()
                        .thenComparing(HcQtimeConfigDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        for (String candidate : candidates) {
            String normalized = normalizeCandidate(candidate);
            if (StrUtil.isBlank(normalized)) {
                continue;
            }
            for (HcQtimeConfigDO config : configs) {
                String prefix = normalizeCandidate(config.getModelPrefix());
                if (StrUtil.isNotBlank(prefix) && normalized.startsWith(prefix)) {
                    return config;
                }
            }
        }
        return null;
    }

    private Integer resolveStandardMinutes(HcQtimeConfigDO config, String ruleType) {
        if (config == null) {
            return null;
        }
        if (RULE_FORMULA_TO_WET.equals(ruleType)) {
            return config.getFormulaToWetMinutes();
        }
        if (RULE_WET_TO_GRINDING.equals(ruleType)) {
            return config.getWetToGrindingMinutes();
        }
        if (RULE_FIRST_GRINDING_TO_SECOND_GRINDING.equals(ruleType)) {
            return config.getFirstGrindingToSecondMinutes();
        }
        if (RULE_GRINDING_TO_ADHESIVE.equals(ruleType)) {
            return config.getGrindingToAdhesiveMinutes();
        }
        if (RULE_ADHESIVE_TO_SLITTING.equals(ruleType)) {
            return config.getAdhesiveToSlittingMinutes();
        }
        if (RULE_SLITTING_TO_PRESS_SLOT.equals(ruleType)) {
            return config.getSlittingToPressSlotMinutes();
        }
        if (RULE_PRESS_SLOT_TO_ADHESIVE2.equals(ruleType)) {
            return config.getPressSlotToAdhesive2Minutes();
        }
        if (RULE_ADHESIVE2_TO_CUT_ROUND.equals(ruleType)) {
            return config.getAdhesive2ToCutRoundMinutes();
        }
        if (RULE_CUT_ROUND_TO_FQC.equals(ruleType)) {
            return config.getCutRoundToFqcMinutes();
        }
        return null;
    }

    private String normalizeCandidate(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private String buildMessage(String ruleType, boolean timeout, boolean targetStarted,
                                Integer elapsedMinutes, Integer standardMinutes) {
        String actionText;
        if (RULE_FORMULA_TO_WET.equals(ruleType)) {
            actionText = "湿法开工";
        } else if (RULE_WET_TO_GRINDING.equals(ruleType)) {
            actionText = "磨皮开工";
        } else if (RULE_FIRST_GRINDING_TO_SECOND_GRINDING.equals(ruleType)) {
            actionText = "二次磨皮开工";
        } else if (RULE_GRINDING_TO_ADHESIVE.equals(ruleType)) {
            actionText = "粘胶1开工";
        } else if (RULE_ADHESIVE_TO_SLITTING.equals(ruleType)) {
            actionText = "分切开工";
        } else if (RULE_SLITTING_TO_PRESS_SLOT.equals(ruleType)) {
            actionText = "压槽开工";
        } else if (RULE_PRESS_SLOT_TO_ADHESIVE2.equals(ruleType)) {
            actionText = "粘胶2开工";
        } else if (RULE_ADHESIVE2_TO_CUT_ROUND.equals(ruleType)) {
            actionText = "裁切开工";
        } else if (RULE_CUT_ROUND_TO_FQC.equals(ruleType)) {
            actionText = "成品检验开工";
        } else {
            actionText = "下道开工";
        }
        String stateText = targetStarted ? "实际" : "当前";
        String elapsedText = formatMinutes(elapsedMinutes);
        String standardText = formatMinutes(standardMinutes);
        if (timeout) {
            return String.format("%s%s间隔 %s，已超出额定 %s", stateText, actionText, elapsedText, standardText);
        }
        return String.format("%s%s间隔 %s，额定 %s 内", stateText, actionText, elapsedText, standardText);
    }

    private String formatMinutes(Integer minutes) {
        if (minutes == null) {
            return "-";
        }
        int safeMinutes = Math.max(0, minutes);
        int hours = safeMinutes / 60;
        int remainMinutes = safeMinutes % 60;
        if (hours > 0 && remainMinutes > 0) {
            return hours + "小时" + remainMinutes + "分钟";
        }
        if (hours > 0) {
            return hours + "小时";
        }
        return safeMinutes + "分钟";
    }

}
