package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisExportVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisSourcePreviewRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldAnalysisSourceRow;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldAnalysisSourcePreviewRow;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldTargetConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsYieldAnalysisMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsYieldTargetConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class QmsYieldAnalysisServiceImpl implements QmsYieldAnalysisService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFECT_BLACK_DOT = "黑点";
    private static final String DEFECT_BLUE_DOT = "蓝点";
    private static final String DEFECT_YELLOW_DOT = "黄点";
    private static final String DEFECT_RED_DOT = "红点";
    private static final String DEFECT_PINHOLE = "针孔";
    private static final String DEFECT_STRIPE = "条纹";
    private static final String DEFECT_WRINKLE = "褶皱";
    private static final String DEFECT_WAVE = "波浪纹";
    private static final String DEFECT_OTHER = "其他";
    private static final List<String> DEFECT_NAMES = List.of(
            DEFECT_BLACK_DOT, DEFECT_BLUE_DOT, DEFECT_YELLOW_DOT, DEFECT_RED_DOT,
            DEFECT_PINHOLE, DEFECT_STRIPE, DEFECT_WRINKLE, DEFECT_WAVE, DEFECT_OTHER);
    private static final Map<String, Integer> PROCESS_SORT = Map.of(
            "FORMULA", 10,
            "WET", 20,
            "ROUGH_GRINDING", 30,
            "ADHESIVE1", 40,
            "SLITTING", 50,
            "PRESS_SLOT", 60,
            "ADHESIVE2", 70,
            "CUT_ROUND", 80,
            "FINAL_INSPECTION", 90);
    private static final String TARGET_TYPE_OUTPUT_QTY = "OUTPUT_QTY";
    private static final String TARGET_TYPE_YIELD_RATE = "YIELD_RATE";
    private static final String W33_MODEL_CODE = "W33";
    private static final Set<String> PIECE_OUTPUT_PROCESS_CODES = Set.of(
            "SLITTING", "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Pattern SEGMENT_BATCH_PATTERN =
            Pattern.compile("^([A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADHESIVE_SEGMENT_BATCH_PATTERN =
            Pattern.compile("^([A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS])-J[0-9]+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PIECE_BATCH_PATTERN =
            Pattern.compile("^([A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS])[0-9]{3}[A-Z]?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOTHER_FROM_SEGMENT_PATTERN =
            Pattern.compile("^([A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z])[PQRS]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEGMENT_MARK_PATTERN =
            Pattern.compile("^[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]([PQRS])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFECT_SUMMARY_ITEM_PATTERN = Pattern.compile("^(.+?)(\\d+)$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SOURCE_PREVIEW_PATH = "/mes/quality/statistics/yield-analysis/source-preview";
    private static final String SOURCE_TABLE_OPERATION_REPORT = "mes_sfc_operation_report";
    private static final String SOURCE_TABLE_GRINDING_SECOND = "mes_sfc_grinding_second_detail";
    private static final String SOURCE_TABLE_ADHESIVE1 = "mes_sfc_adhesive_report";
    private static final String SOURCE_TABLE_SLITTING = "mes_sfc_slitting_slice_record";
    private static final String SOURCE_TABLE_PRESS_SLOT = "mes_sfc_press_slot_report";
    private static final String SOURCE_TABLE_ADHESIVE2 = "mes_sfc_adhesive2_report";
    private static final String SOURCE_TABLE_CUT_ROUND = "mes_sfc_cut_round_report";
    private static final String SOURCE_TABLE_FQC_SUBMISSION_DETAIL = "mes_qms_fqc_submission_detail";

    @Resource
    private QmsYieldAnalysisMapper qmsYieldAnalysisMapper;
    @Resource
    private QmsYieldTargetConfigMapper qmsYieldTargetConfigMapper;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public QmsYieldAnalysisRespVO getOverview(QmsYieldAnalysisReqVO reqVO) {
        return buildOverview(reqVO, loadDetails(reqVO));
    }

    @Override
    public QmsYieldAnalysisRespVO getOverviewFromSourceRows(QmsYieldAnalysisReqVO reqVO,
                                                            List<QmsYieldAnalysisSourceRow> sourceRows) {
        return buildOverview(reqVO, convertSourceRows(reqVO, sourceRows));
    }

    @Override
    public PageResult<QmsYieldAnalysisDetailRespVO> getDetailPageFromSourceRows(
            QmsYieldAnalysisReqVO reqVO,
            List<QmsYieldAnalysisSourceRow> sourceRows) {
        return pageDetails(reqVO, convertSourceRows(reqVO, sourceRows));
    }

    private QmsYieldAnalysisRespVO buildOverview(QmsYieldAnalysisReqVO reqVO,
                                                 List<QmsYieldAnalysisDetailRespVO> details) {
        Map<String, SummaryAccumulator> summaryMap = new LinkedHashMap<>();
        Map<String, Integer> defectTotals = initDefectCounter();
        Map<String, TrendAccumulator> trendMap = new TreeMap<>();

        for (QmsYieldAnalysisDetailRespVO detail : details) {
            summaryMap.computeIfAbsent(detail.getGroupKey(), key -> new SummaryAccumulator(detail)).add(detail);
            addDefectCounts(defectTotals, detail);
            String statDate = firstLeft(detail.getConfirmTime(), 10);
            if (hasText(statDate)) {
                trendMap.computeIfAbsent(statDate, TrendAccumulator::new).add(detail);
            }
        }

        List<QmsYieldAnalysisRespVO.SummaryRow> summaryRows = summaryMap.values().stream()
                .map(SummaryAccumulator::toRow)
                .sorted(summaryComparator())
                .collect(Collectors.toList());
        applyTargetConfigs(summaryRows);
        List<QmsYieldAnalysisRespVO.SegmentRow> segmentRows = buildSegmentRows(summaryRows);
        List<QmsYieldAnalysisRespVO.DefectDistribution> defectDistribution = buildDefectDistribution(defectTotals);
        List<QmsYieldAnalysisRespVO.TrendPoint> trendRows = trendMap.values().stream()
                .map(TrendAccumulator::toRow)
                .collect(Collectors.toList());

        int confirmedTotal = details.size();
        BigDecimal inputTotal = details.stream()
                .map(QmsYieldAnalysisDetailRespVO::getInputCount)
                .map(QmsYieldAnalysisServiceImpl::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outputGoodTotal = details.stream()
                .map(QmsYieldAnalysisDetailRespVO::getOutputGoodCount)
                .map(QmsYieldAnalysisServiceImpl::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outputNgTotal = details.stream()
                .map(QmsYieldAnalysisDetailRespVO::getOutputNgCount)
                .map(QmsYieldAnalysisServiceImpl::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int inspectionTotal = details.stream().mapToInt(detail -> defaultInt(detail.getInspectionCount())).sum();
        BigDecimal ngTotal = outputNgTotal;
        int selfCheckNgTotal = details.stream().mapToInt(QmsYieldAnalysisDetailRespVO::getSelfCheckNgCount).sum();
        int submissionNgTotal = details.stream().mapToInt(QmsYieldAnalysisDetailRespVO::getSubmissionNgCount).sum();
        QmsYieldAnalysisRespVO.DefectDistribution primaryDefect = defectDistribution.stream()
                .filter(row -> row.getDefectCount() != null && row.getDefectCount() > 0)
                .findFirst()
                .orElse(null);
        QmsYieldTargetConfigDO overviewTargetConfig = resolveOverviewTargetConfig(reqVO);
        String overviewTargetType = overviewTargetConfig == null ? null
                : normalizeTargetType(overviewTargetConfig.getTargetType(), overviewTargetConfig.getProcessCode());

        return QmsYieldAnalysisRespVO.builder()
                .overview(QmsYieldAnalysisRespVO.Overview.builder()
                        .confirmedTotal(confirmedTotal)
                        .goodTotal(outputGoodTotal)
                        .ngTotal(ngTotal)
                        .selfCheckNgTotal(selfCheckNgTotal)
                        .submissionNgTotal(submissionNgTotal)
                        .inputTotal(inputTotal)
                        .outputGoodTotal(outputGoodTotal)
                        .outputNgTotal(outputNgTotal)
                        .inspectionTotal(inspectionTotal)
                        .yieldRate(calculateYieldRate(inputTotal, outputGoodTotal))
                        .targetQualifiedQty(overviewTargetConfig == null ? null : overviewTargetConfig.getTargetQualifiedQty())
                        .targetUnit(overviewTargetConfig == null ? null : resolveTargetUnit(overviewTargetConfig, overviewTargetType))
                        .targetType(overviewTargetType)
                        .targetModelCode(overviewTargetConfig == null ? null : overviewTargetConfig.getModelCode())
                        .targetProcessName(overviewTargetConfig == null ? null
                                : firstNotBlank(overviewTargetConfig.getProcessName(),
                                resolveProcessName(overviewTargetConfig.getProcessCode())))
                        .primaryDefectName(primaryDefect == null ? "-" : primaryDefect.getDefectName())
                        .primaryDefectCount(primaryDefect == null ? 0 : primaryDefect.getDefectCount())
                        .build())
                .summaryRows(summaryRows)
                .segmentRows(segmentRows)
                .defectDistribution(defectDistribution)
                .trendRows(trendRows)
                .formulaText("良品率 = 产出良品数 / 投入数 * 100%；配料到粘胶1按产出值绝对值达标，分切、压槽、粘胶2、裁切按片数达标。")
                .formulaExample("例：W26分切理论产量106片，当前产出良品96片，则目标差异=96片-106片=-10片，该行未达标。")
                .build();
    }

    @Override
    public List<String> getTargetModelOptions() {
        List<String> modelCodes = qmsYieldTargetConfigMapper.selectEnabledModelCodes(TenantContextHolder.getTenantId());
        if (modelCodes == null || modelCodes.isEmpty()) {
            return List.of();
        }
        return modelCodes.stream()
                .filter(QmsYieldAnalysisServiceImpl::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<QmsYieldAnalysisDetailRespVO> getDetailPage(QmsYieldAnalysisReqVO reqVO) {
        return pageDetails(reqVO, loadDetails(reqVO));
    }

    private PageResult<QmsYieldAnalysisDetailRespVO> pageDetails(
            QmsYieldAnalysisReqVO reqVO,
            List<QmsYieldAnalysisDetailRespVO> details) {
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        int fromIndex = Math.min((pageNo - 1) * pageSize, details.size());
        int toIndex = Math.min(fromIndex + pageSize, details.size());
        return new PageResult<>(new ArrayList<>(details.subList(fromIndex, toIndex)), (long) details.size());
    }

    @Override
    public List<QmsYieldAnalysisExportVO> getExportList(QmsYieldAnalysisReqVO reqVO) {
        return getOverview(reqVO).getSummaryRows().stream()
                .map(this::toExportRow)
                .collect(Collectors.toList());
    }

    private QmsYieldAnalysisExportVO toExportRow(QmsYieldAnalysisRespVO.SummaryRow row) {
        QmsYieldAnalysisExportVO exportVO = new QmsYieldAnalysisExportVO();
        exportVO.setModelCode(row.getModelCode());
        exportVO.setMotherRollNo(row.getMotherRollNo());
        exportVO.setSegmentNo(row.getSegmentNo());
        exportVO.setProcessName(row.getProcessName());
        exportVO.setInputTotal(row.getInputTotal());
        exportVO.setOutputGoodTotal(row.getOutputGoodTotal());
        exportVO.setTargetQualifiedQty(row.getTargetQualifiedQty());
        exportVO.setTargetUnit(row.getTargetUnit());
        exportVO.setTargetTypeName(targetTypeName(row.getTargetType()));
        exportVO.setTargetAchievementRate(row.getTargetAchievementRate());
        exportVO.setTargetDifference(row.getTargetDifference());
        exportVO.setTargetResult(Boolean.TRUE.equals(row.getTargetMatched())
                ? Boolean.TRUE.equals(row.getTargetReached()) ? "达标" : "未达标"
                : "未配置目标");
        exportVO.setOutputNgTotal(row.getOutputNgTotal());
        exportVO.setBlackDotCount(row.getBlackDotCount());
        exportVO.setBlueDotCount(row.getBlueDotCount());
        exportVO.setYellowDotCount(row.getYellowDotCount());
        exportVO.setRedDotCount(row.getRedDotCount());
        exportVO.setPinholeCount(row.getPinholeCount());
        exportVO.setStripeCount(row.getStripeCount());
        exportVO.setWrinkleCount(row.getWrinkleCount());
        exportVO.setWaveCount(row.getWaveCount());
        exportVO.setOtherCount(row.getOtherCount());
        exportVO.setInspectionTotal(row.getInspectionTotal());
        exportVO.setYieldRate(row.getYieldRate());
        return exportVO;
    }

    private void applyTargetConfigs(List<QmsYieldAnalysisRespVO.SummaryRow> summaryRows) {
        if (summaryRows.isEmpty()) {
            return;
        }
        Map<String, QmsYieldTargetConfigDO> targetConfigMap = qmsYieldTargetConfigMapper.selectEnabledList().stream()
                .collect(Collectors.toMap(config -> targetKey(config.getModelCode(), config.getProcessCode(),
                                defaultTargetSegmentCount(config.getModelCode(), config.getSegmentCount())),
                        config -> config, (first, ignored) -> first, LinkedHashMap::new));
        Map<String, Integer> motherSegmentCountMap = buildMotherSegmentCountMap(summaryRows);
        for (QmsYieldAnalysisRespVO.SummaryRow row : summaryRows) {
            Integer targetSegmentCount = resolveTargetSegmentCount(row.getModelCode(),
                    motherSegmentCountMap.get(motherSegmentKey(row.getModelCode(), row.getMotherRollNo())));
            QmsYieldTargetConfigDO config = targetConfigMap.get(targetKey(row.getModelCode(), row.getProcessCode(),
                    targetSegmentCount));
            if (config == null || config.getTargetQualifiedQty() == null
                    || config.getTargetQualifiedQty().compareTo(BigDecimal.ZERO) <= 0) {
                row.setTargetMatched(false);
                row.setTargetReached(true);
                continue;
            }
            String targetType = normalizeTargetType(config.getTargetType(), config.getProcessCode());
            String targetUnit = resolveTargetUnit(config, targetType);
            BigDecimal actual = TARGET_TYPE_YIELD_RATE.equals(targetType)
                    ? defaultDecimal(row.getYieldRate())
                    : defaultDecimal(row.getOutputGoodTotal());
            BigDecimal target = config.getTargetQualifiedQty();
            row.setTargetMatched(true);
            row.setTargetQualifiedQty(target);
            row.setTargetUnit(targetUnit);
            row.setTargetType(targetType);
            row.setTargetDifference(actual.subtract(target).setScale(2, RoundingMode.HALF_UP));
            row.setTargetAchievementRate(actual.multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP));
            row.setTargetReached(actual.compareTo(target) >= 0);
        }
    }

    private String normalizeTargetType(String targetType, String processCode) {
        String normalizedProcess = normalizeProcessCode(processCode);
        if (PIECE_OUTPUT_PROCESS_CODES.contains(normalizedProcess)) {
            return TARGET_TYPE_OUTPUT_QTY;
        }
        if (TARGET_TYPE_OUTPUT_QTY.equals(targetType) || TARGET_TYPE_YIELD_RATE.equals(targetType)) {
            return targetType;
        }
        if ("FINAL_INSPECTION".equals(normalizedProcess)) {
            return TARGET_TYPE_YIELD_RATE;
        }
        return TARGET_TYPE_OUTPUT_QTY;
    }

    private String targetTypeName(String targetType) {
        return TARGET_TYPE_YIELD_RATE.equals(targetType) ? "良品率百分比" : "产出值绝对值";
    }

    private QmsYieldTargetConfigDO resolveOverviewTargetConfig(QmsYieldAnalysisReqVO reqVO) {
        String processCode = normalizeProcessCode(reqVO.getProcessCode());
        if (!hasText(reqVO.getModelCode()) || !hasText(processCode)) {
            return null;
        }
        String targetKey = targetKey(reqVO.getModelCode(), processCode,
                defaultTargetSegmentCount(reqVO.getModelCode(), null));
        return qmsYieldTargetConfigMapper.selectEnabledList().stream()
                .filter(this::hasAvailableTarget)
                .filter(config -> Objects.equals(targetKey, targetKey(config.getModelCode(), config.getProcessCode(),
                        defaultTargetSegmentCount(config.getModelCode(), config.getSegmentCount()))))
                .findFirst()
                .orElse(null);
    }

    private boolean hasAvailableTarget(QmsYieldTargetConfigDO config) {
        return config != null
                && Objects.equals(config.getStatus(), 0)
                && config.getTargetQualifiedQty() != null
                && config.getTargetQualifiedQty().compareTo(BigDecimal.ZERO) > 0;
    }

    private String resolveTargetUnit(QmsYieldTargetConfigDO config, String targetType) {
        if (PIECE_OUTPUT_PROCESS_CODES.contains(normalizeProcessCode(config.getProcessCode()))) {
            return "片";
        }
        return TARGET_TYPE_YIELD_RATE.equals(targetType) ? "%" : config.getMeasureUnit();
    }

    private String targetKey(String modelCode, String processCode, Integer segmentCount) {
        return modelCodePrefix(modelCode).toUpperCase(Locale.ROOT) + "|"
                + safe(processCode).trim().toUpperCase(Locale.ROOT) + "|"
                + defaultTargetSegmentCount(modelCode, segmentCount);
    }

    private Map<String, Integer> buildMotherSegmentCountMap(List<QmsYieldAnalysisRespVO.SummaryRow> summaryRows) {
        Map<String, Set<String>> segmentMarkMap = new LinkedHashMap<>();
        for (QmsYieldAnalysisRespVO.SummaryRow row : summaryRows) {
            if (!W33_MODEL_CODE.equals(modelCodePrefix(row.getModelCode()))) {
                continue;
            }
            String mark = extractSegmentMark(row.getSegmentNo());
            if (!hasText(mark)) {
                continue;
            }
            segmentMarkMap.computeIfAbsent(motherSegmentKey(row.getModelCode(), row.getMotherRollNo()),
                    ignored -> new java.util.LinkedHashSet<>()).add(mark);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        segmentMarkMap.forEach((key, marks) -> {
            if (marks.size() >= 2 && marks.size() <= 4) {
                result.put(key, marks.size());
            }
        });
        return result;
    }

    private Integer resolveTargetSegmentCount(String modelCode, Integer detectedSegmentCount) {
        if (!W33_MODEL_CODE.equals(modelCodePrefix(modelCode))) {
            return 0;
        }
        return detectedSegmentCount != null && detectedSegmentCount >= 2 && detectedSegmentCount <= 4
                ? detectedSegmentCount : 4;
    }

    private Integer defaultTargetSegmentCount(String modelCode, Integer segmentCount) {
        if (!W33_MODEL_CODE.equals(modelCodePrefix(modelCode))) {
            return 0;
        }
        return segmentCount != null && segmentCount >= 2 && segmentCount <= 4 ? segmentCount : 4;
    }

    private String motherSegmentKey(String modelCode, String motherRollNo) {
        return modelCodePrefix(modelCode).toUpperCase(Locale.ROOT) + "|"
                + safe(motherRollNo).trim().toUpperCase(Locale.ROOT);
    }

    private String extractSegmentMark(String segmentNo) {
        if (!hasText(segmentNo) || "-".equals(segmentNo.trim())) {
            return "";
        }
        Matcher matcher = SEGMENT_MARK_PATTERN.matcher(normalizeSegmentNo(segmentNo));
        return matcher.matches() ? matcher.group(1).toUpperCase(Locale.ROOT) : "";
    }

    @Override
    public QmsYieldAnalysisSourcePreviewRespVO getSourcePreview(String sourceTable, Long sourceId, String processCode) {
        String normalizedSourceTable = safe(sourceTable).trim();
        String normalizedProcessCode = normalizeProcessCode(processCode);
        if (sourceId == null || !isSupportedSourceTable(normalizedSourceTable)) {
            return buildMissingSourcePreview(normalizedSourceTable, sourceId, normalizedProcessCode);
        }

        QmsYieldAnalysisSourcePreviewRow row = selectSourcePreviewRow(normalizedSourceTable, sourceId);
        if (row == null) {
            return buildMissingSourcePreview(normalizedSourceTable, sourceId, normalizedProcessCode);
        }

        QmsYieldAnalysisDetailRespVO detail = toDetail(row);
        Map<String, Integer> defectCounts = buildDefectCounts(row);
        List<QmsYieldAnalysisSourcePreviewRespVO.FieldGroup> fieldGroups = new ArrayList<>();
        addFieldGroup(fieldGroups, "基础信息", true, List.of(
                field("工序", detail.getProcessName()),
                field("计划号", detail.getPlanNo()),
                field("母卷批号", detail.getMotherRollNo()),
                field("分段", detail.getSegmentNo()),
                field("片号", detail.getPieceNo()),
                field("物料编码", detail.getMaterialCode()),
                field("物料名称", detail.getMaterialName()),
                field("型号", detail.getModelCode())
        ));
        addFieldGroup(fieldGroups, "质量判定", true, List.of(
                field("自检", detail.getSelfCheck(), isNg(detail.getSelfCheck()) ? "ng" : "ok"),
                field("送检结果", detail.getSubmissionResult(), detail.getSubmissionNgCount() != null && detail.getSubmissionNgCount() > 0 ? "ng" : "text"),
                field("缺陷编码", row.getDefectCode()),
                field("缺陷摘要", detail.getDefectSummary()),
                field("扫码/确认时间", detail.getConfirmTime(), "time")
        ));
        addFieldGroup(fieldGroups, "来源追溯", false, buildTraceFields(row));
        addFieldGroup(fieldGroups, detail.getProcessName() + "记录", false, buildProcessFields(row));
        addFieldGroup(fieldGroups, "人员与状态", false, buildStatusFields(row, detail));

        return QmsYieldAnalysisSourcePreviewRespVO.builder()
                .found(true)
                .title(detail.getProcessName() + "原始记录")
                .sourceKey(detail.getSourceKey())
                .sourceTable(detail.getSourceTable())
                .sourceId(detail.getSourceId())
                .processCode(detail.getProcessCode())
                .processName(detail.getProcessName())
                .planNo(detail.getPlanNo())
                .motherRollNo(detail.getMotherRollNo())
                .segmentNo(detail.getSegmentNo())
                .pieceNo(detail.getPieceNo())
                .selfCheck(translateDisplayValue(detail.getSelfCheck()))
                .submissionResult(translateDisplayValue(detail.getSubmissionResult()))
                .defectSummary(detail.getDefectSummary())
                .confirmTime(detail.getConfirmTime())
                .fieldGroups(fieldGroups)
                .defectItems(buildDefectItems(defectCounts))
                .rawItems(List.of())
                .build();
    }

    private QmsYieldAnalysisSourcePreviewRespVO buildMissingSourcePreview(String sourceTable,
                                                                          Long sourceId,
                                                                          String processCode) {
        return QmsYieldAnalysisSourcePreviewRespVO.builder()
                .found(false)
                .title("原始记录")
                .sourceKey(sourceTable + ":" + (sourceId == null ? "" : sourceId))
                .sourceTable(blankToDash(sourceTable))
                .sourceId(sourceId)
                .processCode(blankToDash(processCode))
                .processName(resolveProcessName(processCode))
                .fieldGroups(List.of())
                .defectItems(List.of())
                .rawItems(List.of())
                .build();
    }

    private boolean isSupportedSourceTable(String sourceTable) {
        return SOURCE_TABLE_OPERATION_REPORT.equals(sourceTable)
                || SOURCE_TABLE_GRINDING_SECOND.equals(sourceTable)
                || SOURCE_TABLE_ADHESIVE1.equals(sourceTable)
                || SOURCE_TABLE_SLITTING.equals(sourceTable)
                || SOURCE_TABLE_PRESS_SLOT.equals(sourceTable)
                || SOURCE_TABLE_ADHESIVE2.equals(sourceTable)
                || SOURCE_TABLE_CUT_ROUND.equals(sourceTable)
                || SOURCE_TABLE_FQC_SUBMISSION_DETAIL.equals(sourceTable);
    }

    private QmsYieldAnalysisSourcePreviewRow selectSourcePreviewRow(String sourceTable, Long sourceId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (SOURCE_TABLE_OPERATION_REPORT.equals(sourceTable)) {
            return qmsYieldAnalysisMapper.selectOperationReportSourcePreviewRow(tenantId, sourceId);
        }
        if (SOURCE_TABLE_GRINDING_SECOND.equals(sourceTable)) {
            return qmsYieldAnalysisMapper.selectGrindingSecondSourcePreviewRow(tenantId, sourceId);
        }
        if (SOURCE_TABLE_ADHESIVE1.equals(sourceTable)) {
            return qmsYieldAnalysisMapper.selectAdhesive1SourcePreviewRow(tenantId, sourceId);
        }
        if (SOURCE_TABLE_SLITTING.equals(sourceTable)) {
            return qmsYieldAnalysisMapper.selectSlittingSourcePreviewRow(tenantId, sourceId);
        }
        if (SOURCE_TABLE_PRESS_SLOT.equals(sourceTable)) {
            return qmsYieldAnalysisMapper.selectPressSlotSourcePreviewRow(tenantId, sourceId);
        }
        if (SOURCE_TABLE_ADHESIVE2.equals(sourceTable)) {
            return qmsYieldAnalysisMapper.selectAdhesive2SourcePreviewRow(tenantId, sourceId);
        }
        if (SOURCE_TABLE_CUT_ROUND.equals(sourceTable)) {
            return qmsYieldAnalysisMapper.selectCutRoundSourcePreviewRow(tenantId, sourceId);
        }
        if (SOURCE_TABLE_FQC_SUBMISSION_DETAIL.equals(sourceTable)) {
            return qmsYieldAnalysisMapper.selectFinalInspectionSourcePreviewRow(tenantId, sourceId);
        }
        return null;
    }

    private List<QmsYieldAnalysisSourcePreviewRespVO.FieldItem> buildTraceFields(QmsYieldAnalysisSourcePreviewRow row) {
        List<QmsYieldAnalysisSourcePreviewRespVO.FieldItem> fields = new ArrayList<>();
        fields.add(field("来源批号", row.getSourceBatchNo()));
        fields.add(field("来源生产批号", row.getSourceProductionBatchNo()));
        fields.add(field("上游片号", row.getSourceSliceSerialNo()));
        fields.add(field("压槽来源片号", row.getSourcePressSlotProductionBatchNo()));
        fields.add(field("粘胶2来源片号", row.getSourceAdhesive2ProductionBatchNo()));
        fields.add(field("来源库存批次", row.getSourceStockBatchNo()));
        fields.add(field("来源长度", row.getSourceLength(), "number"));
        fields.add(field("锁定数量", row.getSourceLockQty(), "number"));
        fields.add(field("消耗数量", row.getSourceConsumeQty(), "number"));
        fields.add(field("消耗事务号", row.getSourceConsumeTxnNo()));
        fields.add(field("消耗时间", row.getSourceConsumeTime(), "time"));
        return fields;
    }

    private List<QmsYieldAnalysisSourcePreviewRespVO.FieldItem> buildProcessFields(QmsYieldAnalysisSourcePreviewRow row) {
        if ("FORMULA".equals(row.getProcessCode()) || "WET".equals(row.getProcessCode())) {
            return List.of(
                    field("操作编码", row.getOperationCode()),
                    field("操作名称", row.getOperationName()),
                    field("生产批号", row.getProductionBatchNo()),
                    field("报工类型", row.getReportType()),
                    field("报工日期", row.getReportDate(), "date"),
                    field("开始时间", row.getStartTime(), "time"),
                    field("结束时间", row.getEndTime(), "time"),
                    field("计量单位", row.getReportUom()),
                    field("投入数", firstNonNull(row.getFeedQty(), row.getInputQty()), "number"),
                    field("产出良品数", firstNonNull(row.getGoodQty(), row.getOutputGoodQty()), "number"),
                    field("产出不良品数", firstNonNull(row.getScrapQty(), row.getOutputNgQty()), "number")
            );
        }
        if ("ROUGH_GRINDING".equals(row.getProcessCode())) {
            return List.of(
                    field("操作编码", row.getOperationCode()),
                    field("操作名称", row.getOperationName()),
                    field("生产批号", row.getProductionBatchNo()),
                    field("上游批号", row.getParentProductionBatchNo()),
                    field("报工日期", row.getReportDate(), "date"),
                    field("开始时间", row.getStartTime(), "time"),
                    field("结束时间", row.getEndTime(), "time"),
                    field("投入长度", row.getInputLength(), "number"),
                    field("损耗长度", row.getLossLength(), "number"),
                    field("产出长度", row.getOutputLength(), "number"),
                    field("留样长度", row.getNapSampleLength(), "number"),
                    field("送检单号", row.getInspectionNo()),
                    field("送检结果", row.getInspectionResult())
            );
        }
        if ("ADHESIVE1".equals(row.getProcessCode())) {
            return List.of(
                    field("操作编码", row.getOperationCode()),
                    field("操作名称", row.getOperationName()),
                    field("生产批号", row.getProductionBatchNo()),
                    field("上游批号", row.getParentProductionBatchNo()),
                    field("报工日期", row.getReportDate(), "date"),
                    field("开始时间", row.getStartTime(), "time"),
                    field("结束时间", row.getEndTime(), "time"),
                    field("投入长度", row.getInputLength(), "number"),
                    field("起始米数", row.getStartPosition(), "number"),
                    field("结束米数", row.getEndPosition(), "number"),
                    field("损耗长度", row.getLossLength(), "number"),
                    field("产出长度", row.getOutputLength(), "number"),
                    field("留样长度", row.getNapSampleLength(), "number"),
                    field("产出入库状态", row.getOutputStockPostStatus()),
                    field("产出入库时间", row.getOutputStockPostTime(), "time")
            );
        }
        if ("SLITTING".equals(row.getProcessCode())) {
            return List.of(
                    field("操作编码", row.getOperationCode()),
                    field("操作名称", row.getOperationName()),
                    field("片号", row.getSliceSerialNo()),
                    field("分切序号", row.getSliceIndex(), "number"),
                    field("裁切方式", row.getCutMode()),
                    field("尺寸编码", row.getSizeCode()),
                    field("尺寸名称", row.getSizeName()),
                    field("起始米数", row.getStartPosition(), "number"),
                    field("结束米数", row.getEndPosition(), "number"),
                    field("分切长度", row.getSliceLength(), "number"),
                    field("扫码状态", row.getScanStatus()),
                    field("扫码时间", row.getScanTime(), "time"),
                    field("扫码人", row.getScannerName()),
                    field("打印状态", row.getPrintStatus()),
                    field("打印次数", row.getPrintCount(), "number"),
                    field("最后打印时间", row.getLastPrintTime(), "time")
            );
        }
        if ("PRESS_SLOT".equals(row.getProcessCode())) {
            return List.of(
                    field("操作编码", row.getOperationCode()),
                    field("操作名称", row.getOperationName()),
                    field("生产片号", row.getProductionBatchNo()),
                    field("上游片号", row.getParentProductionBatchNo()),
                    field("报工日期", row.getReportDate(), "date"),
                    field("开始时间", row.getStartTime(), "time"),
                    field("结束时间", row.getEndTime(), "time"),
                    field("投入长度", row.getInputLength(), "number"),
                    field("起始米数", row.getStartPosition(), "number"),
                    field("结束米数", row.getEndPosition(), "number"),
                    field("损耗长度", row.getLossLength(), "number"),
                    field("产出长度", row.getOutputLength(), "number"),
                    field("留样长度", row.getNapSampleLength(), "number"),
                    field("压辊料号", row.getPressureRollerMaterialCode()),
                    field("压辊批号", row.getPressureRollerBatchNo()),
                    field("轴承料号", row.getBearingMaterialCode()),
                    field("轴承批号", row.getBearingBatchNo()),
                    field("压辊清洁累计片数", row.getRollerCleanAccumulatedPcs(), "number"),
                    field("轴承更换累计片数", row.getBearingReplaceAccumulatedPcs(), "number")
            );
        }
        if ("ADHESIVE2".equals(row.getProcessCode())) {
            return List.of(
                    field("操作编码", row.getOperationCode()),
                    field("操作名称", row.getOperationName()),
                    field("生产片号", row.getProductionBatchNo()),
                    field("上游片号", row.getParentProductionBatchNo()),
                    field("报工日期", row.getReportDate(), "date"),
                    field("开始时间", row.getStartTime(), "time"),
                    field("结束时间", row.getEndTime(), "time"),
                    field("投入长度", row.getInputLength(), "number"),
                    field("起始米数", row.getStartPosition(), "number"),
                    field("结束米数", row.getEndPosition(), "number"),
                    field("损耗长度", row.getLossLength(), "number"),
                    field("产出长度", row.getOutputLength(), "number"),
                    field("留样长度", row.getNapSampleLength(), "number"),
                    field("胶板型号", row.getGlueBoardModel()),
                    field("胶板料号", row.getGlueBoardMaterialCode()),
                    field("胶板批号", row.getGlueBoardBatchNo()),
                    field("胶板起始米数", row.getGlueBoardStartPosition(), "number"),
                    field("胶板使用长度", row.getGlueBoardUseLength(), "number"),
                    field("产品质量状态", row.getProductQualityStatus()),
                    field("质量锁定原因", row.getQualityLockReason())
            );
        }
        if ("CUT_ROUND".equals(row.getProcessCode())) {
            return List.of(
                    field("操作编码", row.getOperationCode()),
                    field("操作名称", row.getOperationName()),
                    field("生产片号", row.getProductionBatchNo()),
                    field("上游片号", row.getParentProductionBatchNo()),
                    field("报工日期", row.getReportDate(), "date"),
                    field("开始时间", row.getStartTime(), "time"),
                    field("结束时间", row.getEndTime(), "time"),
                    field("投入长度", row.getInputLength(), "number"),
                    field("产出长度", row.getOutputLength(), "number"),
                    field("刀片料号", row.getBladeMaterialCode()),
                    field("刀片批号", row.getBladeBatchNo()),
                    field("毛毡料号", row.getFeltMaterialCode()),
                    field("毛毡批号", row.getFeltBatchNo()),
                    field("刀片使用次数", row.getBladeUseCount(), "number"),
                    field("毛毡使用次数", row.getFeltUseCount(), "number"),
                    field("过程风险标记", row.getQualityRiskFlag()),
                    field("检验任务号", row.getInspectionTaskNo()),
                    field("检验状态", row.getInspectionStatus()),
                    field("检验结果", row.getInspectionResult()),
                    field("检验员", row.getInspectorName()),
                    field("检验时间", row.getInspectionTime(), "time"),
                    field("检验备注", row.getInspectionRemark())
            );
        }
        if ("FINAL_INSPECTION".equals(row.getProcessCode())) {
            return List.of(
                    field("操作编码", row.getOperationCode()),
                    field("操作名称", row.getOperationName()),
                    field("终检单号", row.getInspectionNo()),
                    field("检验任务号", row.getInspectionTaskNo()),
                    field("分段", row.getParentProductionBatchNo()),
                    field("片号", row.getProductionBatchNo()),
                    field("尺寸", row.getSizeName()),
                    field("终检状态", row.getInspectionStatus()),
                    field("片级判定", row.getInspectionResult()),
                    field("检验员", row.getInspectorName()),
                    field("检验时间", row.getInspectionTime(), "time"),
                    field("质量风险标记", row.getQualityRiskFlag()),
                    field("终检备注", row.getInspectionRemark())
            );
        }
        return List.of();
    }

    private List<QmsYieldAnalysisSourcePreviewRespVO.FieldItem> buildStatusFields(QmsYieldAnalysisSourcePreviewRow row,
                                                                                  QmsYieldAnalysisDetailRespVO detail) {
        return List.of(
                field("报工状态", row.getReportStatus()),
                field("扫码/确认时间", detail.getConfirmTime(), "time"),
                field("记录人", row.getRecorderName()),
                field("记录时间", row.getRecorderTime(), "time"),
                field("确认人", row.getConfirmerName()),
                field("报工确认时间", row.getConfirmerTime(), "time"),
                field("产出入库状态", row.getOutputStockPostStatus()),
                field("产出入库时间", row.getOutputStockPostTime(), "time"),
                field("备注", row.getRemark())
        );
    }

    private List<QmsYieldAnalysisSourcePreviewRespVO.FieldItem> buildDefectItems(Map<String, Integer> defectCounts) {
        return DEFECT_NAMES.stream()
                .map(defectName -> field(defectName, defectCounts.getOrDefault(defectName, 0), "number"))
                .collect(Collectors.toList());
    }

    private void addFieldGroup(List<QmsYieldAnalysisSourcePreviewRespVO.FieldGroup> groups,
                               String title,
                               boolean keepEmpty,
                               List<QmsYieldAnalysisSourcePreviewRespVO.FieldItem> items) {
        List<QmsYieldAnalysisSourcePreviewRespVO.FieldItem> visibleItems = keepEmpty ? items : items.stream()
                .filter(this::hasMeaningfulValue)
                .collect(Collectors.toList());
        if (visibleItems.isEmpty()) {
            return;
        }
        groups.add(QmsYieldAnalysisSourcePreviewRespVO.FieldGroup.builder()
                .title(title)
                .items(visibleItems)
                .build());
    }

    private QmsYieldAnalysisSourcePreviewRespVO.FieldItem field(String label, Object value) {
        return field(label, value, "text");
    }

    private QmsYieldAnalysisSourcePreviewRespVO.FieldItem field(String label, Object value, String valueType) {
        return QmsYieldAnalysisSourcePreviewRespVO.FieldItem.builder()
                .label(label)
                .value(formatDisplayValue(value))
                .valueType(valueType)
                .build();
    }

    private boolean hasMeaningfulValue(QmsYieldAnalysisSourcePreviewRespVO.FieldItem item) {
        return item != null && hasText(item.getValue()) && !"-".equals(item.getValue().trim());
    }

    private String formatDisplayValue(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof String text) {
            return hasText(text) ? translateDisplayValue(text.trim()) : "-";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return formatDateTime(dateTime);
        }
        if (value instanceof LocalDate date) {
            return DATE_FORMATTER.format(date);
        }
        return String.valueOf(value);
    }

    private String translateDisplayValue(String value) {
        if (!hasText(value)) {
            return "-";
        }
        String text = value.trim();
        String upper = text.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "OK", "PASS", "PASSED", "QUALIFIED", "SUCCESS" -> "合格";
            case "NG", "N", "FAIL", "FAILED", "UNQUALIFIED", "REJECTED" -> "不合格";
            case "CONFIRMED" -> "已确认";
            case "UNCONFIRMED", "NOT_CONFIRMED" -> "待确认";
            case "PENDING", "WAITING" -> "待处理";
            case "DRAFT" -> "草稿";
            case "SUBMITTED" -> "已提交";
            case "APPROVED" -> "已审核";
            case "REWORK" -> "返工";
            case "CANCELLED", "CANCELED" -> "已取消";
            case "POSTED" -> "已入库";
            case "NOT_POSTED", "UNPOSTED" -> "未入库";
            case "POST_FAILED" -> "入库失败";
            case "PRINTED" -> "已打印";
            case "UNPRINTED", "NOT_PRINTED" -> "未打印";
            case "SCAN_PENDING" -> "待扫码";
            case "SCANNED" -> "已扫码";
            case "MANUAL" -> "手动";
            case "AUTO", "AUTOMATIC" -> "自动";
            case "NORMAL", "NONE" -> "正常";
            case "ABNORMAL" -> "异常";
            case "TRUE", "YES", "Y" -> "是";
            case "FALSE", "NO" -> "否";
            case "LOCKED" -> "已锁定";
            case "UNLOCKED" -> "未锁定";
            case "ADHESIVE2_NG" -> "粘胶2不合格";
            case "CUT_ROUND_NG" -> "裁切不合格";
            case "BOTH_NG" -> "粘胶2与裁切均不合格";
            default -> text;
        };
    }

    private List<QmsYieldAnalysisDetailRespVO> loadDetails(QmsYieldAnalysisReqVO reqVO) {
        DateRange dateRange = resolveDateRange(reqVO);
        List<QmsYieldAnalysisSourceRow> sourceRows = qmsYieldAnalysisMapper.selectSourceRows(
                TenantContextHolder.getTenantId(),
                dateRange.startTime(),
                dateRange.endExclusiveTime(),
                normalizeProcessCode(reqVO.getProcessCode()));
        return sourceRows.stream()
                .map(this::toDetail)
                .filter(detail -> matchesReq(detail, reqVO))
                .sorted(detailComparator())
                .collect(Collectors.toList());
    }

    private List<QmsYieldAnalysisDetailRespVO> convertSourceRows(
            QmsYieldAnalysisReqVO reqVO,
            List<QmsYieldAnalysisSourceRow> sourceRows) {
        if (sourceRows == null || sourceRows.isEmpty()) {
            return List.of();
        }
        return sourceRows.stream()
                .map(this::toDetail)
                .filter(detail -> matchesReq(detail, reqVO))
                .sorted(detailComparator())
                .collect(Collectors.toList());
    }

    private QmsYieldAnalysisDetailRespVO toDetail(QmsYieldAnalysisSourceRow row) {
        String segmentNo = firstNotBlank(normalizeSegmentNo(row.getSegmentNo()), normalizeSegmentNo(row.getPieceNo()));
        String motherRollNo = firstNotBlank(normalizeMotherRollNo(row.getMotherRollNo()), deriveMotherRollNo(segmentNo));
        boolean finalInspection = isFinalInspectionProcess(row.getProcessCode());
        Map<String, Integer> defectCounts = buildDefectCounts(row);
        boolean hasDefectCount = defectCounts.values().stream().anyMatch(count -> count != null && count > 0);
        boolean selfCheckNg = isNg(row.getSelfCheck())
                || hasDefectCount
                || hasText(row.getDefectCode())
                || isExtraSelfCheckNg(row.getExtraJson());
        boolean submissionNg = isNg(row.getSubmissionResult()) || isExtraSubmissionNg(row.getExtraJson());
        if (selfCheckNg && !hasDefectCount && !finalInspection) {
            defectCounts.put(DEFECT_OTHER, 1);
        }

        String selfCheck = firstNotBlank(row.getSelfCheck(), selfCheckNg ? "NG" : "OK");
        String submissionResult = firstNotBlank(row.getSubmissionResult(), readExtraText(row.getExtraJson(),
                "feedbackResult", "inspectionResult", "submissionResult", "qaResult", "fqcResult"));
        BigDecimal inputCount = positiveOrDefault(row.getInputQty(), BigDecimal.ONE);
        BigDecimal sourceOutputNg = nonNegative(row.getOutputNgQty());
        BigDecimal outputNgCount = sourceOutputNg.signum() > 0 ? sourceOutputNg
                : selfCheckNg ? BigDecimal.ONE : BigDecimal.ZERO;
        int inspectionCount = finalInspection ? 1 : hasInspection(row) ? 1 : 0;
        BigDecimal outputGoodCount = row.getOutputGoodQty() == null
                ? subtractNonNegative(inputCount, outputNgCount, getInspectionDeductQty(row, inspectionCount))
                : nonNegative(row.getOutputGoodQty());
        BigDecimal ngCount = outputNgCount;
        String defectSummary = finalInspection
                ? buildFinalInspectionDefectSummary(row)
                : buildDefectSummary(defectCounts, row.getDefectCode());

        return QmsYieldAnalysisDetailRespVO.builder()
                .groupKey(buildGroupKey(modelCodePrefix(row.getModelCode()), motherRollNo, segmentNo, row.getProcessCode()))
                .sourceKey(row.getSourceTable() + ":" + row.getSourceId())
                .sourceTable(row.getSourceTable())
                .sourceId(row.getSourceId())
                .sourceRoute(buildSourcePreviewRoute(row))
                .inspectionId(row.getInspectionId())
                .inspectionNo(row.getInspectionNo())
                .inspectionSourceType(row.getInspectionSourceType())
                .processCode(row.getProcessCode())
                .processName(firstNotBlank(row.getProcessName(), resolveProcessName(row.getProcessCode())))
                .planNo(blankToDash(row.getPlanNo()))
                .motherRollNo(blankToDash(motherRollNo))
                .segmentNo(blankToDash(segmentNo))
                .pieceNo(blankToDash(row.getPieceNo()))
                .materialCode(blankToDash(row.getMaterialCode()))
                .materialName(blankToDash(row.getMaterialName()))
                .modelCode(blankToDash(row.getModelCode()))
                .selfCheck(selfCheck)
                .submissionResult(firstNotBlank(submissionResult, submissionNg ? "NG" : "-"))
                .inputCount(inputCount)
                .outputGoodCount(outputGoodCount)
                .outputNgCount(outputNgCount)
                .inspectionCount(inspectionCount)
                .selfCheckNgCount(selfCheckNg ? 1 : 0)
                .submissionNgCount(submissionNg ? 1 : 0)
                .ngCount(ngCount)
                .yieldRate(calculateYieldRate(inputCount, outputGoodCount))
                .blackDotCount(defectCounts.get(DEFECT_BLACK_DOT))
                .blueDotCount(defectCounts.get(DEFECT_BLUE_DOT))
                .yellowDotCount(defectCounts.get(DEFECT_YELLOW_DOT))
                .redDotCount(defectCounts.get(DEFECT_RED_DOT))
                .pinholeCount(defectCounts.get(DEFECT_PINHOLE))
                .stripeCount(defectCounts.get(DEFECT_STRIPE))
                .wrinkleCount(defectCounts.get(DEFECT_WRINKLE))
                .waveCount(defectCounts.get(DEFECT_WAVE))
                .otherCount(defectCounts.get(DEFECT_OTHER))
                .defectSummary(defectSummary)
                .confirmTime(formatDateTime(row.getConfirmTime()))
                .build();
    }

    private boolean hasInspection(QmsYieldAnalysisSourceRow row) {
        return row.getInspectionId() != null || hasText(row.getInspectionNo());
    }

    private BigDecimal getInspectionDeductQty(QmsYieldAnalysisSourceRow row, int inspectionCount) {
        if (inspectionCount <= 0) {
            return BigDecimal.ZERO;
        }
        String processCode = normalizeProcessCode(row.getProcessCode());
        return "PRESS_SLOT".equals(processCode) || "ADHESIVE2".equals(processCode)
                ? BigDecimal.valueOf(inspectionCount) : BigDecimal.ZERO;
    }

    private DateRange resolveDateRange(QmsYieldAnalysisReqVO reqVO) {
        LocalDate defaultDate = LocalDate.now().minusDays(1);
        LocalDate startDate = reqVO.getStartDate() == null ? defaultDate : reqVO.getStartDate();
        LocalDate endDate = reqVO.getEndDate() == null ? startDate : reqVO.getEndDate();
        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }
        return new DateRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private boolean matchesReq(QmsYieldAnalysisDetailRespVO detail, QmsYieldAnalysisReqVO reqVO) {
        if (!matchesText(detail.getPlanNo(), reqVO.getPlanNo())) {
            return false;
        }
        if (!matchesText(detail.getMotherRollNo(), reqVO.getMotherRollNo())) {
            return false;
        }
        if (!matchesText(detail.getSegmentNo(), reqVO.getSegmentNo())) {
            return false;
        }
        if (!matchesText(detail.getPieceNo(), reqVO.getPieceNo())) {
            return false;
        }
        if (!matchesModelCode(detail.getModelCode(), reqVO.getModelCode())) {
            return false;
        }
        if (hasText(reqVO.getMaterialKeyword())) {
            String materialSource = String.join(" ",
                    safe(detail.getMaterialCode()), safe(detail.getMaterialName()), safe(detail.getModelCode()));
            if (!matchesText(materialSource, reqVO.getMaterialKeyword())) {
                return false;
            }
        }
        if (reqVO.getStatDate() != null
                && !Objects.equals(firstLeft(detail.getConfirmTime(), 10), DATE_FORMATTER.format(reqVO.getStatDate()))) {
            return false;
        }
        if (hasText(reqVO.getDefectName()) && getDetailDefectCount(detail, reqVO.getDefectName()) <= 0) {
            return false;
        }
        if (hasText(reqVO.getMetricKey()) && getDetailMetricCount(detail, reqVO.getMetricKey()) <= 0) {
            return false;
        }
        if (hasText(reqVO.getKeyword())) {
            String keywordSource = String.join(" ",
                    safe(detail.getPlanNo()), safe(detail.getMotherRollNo()), safe(detail.getSegmentNo()),
                    safe(detail.getPieceNo()), safe(detail.getMaterialCode()), safe(detail.getMaterialName()),
                    safe(detail.getModelCode()), safe(detail.getDefectSummary()));
            return matchesText(keywordSource, reqVO.getKeyword());
        }
        return true;
    }

    private int getDetailMetricCount(QmsYieldAnalysisDetailRespVO detail, String metricKey) {
        if (!hasText(metricKey)) {
            return 1;
        }
        String normalizedMetricKey = metricKey.trim();
        if ("inputTotal".equals(normalizedMetricKey) || "inputCount".equals(normalizedMetricKey)
                || "confirmedTotal".equals(normalizedMetricKey)) {
            return positiveMetric(detail.getInputCount());
        }
        if ("outputGoodTotal".equals(normalizedMetricKey) || "outputGoodCount".equals(normalizedMetricKey)
                || "goodTotal".equals(normalizedMetricKey)) {
            return positiveMetric(detail.getOutputGoodCount());
        }
        if ("outputNgTotal".equals(normalizedMetricKey) || "outputNgCount".equals(normalizedMetricKey)
                || "ngTotal".equals(normalizedMetricKey) || "selfCheckNgTotal".equals(normalizedMetricKey)) {
            return positiveMetric(detail.getOutputNgCount());
        }
        if ("inspectionTotal".equals(normalizedMetricKey) || "inspectionCount".equals(normalizedMetricKey)
                || "submissionNgTotal".equals(normalizedMetricKey)) {
            return defaultInt(detail.getInspectionCount());
        }
        if ("yieldRate".equals(normalizedMetricKey)) {
            return positiveMetric(detail.getInputCount());
        }
        Integer defectCount = getDetailDefectCountByMetricKey(detail, normalizedMetricKey);
        return defectCount == null ? 1 : defectCount;
    }

    private Integer getDetailDefectCountByMetricKey(QmsYieldAnalysisDetailRespVO detail, String metricKey) {
        if ("blackDotCount".equals(metricKey)) {
            return defaultInt(detail.getBlackDotCount());
        }
        if ("blueDotCount".equals(metricKey)) {
            return defaultInt(detail.getBlueDotCount());
        }
        if ("yellowDotCount".equals(metricKey)) {
            return defaultInt(detail.getYellowDotCount());
        }
        if ("redDotCount".equals(metricKey)) {
            return defaultInt(detail.getRedDotCount());
        }
        if ("pinholeCount".equals(metricKey)) {
            return defaultInt(detail.getPinholeCount());
        }
        if ("stripeCount".equals(metricKey)) {
            return defaultInt(detail.getStripeCount());
        }
        if ("wrinkleCount".equals(metricKey)) {
            return defaultInt(detail.getWrinkleCount());
        }
        if ("waveCount".equals(metricKey)) {
            return defaultInt(detail.getWaveCount());
        }
        if ("otherCount".equals(metricKey)) {
            return defaultInt(detail.getOtherCount());
        }
        return null;
    }

    private int getDetailDefectCount(QmsYieldAnalysisDetailRespVO detail, String defectName) {
        String normalizedDefectName = normalizeDefectName(defectName);
        if (DEFECT_BLACK_DOT.equals(normalizedDefectName)) {
            return defaultInt(detail.getBlackDotCount());
        }
        if (DEFECT_BLUE_DOT.equals(normalizedDefectName)) {
            return defaultInt(detail.getBlueDotCount());
        }
        if (DEFECT_YELLOW_DOT.equals(normalizedDefectName)) {
            return defaultInt(detail.getYellowDotCount());
        }
        if (DEFECT_RED_DOT.equals(normalizedDefectName)) {
            return defaultInt(detail.getRedDotCount());
        }
        if (DEFECT_PINHOLE.equals(normalizedDefectName)) {
            return defaultInt(detail.getPinholeCount());
        }
        if (DEFECT_STRIPE.equals(normalizedDefectName)) {
            return defaultInt(detail.getStripeCount());
        }
        if (DEFECT_WRINKLE.equals(normalizedDefectName)) {
            return defaultInt(detail.getWrinkleCount());
        }
        if (DEFECT_WAVE.equals(normalizedDefectName)) {
            return defaultInt(detail.getWaveCount());
        }
        return defaultInt(detail.getOtherCount());
    }

    private static int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal positiveOrDefault(BigDecimal value, BigDecimal defaultValue) {
        BigDecimal decimal = defaultDecimal(value);
        return decimal.compareTo(BigDecimal.ZERO) > 0 ? decimal : defaultValue;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        BigDecimal decimal = defaultDecimal(value);
        return decimal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : decimal;
    }

    private static BigDecimal subtractNonNegative(BigDecimal source, BigDecimal... deductions) {
        BigDecimal result = defaultDecimal(source);
        for (BigDecimal deduction : deductions) {
            result = result.subtract(defaultDecimal(deduction));
        }
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    private static int positiveMetric(BigDecimal value) {
        return defaultDecimal(value).compareTo(BigDecimal.ZERO) > 0 ? 1 : 0;
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String blankDisplay(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private static String modelCodePrefix(String value) {
        String display = blankDisplay(value);
        return "-".equals(display) || display.length() <= 3 ? display : display.substring(0, 3);
    }

    private Map<String, Integer> buildDefectCounts(QmsYieldAnalysisSourceRow row) {
        Map<String, Integer> counts = initDefectCounter();
        if (isFinalInspectionProcess(row.getProcessCode())) {
            return counts;
        }
        collectVisualDefectCounts(row.getVisualResultJson(), counts);
        collectVisualDefectCountsFromExtra(row.getExtraJson(), counts);
        if (sumDefectCounts(counts) == 0) {
            collectDefectCodeCounts(row.getDefectCode(), counts);
        }
        return counts;
    }

    private String buildFinalInspectionDefectSummary(QmsYieldAnalysisSourceRow row) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        collectFinalInspectionDefectItems(row.getVisualResultJson(), counts);
        collectFinalInspectionDefectItemsFromExtra(row.getExtraJson(), counts);
        if (counts.isEmpty()) {
            mergeDefectSummaryItem(counts, row.getDefectCode(), 1);
        }
        return formatDefectSummaryCounts(counts);
    }

    private void collectFinalInspectionDefectItemsFromExtra(String extraJson, Map<String, Integer> counts) {
        JsonNode extra = parseJson(extraJson);
        if (extra == null || extra.isNull()) {
            return;
        }
        collectFinalInspectionDefectItems(extra.get("visualItems"), counts);
        collectFinalInspectionDefectItems(extra.get("visualInspectionItems"), counts);
        collectFinalInspectionDefectItems(extra.get("visualResult"), counts);
        collectFinalInspectionDefectItems(extra.get("visualResults"), counts);
        collectFinalInspectionDefectItems(extra.get("defects"), counts);
        collectFinalInspectionDefectItems(extra.get("defectItems"), counts);
        addDefectSummaryCounts(counts, firstText(extra, "defectSummary"));
        mergeDefectSummaryItem(counts, firstText(extra, "defectName", "defectCode"), 1);
    }

    private void collectFinalInspectionDefectItems(String json, Map<String, Integer> counts) {
        collectFinalInspectionDefectItems(parseJson(json), counts);
    }

    private void collectFinalInspectionDefectItems(JsonNode source, Map<String, Integer> counts) {
        if (source == null || source.isNull()) {
            return;
        }
        if (source.isTextual()) {
            JsonNode parsed = parseJson(source.asText());
            if (parsed == null) {
                mergeDefectSummaryItem(counts, source.asText(), 1);
                return;
            }
            collectFinalInspectionDefectItems(parsed, counts);
            return;
        }
        if (source.isArray()) {
            for (JsonNode item : source) {
                collectFinalInspectionDefectItem(item, counts);
            }
            return;
        }
        if (source.isObject()) {
            JsonNode rows = firstNode(source, "visualItems", "visualInspectionItems", "items", "details", "list",
                    "defects", "defectItems");
            if (rows != null) {
                collectFinalInspectionDefectItems(rows, counts);
                return;
            }
            String defectName = firstText(source, "defectName", "defectCode", "itemName", "name", "checkItem",
                    "label", "code");
            if (hasText(defectName)) {
                mergeDefectSummaryItem(counts, defectName, Math.max(resolveVisualItemQuantity(source), 1));
                return;
            }
            source.fields().forEachRemaining(entry -> {
                if (isActiveValue(entry.getValue())) {
                    mergeDefectSummaryItem(counts, entry.getKey(), resolveActiveQuantity(entry.getValue()));
                }
            });
        }
    }

    private void collectFinalInspectionDefectItem(JsonNode item, Map<String, Integer> counts) {
        if (item == null || item.isNull()) {
            return;
        }
        if (!item.isObject()) {
            collectFinalInspectionDefectItems(item, counts);
            return;
        }
        int quantity = resolveVisualItemQuantity(item);
        if (quantity <= 0) {
            return;
        }
        String defectName = firstText(item, "defectName", "defectCode", "itemName", "name", "checkItem", "label",
                "code");
        mergeDefectSummaryItem(counts, defectName, quantity);
    }

    private void collectVisualDefectCountsFromExtra(String extraJson, Map<String, Integer> counts) {
        JsonNode extra = parseJson(extraJson);
        if (extra == null || extra.isNull()) {
            return;
        }
        collectVisualDefectCounts(extra.get("visualItems"), counts);
        collectVisualDefectCounts(extra.get("visualInspectionItems"), counts);
        collectVisualDefectCounts(extra.get("visualResult"), counts);
        collectVisualDefectCounts(extra.get("visualResults"), counts);
    }

    private void collectVisualDefectCounts(String json, Map<String, Integer> counts) {
        collectVisualDefectCounts(parseJson(json), counts);
    }

    private void collectVisualDefectCounts(JsonNode source, Map<String, Integer> counts) {
        if (source == null || source.isNull()) {
            return;
        }
        if (source.isTextual()) {
            collectVisualDefectCounts(parseJson(source.asText()), counts);
            return;
        }
        if (source.isArray()) {
            for (JsonNode item : source) {
                collectVisualItem(item, counts);
            }
            return;
        }
        if (source.isObject()) {
            JsonNode rows = firstNode(source, "visualItems", "visualInspectionItems", "items", "details", "list");
            if (rows != null) {
                collectVisualDefectCounts(rows, counts);
                return;
            }
            source.fields().forEachRemaining(entry -> {
                String defectName = normalizeDefectName(entry.getKey());
                if (!DEFECT_OTHER.equals(defectName) && isActiveValue(entry.getValue())) {
                    counts.merge(defectName, resolveActiveQuantity(entry.getValue()), Integer::sum);
                }
            });
        }
    }

    private void collectVisualItem(JsonNode item, Map<String, Integer> counts) {
        if (item == null || !item.isObject()) {
            return;
        }
        int quantity = resolveVisualItemQuantity(item);
        if (quantity <= 0) {
            return;
        }
        String itemName = firstText(item, "itemName", "name", "defectName", "checkItem", "label");
        counts.merge(normalizeDefectName(itemName), quantity, Integer::sum);
    }

    private int resolveVisualItemQuantity(JsonNode item) {
        JsonNode okNode = item.get("ok");
        if (okNode != null && okNode.isBoolean()) {
            return okNode.asBoolean() ? 0 : 1;
        }
        JsonNode qtyNode = firstNode(item, "count", "qty", "quantity");
        if (qtyNode != null && qtyNode.isNumber() && qtyNode.asInt() > 0) {
            return qtyNode.asInt();
        }
        return isNg(firstText(item, "result", "checkResult", "status", "judgment")) ? 1 : 0;
    }

    private void collectDefectCodeCounts(String defectCode, Map<String, Integer> counts) {
        if (!hasText(defectCode)) {
            return;
        }
        counts.merge(normalizeDefectName(defectCode), 1, Integer::sum);
    }

    private int resolveActiveQuantity(JsonNode value) {
        if (value != null && value.isNumber() && value.asInt() > 0) {
            return value.asInt();
        }
        return 1;
    }

    private int sumDefectCounts(Map<String, Integer> counts) {
        return counts.values().stream().mapToInt(value -> value == null ? 0 : value).sum();
    }

    private boolean isExtraSelfCheckNg(String extraJson) {
        return isNg(readExtraText(extraJson, "visualInspectionResult", "selfCheck", "selfCheckResult"));
    }

    private boolean isExtraSubmissionNg(String extraJson) {
        return isNg(readExtraText(extraJson, "feedbackResult", "inspectionResult", "submissionResult", "qaResult", "fqcResult"));
    }

    private String readExtraText(String extraJson, String... fieldNames) {
        JsonNode extra = parseJson(extraJson);
        if (extra == null || !extra.isObject()) {
            return "";
        }
        return firstText(extra, fieldNames);
    }

    private JsonNode parseJson(String json) {
        if (!hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonNode firstNode(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        JsonNode value = firstNode(node, fieldNames);
        return value == null ? "" : value.asText("");
    }

    private boolean isActiveValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            return value.asInt() > 0;
        }
        return isNg(value.asText());
    }

    private boolean isNg(String value) {
        if (!hasText(value)) {
            return false;
        }
        String text = value.trim().toUpperCase(Locale.ROOT);
        return Objects.equals(text, "NG")
                || Objects.equals(text, "N")
                || Objects.equals(text, "FAIL")
                || Objects.equals(text, "FAILED")
                || Objects.equals(text, "ABNORMAL")
                || Objects.equals(text, "REJECTED")
                || text.contains("不合格")
                || text.contains("异常");
    }

    private String normalizeDefectName(String rawName) {
        if (!hasText(rawName)) {
            return DEFECT_OTHER;
        }
        String text = rawName.trim();
        for (String defectName : DEFECT_NAMES) {
            if (!DEFECT_OTHER.equals(defectName) && text.contains(defectName)) {
                return defectName;
            }
        }
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("BLACK")) {
            return DEFECT_BLACK_DOT;
        }
        if (upper.contains("BLUE")) {
            return DEFECT_BLUE_DOT;
        }
        if (upper.contains("YELLOW")) {
            return DEFECT_YELLOW_DOT;
        }
        if (upper.contains("RED")) {
            return DEFECT_RED_DOT;
        }
        if (upper.contains("PIN") || upper.contains("HOLE")) {
            return DEFECT_PINHOLE;
        }
        if (upper.contains("STRIPE")) {
            return DEFECT_STRIPE;
        }
        if (upper.contains("WRINKLE")) {
            return DEFECT_WRINKLE;
        }
        if (upper.contains("WAVE")) {
            return DEFECT_WAVE;
        }
        return DEFECT_OTHER;
    }

    private List<QmsYieldAnalysisRespVO.DefectDistribution> buildDefectDistribution(Map<String, Integer> defectTotals) {
        int total = defectTotals.values().stream().mapToInt(Integer::intValue).sum();
        return defectTotals.entrySet().stream()
                .map(entry -> QmsYieldAnalysisRespVO.DefectDistribution.builder()
                        .defectName(entry.getKey())
                        .defectCount(entry.getValue())
                        .ratio(calculateRatio(entry.getValue(), total))
                        .build())
                .sorted(Comparator.comparing(QmsYieldAnalysisRespVO.DefectDistribution::getDefectCount).reversed()
                        .thenComparing(row -> DEFECT_NAMES.indexOf(row.getDefectName())))
                .collect(Collectors.toList());
    }

    private void addDefectCounts(Map<String, Integer> target, QmsYieldAnalysisDetailRespVO detail) {
        if (isFinalInspectionProcess(detail.getProcessCode())) {
            return;
        }
        target.merge(DEFECT_BLACK_DOT, detail.getBlackDotCount(), Integer::sum);
        target.merge(DEFECT_BLUE_DOT, detail.getBlueDotCount(), Integer::sum);
        target.merge(DEFECT_YELLOW_DOT, detail.getYellowDotCount(), Integer::sum);
        target.merge(DEFECT_RED_DOT, detail.getRedDotCount(), Integer::sum);
        target.merge(DEFECT_PINHOLE, detail.getPinholeCount(), Integer::sum);
        target.merge(DEFECT_STRIPE, detail.getStripeCount(), Integer::sum);
        target.merge(DEFECT_WRINKLE, detail.getWrinkleCount(), Integer::sum);
        target.merge(DEFECT_WAVE, detail.getWaveCount(), Integer::sum);
        target.merge(DEFECT_OTHER, detail.getOtherCount(), Integer::sum);
    }

    private String buildDefectSummary(Map<String, Integer> counts, String defectCode) {
        String summary = counts.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> entry.getKey() + entry.getValue())
                .collect(Collectors.joining("、"));
        if (hasText(summary)) {
            return summary;
        }
        return hasText(defectCode) ? defectCode.trim() : "-";
    }

    private static void addDefectSummaryCounts(Map<String, Integer> target, String summary) {
        if (!hasText(summary) || "-".equals(summary.trim())) {
            return;
        }
        String[] items = summary.split("[、,，;；\\s]+");
        for (String rawItem : items) {
            if (!hasText(rawItem) || "-".equals(rawItem.trim())) {
                continue;
            }
            String item = rawItem.trim();
            Matcher matcher = DEFECT_SUMMARY_ITEM_PATTERN.matcher(item);
            if (matcher.matches()) {
                String defectName = matcher.group(1).trim();
                int quantity = Integer.parseInt(matcher.group(2));
                mergeDefectSummaryItem(target, defectName, quantity);
            } else {
                mergeDefectSummaryItem(target, item, 1);
            }
        }
    }

    private static void mergeDefectSummaryItem(Map<String, Integer> target, String rawName, int quantity) {
        if (!hasText(rawName)) {
            return;
        }
        String defectName = rawName.trim();
        if ("-".equals(defectName) || "OK".equalsIgnoreCase(defectName) || "PASS".equalsIgnoreCase(defectName)) {
            return;
        }
        target.merge(defectName, Math.max(quantity, 1), Integer::sum);
    }

    private static String formatDefectSummaryCounts(Map<String, Integer> counts) {
        String summary = counts.entrySet().stream()
                .filter(entry -> hasText(entry.getKey()) && entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> entry.getKey() + entry.getValue())
                .collect(Collectors.joining("、"));
        return hasText(summary) ? summary : "-";
    }

    private static boolean isFinalInspectionProcess(String processCode) {
        return processCode != null && "FINAL_INSPECTION".equals(processCode.trim().toUpperCase(Locale.ROOT));
    }

    private Map<String, Integer> initDefectCounter() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String defectName : DEFECT_NAMES) {
            result.put(defectName, 0);
        }
        return result;
    }

    private Comparator<QmsYieldAnalysisRespVO.SummaryRow> summaryComparator() {
        return Comparator.comparing(QmsYieldAnalysisRespVO.SummaryRow::getModelCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(QmsYieldAnalysisRespVO.SummaryRow::getMotherRollNo, Comparator.nullsLast(String::compareTo))
                .thenComparing(QmsYieldAnalysisRespVO.SummaryRow::getSegmentNo, Comparator.nullsLast(String::compareTo))
                .thenComparingInt(row -> PROCESS_SORT.getOrDefault(row.getProcessCode(), 999));
    }

    private List<QmsYieldAnalysisRespVO.SegmentRow> buildSegmentRows(List<QmsYieldAnalysisRespVO.SummaryRow> summaryRows) {
        Map<String, SegmentAccumulator> segmentMap = new LinkedHashMap<>();
        for (QmsYieldAnalysisRespVO.SummaryRow summaryRow : summaryRows) {
            String key = String.join("|", blankDisplay(summaryRow.getModelCode()),
                    blankDisplay(summaryRow.getMotherRollNo()), blankDisplay(summaryRow.getSegmentNo()));
            segmentMap.computeIfAbsent(key, ignored -> new SegmentAccumulator(summaryRow)).add(summaryRow);
        }
        return segmentMap.values().stream()
                .map(SegmentAccumulator::toRow)
                .sorted(Comparator.comparing(QmsYieldAnalysisRespVO.SegmentRow::getModelCode, Comparator.nullsLast(String::compareTo))
                        .thenComparing(QmsYieldAnalysisRespVO.SegmentRow::getMotherRollNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(QmsYieldAnalysisRespVO.SegmentRow::getSegmentNo, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private Comparator<QmsYieldAnalysisDetailRespVO> detailComparator() {
        return Comparator.comparing(QmsYieldAnalysisDetailRespVO::getConfirmTime, Comparator.nullsLast(String::compareTo)).reversed()
                .thenComparingInt(row -> PROCESS_SORT.getOrDefault(row.getProcessCode(), 999))
                .thenComparing(QmsYieldAnalysisDetailRespVO::getPieceNo, Comparator.nullsLast(String::compareTo));
    }

    private static BigDecimal calculateYieldRate(BigDecimal inputTotal, BigDecimal outputGoodTotal) {
        BigDecimal input = defaultDecimal(inputTotal);
        if (input.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return nonNegative(outputGoodTotal)
                .multiply(BigDecimal.valueOf(100))
                .divide(input, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRatio(int count, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private String buildGroupKey(String modelCode, String motherRollNo, String segmentNo, String processCode) {
        return String.join("|", blankToDash(modelCode), blankToDash(motherRollNo), blankToDash(segmentNo),
                blankToDash(processCode));
    }

    private String deriveMotherRollNo(String segmentNo) {
        String segment = normalizeSegmentNo(segmentNo);
        if (!hasText(segment)) {
            return "";
        }
        Matcher matcher = MOTHER_FROM_SEGMENT_PATTERN.matcher(segment);
        return matcher.matches() ? matcher.group(1).toUpperCase(Locale.ROOT) : segment;
    }

    private String deriveSegmentNo(String pieceNo) {
        return normalizeSegmentNo(pieceNo);
    }

    private String normalizeSegmentNo(String batchNo) {
        if (!hasText(batchNo) || "-".equals(batchNo.trim())) {
            return "";
        }
        String value = batchNo.trim().toUpperCase(Locale.ROOT);
        Matcher directSegmentMatcher = SEGMENT_BATCH_PATTERN.matcher(value);
        if (directSegmentMatcher.matches()) {
            return directSegmentMatcher.group(1).toUpperCase(Locale.ROOT);
        }
        Matcher adhesiveSegmentMatcher = ADHESIVE_SEGMENT_BATCH_PATTERN.matcher(value);
        if (adhesiveSegmentMatcher.matches()) {
            return adhesiveSegmentMatcher.group(1).toUpperCase(Locale.ROOT);
        }
        Matcher pieceMatcher = PIECE_BATCH_PATTERN.matcher(value);
        if (pieceMatcher.matches()) {
            return pieceMatcher.group(1).toUpperCase(Locale.ROOT);
        }
        return value;
    }

    private String normalizeMotherRollNo(String batchNo) {
        if (!hasText(batchNo)) {
            return "";
        }
        String value = batchNo.trim();
        if ("-".equals(value)) {
            return "";
        }
        return deriveMotherRollNo(value);
    }

    private String buildSourcePreviewRoute(QmsYieldAnalysisSourceRow row) {
        if (row == null || row.getSourceId() == null || !hasText(row.getSourceTable())) {
            return "";
        }
        return SOURCE_PREVIEW_PATH
                + "?sourceTable=" + encodeQueryValue(row.getSourceTable())
                + "&sourceId=" + row.getSourceId()
                + "&processCode=" + encodeQueryValue(row.getProcessCode());
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private String resolveProcessName(String processCode) {
        if ("FORMULA".equals(processCode)) {
            return "配料";
        }
        if ("WET".equals(processCode)) {
            return "湿法";
        }
        if ("ROUGH_GRINDING".equals(processCode)) {
            return "磨皮";
        }
        if ("ADHESIVE1".equals(processCode)) {
            return "粘胶1";
        }
        if ("SLITTING".equals(processCode)) {
            return "分切";
        }
        if ("PRESS_SLOT".equals(processCode)) {
            return "压槽";
        }
        if ("ADHESIVE2".equals(processCode)) {
            return "粘胶2";
        }
        if ("CUT_ROUND".equals(processCode)) {
            return "裁切";
        }
        if ("FINAL_INSPECTION".equals(processCode)) {
            return "终检";
        }
        return "-";
    }

    private String normalizeProcessCode(String processCode) {
        if (!hasText(processCode) || "ALL".equalsIgnoreCase(processCode.trim())) {
            return "";
        }
        return processCode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean matchesText(String source, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        return safe(source).toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean matchesModelCode(String source, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        return Objects.equals(modelCodePrefix(source), modelCodePrefix(keyword));
    }

    private String firstLeft(String value, int length) {
        if (!hasText(value)) {
            return "";
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATETIME_FORMATTER.format(dateTime);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String blankToDash(String value) {
        return hasText(value) ? value.trim() : "-";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record DateRange(LocalDateTime startTime, LocalDateTime endExclusiveTime) {
    }

    private static class SummaryAccumulator {

        private final String groupKey;
        private final String modelCode;
        private final String motherRollNo;
        private final String segmentNo;
        private final String processCode;
        private final String processName;
        private String planNo;
        private int confirmedTotal;
        private BigDecimal ngTotal = BigDecimal.ZERO;
        private int selfCheckNgTotal;
        private int submissionNgTotal;
        private BigDecimal inputTotal = BigDecimal.ZERO;
        private BigDecimal outputGoodTotal = BigDecimal.ZERO;
        private BigDecimal outputNgTotal = BigDecimal.ZERO;
        private int inspectionTotal;
        private final Map<String, Integer> defectCounts = new LinkedHashMap<>();
        private final Map<String, Integer> defectSummaryCounts = new LinkedHashMap<>();

        SummaryAccumulator(QmsYieldAnalysisDetailRespVO detail) {
            this.groupKey = detail.getGroupKey();
            this.modelCode = modelCodePrefix(detail.getModelCode());
            this.motherRollNo = detail.getMotherRollNo();
            this.segmentNo = detail.getSegmentNo();
            this.processCode = detail.getProcessCode();
            this.processName = detail.getProcessName();
            for (String defectName : DEFECT_NAMES) {
                defectCounts.put(defectName, 0);
            }
        }

        void add(QmsYieldAnalysisDetailRespVO detail) {
            confirmedTotal++;
            inputTotal = inputTotal.add(defaultDecimal(detail.getInputCount()));
            outputGoodTotal = outputGoodTotal.add(defaultDecimal(detail.getOutputGoodCount()));
            outputNgTotal = outputNgTotal.add(defaultDecimal(detail.getOutputNgCount()));
            inspectionTotal += defaultInt(detail.getInspectionCount());
            ngTotal = outputNgTotal;
            selfCheckNgTotal += detail.getSelfCheckNgCount();
            submissionNgTotal += detail.getSubmissionNgCount();
            if (!"-".equals(detail.getPlanNo())) {
                planNo = planNo == null ? detail.getPlanNo()
                        : planNo.contains(detail.getPlanNo()) ? planNo : planNo + "、" + detail.getPlanNo();
            }
            defectCounts.merge(DEFECT_BLACK_DOT, detail.getBlackDotCount(), Integer::sum);
            defectCounts.merge(DEFECT_BLUE_DOT, detail.getBlueDotCount(), Integer::sum);
            defectCounts.merge(DEFECT_YELLOW_DOT, detail.getYellowDotCount(), Integer::sum);
            defectCounts.merge(DEFECT_RED_DOT, detail.getRedDotCount(), Integer::sum);
            defectCounts.merge(DEFECT_PINHOLE, detail.getPinholeCount(), Integer::sum);
            defectCounts.merge(DEFECT_STRIPE, detail.getStripeCount(), Integer::sum);
            defectCounts.merge(DEFECT_WRINKLE, detail.getWrinkleCount(), Integer::sum);
            defectCounts.merge(DEFECT_WAVE, detail.getWaveCount(), Integer::sum);
            defectCounts.merge(DEFECT_OTHER, detail.getOtherCount(), Integer::sum);
            if (isFinalInspectionProcess(processCode)) {
                addDefectSummaryCounts(defectSummaryCounts, detail.getDefectSummary());
            }
        }

        QmsYieldAnalysisRespVO.SummaryRow toRow() {
            return QmsYieldAnalysisRespVO.SummaryRow.builder()
                    .groupKey(groupKey)
                    .modelCode(modelCode)
                    .motherRollNo(motherRollNo)
                    .segmentNo(segmentNo)
                    .processCode(processCode)
                    .processName(processName)
                    .planNo(planNo == null ? "-" : planNo)
                    .confirmedTotal(confirmedTotal)
                    .goodTotal(outputGoodTotal)
                    .ngTotal(ngTotal)
                    .selfCheckNgTotal(selfCheckNgTotal)
                    .submissionNgTotal(submissionNgTotal)
                    .inputTotal(inputTotal)
                    .outputGoodTotal(outputGoodTotal)
                    .outputNgTotal(outputNgTotal)
                    .inspectionTotal(inspectionTotal)
                    .yieldRate(calculateYieldRate(inputTotal, outputGoodTotal))
                    .blackDotCount(defectCounts.get(DEFECT_BLACK_DOT))
                    .blueDotCount(defectCounts.get(DEFECT_BLUE_DOT))
                    .yellowDotCount(defectCounts.get(DEFECT_YELLOW_DOT))
                    .redDotCount(defectCounts.get(DEFECT_RED_DOT))
                    .pinholeCount(defectCounts.get(DEFECT_PINHOLE))
                    .stripeCount(defectCounts.get(DEFECT_STRIPE))
                    .wrinkleCount(defectCounts.get(DEFECT_WRINKLE))
                    .waveCount(defectCounts.get(DEFECT_WAVE))
                    .otherCount(defectCounts.get(DEFECT_OTHER))
                    .defectSummary(formatDefectSummaryCounts(defectSummaryCounts))
                    .build();
        }
    }

    private static class TrendAccumulator {

        private final String statDate;
        private int confirmedTotal;
        private BigDecimal ngTotal = BigDecimal.ZERO;
        private BigDecimal inputTotal = BigDecimal.ZERO;
        private BigDecimal outputGoodTotal = BigDecimal.ZERO;
        private BigDecimal outputNgTotal = BigDecimal.ZERO;
        private int inspectionTotal;

        TrendAccumulator(String statDate) {
            this.statDate = statDate;
        }

        void add(QmsYieldAnalysisDetailRespVO detail) {
            confirmedTotal++;
            inputTotal = inputTotal.add(defaultDecimal(detail.getInputCount()));
            outputGoodTotal = outputGoodTotal.add(defaultDecimal(detail.getOutputGoodCount()));
            outputNgTotal = outputNgTotal.add(defaultDecimal(detail.getOutputNgCount()));
            inspectionTotal += defaultInt(detail.getInspectionCount());
            ngTotal = outputNgTotal;
        }

        QmsYieldAnalysisRespVO.TrendPoint toRow() {
            return QmsYieldAnalysisRespVO.TrendPoint.builder()
                    .statDate(statDate)
                    .confirmedTotal(confirmedTotal)
                    .ngTotal(ngTotal)
                    .inputTotal(inputTotal)
                    .outputGoodTotal(outputGoodTotal)
                    .outputNgTotal(outputNgTotal)
                    .inspectionTotal(inspectionTotal)
                    .yieldRate(calculateYieldRate(inputTotal, outputGoodTotal))
                    .build();
        }
    }

    private static class SegmentAccumulator {

        private final String groupKey;
        private final String modelCode;
        private final String motherRollNo;
        private final String segmentNo;
        private final List<QmsYieldAnalysisRespVO.ProcessMetric> processMetrics = new ArrayList<>();

        SegmentAccumulator(QmsYieldAnalysisRespVO.SummaryRow summaryRow) {
            this.modelCode = modelCodePrefix(summaryRow.getModelCode());
            this.motherRollNo = blankDisplay(summaryRow.getMotherRollNo());
            this.segmentNo = blankDisplay(summaryRow.getSegmentNo());
            this.groupKey = modelCode + "|" + motherRollNo + "|" + segmentNo;
        }

        void add(QmsYieldAnalysisRespVO.SummaryRow summaryRow) {
            processMetrics.add(QmsYieldAnalysisRespVO.ProcessMetric.builder()
                    .processCode(summaryRow.getProcessCode())
                    .processName(summaryRow.getProcessName())
                    .planNo(summaryRow.getPlanNo())
                    .confirmedTotal(summaryRow.getConfirmedTotal())
                    .goodTotal(summaryRow.getGoodTotal())
                    .ngTotal(summaryRow.getNgTotal())
                    .selfCheckNgTotal(summaryRow.getSelfCheckNgTotal())
                    .submissionNgTotal(summaryRow.getSubmissionNgTotal())
                    .inputTotal(summaryRow.getInputTotal())
                    .outputGoodTotal(summaryRow.getOutputGoodTotal())
                    .outputNgTotal(summaryRow.getOutputNgTotal())
                    .inspectionTotal(summaryRow.getInspectionTotal())
                    .yieldRate(summaryRow.getYieldRate())
                    .targetQualifiedQty(summaryRow.getTargetQualifiedQty())
                    .targetUnit(summaryRow.getTargetUnit())
                    .targetType(summaryRow.getTargetType())
                    .targetAchievementRate(summaryRow.getTargetAchievementRate())
                    .targetDifference(summaryRow.getTargetDifference())
                    .targetMatched(summaryRow.getTargetMatched())
                    .targetReached(summaryRow.getTargetReached())
                    .blackDotCount(summaryRow.getBlackDotCount())
                    .blueDotCount(summaryRow.getBlueDotCount())
                    .yellowDotCount(summaryRow.getYellowDotCount())
                    .redDotCount(summaryRow.getRedDotCount())
                    .pinholeCount(summaryRow.getPinholeCount())
                    .stripeCount(summaryRow.getStripeCount())
                    .wrinkleCount(summaryRow.getWrinkleCount())
                    .waveCount(summaryRow.getWaveCount())
                    .otherCount(summaryRow.getOtherCount())
                    .defectSummary(summaryRow.getDefectSummary())
                    .build());
        }

        QmsYieldAnalysisRespVO.SegmentRow toRow() {
            processMetrics.sort(Comparator.comparingInt(row -> PROCESS_SORT.getOrDefault(row.getProcessCode(), 999)));
            return QmsYieldAnalysisRespVO.SegmentRow.builder()
                    .groupKey(groupKey)
                    .modelCode(modelCode)
                    .motherRollNo(motherRollNo)
                    .segmentNo(segmentNo)
                    .processMetrics(processMetrics)
                    .build();
        }
    }
}
