package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCategoryAnalysisReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCategoryAnalysisRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCategoryAnalysisEventRow;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsDefectCategoryAnalysisMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class QmsDefectCategoryAnalysisServiceImpl implements QmsDefectCategoryAnalysisService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern MOTHER_ROLL_BATCH_PATTERN =
            Pattern.compile("^([A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z])(?:[PQRS])?.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEGMENT_BATCH_PATTERN =
            Pattern.compile("^([A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS]).*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern J_VARIANT_SUFFIX_PATTERN =
            Pattern.compile("-J(?:\\(?[0-9]+\\)?|[0-9]*)$", Pattern.CASE_INSENSITIVE);
    private static final String GROUP_LEVEL_MOTHER = "MOTHER";
    private static final String GROUP_LEVEL_SEGMENT = "SEGMENT";
    private static final String INSPECTION_TYPE_SELF_CHECK = "SELF_CHECK";
    private static final String INSPECTION_TYPE_SUBMISSION = "SUBMISSION";
    private static final String INSPECTION_TYPE_FINAL_INSPECTION = "FINAL_INSPECTION";
    private static final String EVENT_SOURCE_SLITTING_VISUAL = "SLITTING_VISUAL";
    private static final String EVENT_SOURCE_PRESS_SLOT_SELF_CHECK = "PRESS_SLOT_SELF_CHECK";
    private static final String EVENT_SOURCE_ADHESIVE2_SELF_CHECK = "ADHESIVE2_SELF_CHECK";
    private static final String EVENT_SOURCE_CUT_ROUND_SELF_CHECK = "CUT_ROUND_SELF_CHECK";
    private static final String PRE_PROCESS_SELF_CHECK = "PRE_PROCESS_SELF_CHECK";
    private static final String UNCATEGORIZED_SELF_CHECK = "未分类自检异常";
    private static final String UNCATEGORIZED_SUBMISSION = "未分类送检异常";
    private static final String UNCATEGORIZED_FINAL_INSPECTION = "未分类终检异常";
    private static final String FINAL_INSPECTION_BLACK_DOT = "黑点";
    private static final String FINAL_INSPECTION_APPEARANCE_BLACK_DOT = "表观黑点";

    @Resource
    private QmsDefectCategoryAnalysisMapper qmsDefectCategoryAnalysisMapper;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public QmsDefectCategoryAnalysisRespVO getOverview(QmsDefectCategoryAnalysisReqVO reqVO) {
        List<QmsDefectCategoryAnalysisRespVO.DetailRow> details = loadDetails(reqVO);
        String groupLevel = resolveGroupLevel(reqVO);
        Map<String, Integer> categoryTotals = new LinkedHashMap<>();
        Map<String, PivotAccumulator> pivotMap = new LinkedHashMap<>();

        for (QmsDefectCategoryAnalysisRespVO.DetailRow detail : details) {
            int count = positiveQuantity(detail.getDefectCount());
            String category = normalizeCategory(detail.getInspectionCategory(), detail.getInspectionType());
            categoryTotals.merge(category, count, Integer::sum);
            String groupKey = buildGroupKey(detail, groupLevel);
            pivotMap.computeIfAbsent(groupKey, ignored -> new PivotAccumulator(detail, groupLevel))
                    .add(category, count);
        }

        List<QmsDefectCategoryAnalysisRespVO.DefectColumn> defectColumns = categoryTotals.entrySet().stream()
                .sorted(categoryTotalComparator())
                .map(entry -> QmsDefectCategoryAnalysisRespVO.DefectColumn.builder()
                        .key(entry.getKey())
                        .label(entry.getKey())
                        .totalCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
        List<String> orderedCategories = defectColumns.stream()
                .map(QmsDefectCategoryAnalysisRespVO.DefectColumn::getKey)
                .collect(Collectors.toList());
        List<QmsDefectCategoryAnalysisRespVO.PivotRow> rows = pivotMap.values().stream()
                .map(accumulator -> accumulator.toRow(orderedCategories))
                .sorted(pivotRowComparator(groupLevel))
                .collect(Collectors.toList());
        List<QmsDefectCategoryAnalysisRespVO.ParetoRow> paretoRows = buildParetoRows(categoryTotals);
        QmsDefectCategoryAnalysisRespVO.Overview overview = buildOverview(details, categoryTotals);

        return QmsDefectCategoryAnalysisRespVO.builder()
                .overview(overview)
                .groupLevel(groupLevel)
                .defectColumns(defectColumns)
                .rows(rows)
                .paretoRows(paretoRows)
                .build();
    }

    @Override
    public PageResult<QmsDefectCategoryAnalysisRespVO.DetailRow> getDetailPage(QmsDefectCategoryAnalysisReqVO reqVO) {
        List<QmsDefectCategoryAnalysisRespVO.DetailRow> details = loadDetails(reqVO);
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int pageSize = reqVO.getPageSize() == null ? 20 : Math.max(reqVO.getPageSize(), 1);
        int fromIndex = Math.min((pageNo - 1) * pageSize, details.size());
        int toIndex = Math.min(fromIndex + pageSize, details.size());
        return new PageResult<>(new ArrayList<>(details.subList(fromIndex, toIndex)), (long) details.size());
    }

    @Override
    public List<String> getDefectCategoryOptions(QmsDefectCategoryAnalysisReqVO reqVO) {
        return loadDetails(reqVO).stream()
                .map(QmsDefectCategoryAnalysisRespVO.DetailRow::getInspectionCategory)
                .filter(QmsDefectCategoryAnalysisServiceImpl::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<QmsDefectCategoryAnalysisRespVO.DetailRow> loadDetails(QmsDefectCategoryAnalysisReqVO reqVO) {
        DateRange dateRange = resolveDateRange(reqVO);
        List<QmsDefectCategoryAnalysisEventRow> sourceRows = qmsDefectCategoryAnalysisMapper.selectEventRows(
                TenantContextHolder.getTenantId(),
                dateRange.startTime(),
                dateRange.endExclusiveTime(),
                "",
                normalizeFilterCode(reqVO.getInspectionType()));
        List<QmsDefectCategoryAnalysisRespVO.DetailRow> details = new ArrayList<>();
        for (QmsDefectCategoryAnalysisEventRow sourceRow : sourceRows) {
            details.addAll(toDetailRows(sourceRow));
        }
        return details.stream()
                .filter(detail -> matchesReq(detail, reqVO))
                .sorted(detailComparator())
                .collect(Collectors.toList());
    }

    private List<QmsDefectCategoryAnalysisRespVO.DetailRow> toDetailRows(QmsDefectCategoryAnalysisEventRow row) {
        if (shouldParseVisualDefects(row)) {
            List<VisualDefectItem> visualItems = parseVisualDefectItems(row.getVisualResultJson());
            if (!visualItems.isEmpty()) {
                QmsDefectCategoryAnalysisEventRow detailRow = applyPreProcessAttribution(row);
                List<QmsDefectCategoryAnalysisRespVO.DetailRow> details = new ArrayList<>();
                for (VisualDefectItem item : visualItems) {
                    details.add(buildDetailRow(detailRow, item.category(), null, item.category(), item.quantity()));
                }
                return details;
            }
            if (EVENT_SOURCE_SLITTING_VISUAL.equals(row.getEventSource()) && isNg(row.getCheckResult())) {
                return List.of(buildDetailRow(row, UNCATEGORIZED_SELF_CHECK, null, null, positiveQuantity(row.getQuantity())));
            }
            if (EVENT_SOURCE_SLITTING_VISUAL.equals(row.getEventSource())) {
                return List.of();
            }
        }

        if (shouldApplyPreProcessAttribution(row)) {
            row = applyPreProcessAttribution(row);
        }
        String category = firstNotBlank(row.getInspectionCategory(), row.getDefectName(), row.getDefectCode(),
                uncategorizedCategory(row.getInspectionType()));
        return List.of(buildDetailRow(row, category, row.getDefectCode(), row.getDefectName(),
                positiveQuantity(row.getQuantity())));
    }

    private QmsDefectCategoryAnalysisRespVO.DetailRow buildDetailRow(QmsDefectCategoryAnalysisEventRow row,
                                                                    String category,
                                                                    String defectCode,
                                                                    String defectName,
                                                                    Integer quantity) {
        String scanConfirmPieceNo = firstNotBlank(row.getScanConfirmPieceNo(), row.getProcessPieceNo());
        String processPieceNo = firstNotBlank(row.getProcessPieceNo(), scanConfirmPieceNo);
        String segmentBatchNo = firstSegmentBatchNo(row.getSegmentBatchNo(), row.getMotherRollBatchNo(),
                scanConfirmPieceNo, processPieceNo);
        String motherRollBatchNo = firstMotherRollBatchNo(row.getMotherRollBatchNo(), row.getSegmentBatchNo(),
                segmentBatchNo, scanConfirmPieceNo, processPieceNo);
        String modelCode = firstNotBlank(row.getModelCode(), motherRollBatchNo);
        String modelSeriesCode = modelSeriesCode(modelCode);
        String inspectionType = firstNotBlank(row.getInspectionType(), INSPECTION_TYPE_SELF_CHECK);
        String normalizedCategory = normalizeCategory(category, inspectionType);
        int defectCount = positiveQuantity(quantity);
        String eventKey = String.join(":",
                safe(row.getSourceTable()),
                String.valueOf(row.getSourceId()),
                safe(row.getEventSource()),
                normalizedCategory,
                safe(defectCode));

        return QmsDefectCategoryAnalysisRespVO.DetailRow.builder()
                .eventKey(eventKey)
                .eventSource(row.getEventSource())
                .processCode(row.getProcessCode())
                .processName(firstNotBlank(row.getProcessName(), processName(row.getProcessCode())))
                .inspectionType(inspectionType)
                .inspectionTypeName(inspectionTypeName(inspectionType))
                .planNo(blankToDash(row.getPlanNo()))
                .motherRollBatchNo(blankToDash(motherRollBatchNo))
                .segmentBatchNo(blankToDash(segmentBatchNo))
                .scanConfirmPieceNo(blankToDash(scanConfirmPieceNo))
                .processPieceNo(blankToDash(processPieceNo))
                .scanConfirmTime(formatDateTime(row.getScanConfirmTime()))
                .inspectionTime(formatDateTime(row.getInspectionTime()))
                .eventTime(formatDateTime(row.getEventTime()))
                .modelSeriesCode(blankToDash(modelSeriesCode))
                .modelCode(blankToDash(row.getModelCode()))
                .materialCode(blankToDash(row.getMaterialCode()))
                .materialName(blankToDash(row.getMaterialName()))
                .inspectionCategory(normalizedCategory)
                .defectCode(blankToDash(defectCode))
                .defectName(blankToDash(firstNotBlank(defectName, category)))
                .defectLevel(blankToDash(row.getDefectLevel()))
                .inspectorName(blankToDash(row.getInspectorName()))
                .inspectionNo(blankToDash(row.getInspectionNo()))
                .checkResult(blankToDash(row.getCheckResult()))
                .remark(blankToDash(row.getRemark()))
                .defectCount(defectCount)
                .sourceTable(row.getSourceTable())
                .sourceId(row.getSourceId())
                .inspectionId(row.getInspectionId())
                .build();
    }

    private QmsDefectCategoryAnalysisRespVO.Overview buildOverview(
            List<QmsDefectCategoryAnalysisRespVO.DetailRow> details,
            Map<String, Integer> categoryTotals) {
        int totalCount = details.stream().mapToInt(detail -> positiveQuantity(detail.getDefectCount())).sum();
        int selfCheckCount = details.stream()
                .filter(detail -> INSPECTION_TYPE_SELF_CHECK.equals(detail.getInspectionType()))
                .mapToInt(detail -> positiveQuantity(detail.getDefectCount()))
                .sum();
        int submissionCount = details.stream()
                .filter(detail -> INSPECTION_TYPE_SUBMISSION.equals(detail.getInspectionType()))
                .mapToInt(detail -> positiveQuantity(detail.getDefectCount()))
                .sum();
        Map.Entry<String, Integer> primary = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        return QmsDefectCategoryAnalysisRespVO.Overview.builder()
                .totalCount(totalCount)
                .selfCheckCount(selfCheckCount)
                .submissionCount(submissionCount)
                .motherRollCount(distinctCount(details, QmsDefectCategoryAnalysisRespVO.DetailRow::getMotherRollBatchNo))
                .segmentCount(distinctCount(details, QmsDefectCategoryAnalysisRespVO.DetailRow::getSegmentBatchNo))
                .pieceCount(distinctCount(details, QmsDefectCategoryAnalysisRespVO.DetailRow::getScanConfirmPieceNo))
                .primaryDefectCategory(primary == null ? "-" : primary.getKey())
                .primaryDefectCount(primary == null ? 0 : primary.getValue())
                .build();
    }

    private List<QmsDefectCategoryAnalysisRespVO.ParetoRow> buildParetoRows(Map<String, Integer> categoryTotals) {
        int total = categoryTotals.values().stream().mapToInt(QmsDefectCategoryAnalysisServiceImpl::positiveQuantity).sum();
        List<Map.Entry<String, Integer>> sortedEntries = categoryTotals.entrySet().stream()
                .sorted(categoryTotalComparator())
                .collect(Collectors.toList());
        List<QmsDefectCategoryAnalysisRespVO.ParetoRow> rows = new ArrayList<>();
        int cumulative = 0;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            int count = positiveQuantity(entry.getValue());
            cumulative += count;
            rows.add(QmsDefectCategoryAnalysisRespVO.ParetoRow.builder()
                    .defectCategory(entry.getKey())
                    .defectCount(count)
                    .ratio(ratio(count, total))
                    .cumulativeRatio(ratio(cumulative, total))
                    .build());
        }
        return rows;
    }

    private List<VisualDefectItem> parseVisualDefectItems(String visualResultJson) {
        JsonNode root = parseJson(visualResultJson);
        if (root == null || root.isNull()) {
            return List.of();
        }
        List<VisualDefectItem> items = new ArrayList<>();
        collectVisualDefectItems(root, items);
        return items;
    }

    private void collectVisualDefectItems(JsonNode source, List<VisualDefectItem> target) {
        if (source == null || source.isNull()) {
            return;
        }
        if (source.isTextual()) {
            JsonNode parsed = parseJson(source.asText());
            if (parsed != null) {
                collectVisualDefectItems(parsed, target);
            }
            return;
        }
        if (source.isArray()) {
            for (JsonNode item : source) {
                collectVisualDefectItems(item, target);
            }
            return;
        }
        if (!source.isObject()) {
            return;
        }

        JsonNode rows = firstNode(source, "visualItems", "visualInspectionItems", "visualResults",
                "defects", "defectItems");
        if (rows != null) {
            collectVisualDefectItems(rows, target);
            return;
        }

        String category = firstText(source, "itemName", "defectName", "name", "checkItem", "label");
        if (hasText(category)) {
            if (isActiveVisualItem(source)) {
                target.add(new VisualDefectItem(category.trim(), resolveQuantity(source)));
            }
        }
    }

    private boolean shouldParseVisualDefects(QmsDefectCategoryAnalysisEventRow row) {
        return EVENT_SOURCE_SLITTING_VISUAL.equals(row.getEventSource())
                || EVENT_SOURCE_PRESS_SLOT_SELF_CHECK.equals(row.getEventSource())
                || EVENT_SOURCE_ADHESIVE2_SELF_CHECK.equals(row.getEventSource())
                || EVENT_SOURCE_CUT_ROUND_SELF_CHECK.equals(row.getEventSource());
    }

    private QmsDefectCategoryAnalysisEventRow applyPreProcessAttribution(QmsDefectCategoryAnalysisEventRow row) {
        if (!shouldApplyPreProcessAttribution(row)) {
            return row;
        }
        JsonNode root = parseJson(row.getVisualResultJson());
        if (root == null || !isPreProcessSelfCheckAttribution(root)) {
            return row;
        }
        String processCode = normalizeFilterCode(firstText(root, "ngAttributionProcessCode"));
        String processName = firstText(root, "ngAttributionProcessName");
        if (!hasText(processCode) && !hasText(processName)) {
            return row;
        }
        if (hasText(processCode)) {
            row.setProcessCode(processCode);
        }
        row.setProcessName(firstNotBlank(processName, processName(row.getProcessCode())));
        return row;
    }

    private boolean shouldApplyPreProcessAttribution(QmsDefectCategoryAnalysisEventRow row) {
        return EVENT_SOURCE_PRESS_SLOT_SELF_CHECK.equals(row.getEventSource())
                || EVENT_SOURCE_ADHESIVE2_SELF_CHECK.equals(row.getEventSource())
                || EVENT_SOURCE_CUT_ROUND_SELF_CHECK.equals(row.getEventSource());
    }

    private boolean isPreProcessSelfCheckAttribution(JsonNode root) {
        JsonNode flag = firstNode(root, "preProcessSelfCheckAbnormal");
        if (flag != null && isActiveValue(flag)) {
            return true;
        }
        return PRE_PROCESS_SELF_CHECK.equalsIgnoreCase(firstText(root, "ngAttributionType").trim());
    }

    private boolean isActiveVisualItem(JsonNode item) {
        String result = firstText(item, "result", "checkResult", "value", "status", "judgment");
        if (hasText(result)) {
            return isNg(result) || isActiveText(result);
        }
        JsonNode checked = firstNode(item, "checked", "selected", "active", "abnormal", "ng", "value");
        return checked != null && isActiveValue(checked);
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
        if (value.isTextual()) {
            return isNg(value.asText()) || isActiveText(value.asText());
        }
        return false;
    }

    private boolean isActiveText(String value) {
        String normalized = safe(value).trim().toUpperCase(Locale.ROOT);
        return Set.of("TRUE", "YES", "Y", "1", "异常", "不合格", "有").contains(normalized);
    }

    private int resolveQuantity(JsonNode item) {
        for (String field : List.of("quantity", "qty", "count", "defectCount", "abnormalCount")) {
            JsonNode value = item.get(field);
            if (value != null && value.isNumber()) {
                return Math.max(value.asInt(), 1);
            }
        }
        return 1;
    }

    private int resolveActiveQuantity(JsonNode value) {
        return value != null && value.isNumber() ? Math.max(value.asInt(), 1) : 1;
    }

    private JsonNode firstNode(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
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
        if (value == null || value.isNull()) {
            return "";
        }
        return value.isTextual() ? value.asText() : value.asText("");
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

    private boolean matchesReq(QmsDefectCategoryAnalysisRespVO.DetailRow detail,
                               QmsDefectCategoryAnalysisReqVO reqVO) {
        if (!matchesFilterCode(detail.getProcessCode(), reqVO.getProcessCode())) {
            return false;
        }
        if (!matchesFilterCode(detail.getInspectionType(), reqVO.getInspectionType())) {
            return false;
        }
        if (!matchesText(detail.getMotherRollBatchNo(), normalizeMotherRollFilter(reqVO.getMotherRollBatchNo()))) {
            return false;
        }
        if (!matchesText(detail.getSegmentBatchNo(), normalizeSegmentFilter(reqVO.getSegmentBatchNo()))) {
            return false;
        }
        if (!matchesText(detail.getScanConfirmPieceNo(), reqVO.getPieceNo())
                && !matchesText(detail.getProcessPieceNo(), reqVO.getPieceNo())) {
            return false;
        }
        if (!matchesModel(detail, reqVO.getModelCode())) {
            return false;
        }
        if (hasText(reqVO.getDefectCategory())) {
            String source = String.join(" ", safe(detail.getInspectionCategory()), safe(detail.getDefectCode()),
                    safe(detail.getDefectName()));
            if (!matchesText(source, reqVO.getDefectCategory())) {
                return false;
            }
        }
        if (hasText(reqVO.getKeyword())) {
            String source = String.join(" ", safe(detail.getPlanNo()), safe(detail.getMotherRollBatchNo()),
                    safe(detail.getSegmentBatchNo()), safe(detail.getScanConfirmPieceNo()), safe(detail.getProcessPieceNo()),
                    safe(detail.getProcessName()), safe(detail.getInspectionTypeName()), safe(detail.getInspectorName()),
                    safe(detail.getInspectionCategory()), safe(detail.getDefectCode()), safe(detail.getMaterialCode()),
                    safe(detail.getMaterialName()), safe(detail.getModelCode()));
            return matchesText(source, reqVO.getKeyword());
        }
        return true;
    }

    private boolean matchesModel(QmsDefectCategoryAnalysisRespVO.DetailRow detail, String modelCode) {
        if (!hasText(modelCode)) {
            return true;
        }
        String keyword = modelCode.trim().toUpperCase(Locale.ROOT);
        String modelSeries = safe(detail.getModelSeriesCode()).toUpperCase(Locale.ROOT);
        String fullModel = safe(detail.getModelCode()).toUpperCase(Locale.ROOT);
        return modelSeries.contains(modelSeriesCode(keyword)) || fullModel.contains(keyword);
    }

    private boolean matchesFilterCode(String actualValue, String expectedValue) {
        String expected = normalizeFilterCode(expectedValue);
        if (!hasText(expected)) {
            return true;
        }
        return expected.equals(normalizeFilterCode(actualValue));
    }

    private DateRange resolveDateRange(QmsDefectCategoryAnalysisReqVO reqVO) {
        LocalDate defaultDate = LocalDate.now();
        LocalDate startDate = reqVO.getStartDate() == null ? defaultDate : reqVO.getStartDate();
        LocalDate endDate = reqVO.getEndDate() == null ? startDate : reqVO.getEndDate();
        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }
        return new DateRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private String resolveGroupLevel(QmsDefectCategoryAnalysisReqVO reqVO) {
        String groupLevel = normalizeFilterCode(reqVO.getGroupLevel());
        if (GROUP_LEVEL_SEGMENT.equals(groupLevel)) {
            return GROUP_LEVEL_SEGMENT;
        }
        return GROUP_LEVEL_MOTHER;
    }

    private String buildGroupKey(QmsDefectCategoryAnalysisRespVO.DetailRow detail, String groupLevel) {
        if (GROUP_LEVEL_SEGMENT.equals(groupLevel)) {
            return String.join("|", blankDisplay(detail.getModelSeriesCode()), blankDisplay(detail.getMotherRollBatchNo()),
                    blankDisplay(detail.getSegmentBatchNo()));
        }
        return String.join("|", blankDisplay(detail.getModelSeriesCode()), blankDisplay(detail.getMotherRollBatchNo()));
    }

    private Comparator<QmsDefectCategoryAnalysisRespVO.PivotRow> pivotRowComparator(String groupLevel) {
        Comparator<QmsDefectCategoryAnalysisRespVO.PivotRow> comparator =
                Comparator.comparing(QmsDefectCategoryAnalysisRespVO.PivotRow::getModelSeriesCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(QmsDefectCategoryAnalysisRespVO.PivotRow::getMotherRollBatchNo,
                                Comparator.nullsLast(String::compareTo));
        if (GROUP_LEVEL_SEGMENT.equals(groupLevel)) {
            comparator = comparator.thenComparing(QmsDefectCategoryAnalysisRespVO.PivotRow::getSegmentBatchNo,
                    Comparator.nullsLast(String::compareTo));
        }
        return comparator;
    }

    private Comparator<Map.Entry<String, Integer>> categoryTotalComparator() {
        return Comparator.<Map.Entry<String, Integer>>comparingInt(entry -> positiveQuantity(entry.getValue()))
                .reversed()
                .thenComparing(Map.Entry::getKey);
    }

    private Comparator<QmsDefectCategoryAnalysisRespVO.DetailRow> detailComparator() {
        return Comparator.comparing(QmsDefectCategoryAnalysisRespVO.DetailRow::getEventTime,
                        Comparator.nullsLast(String::compareTo))
                .reversed()
                .thenComparing(QmsDefectCategoryAnalysisRespVO.DetailRow::getMotherRollBatchNo,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(QmsDefectCategoryAnalysisRespVO.DetailRow::getSegmentBatchNo,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(QmsDefectCategoryAnalysisRespVO.DetailRow::getScanConfirmPieceNo,
                        Comparator.nullsLast(String::compareTo));
    }

    private String firstSegmentBatchNo(String... values) {
        for (String value : values) {
            String segmentBatchNo = deriveSegmentBatchNo(value);
            if (hasText(segmentBatchNo)) {
                return segmentBatchNo;
            }
        }
        return "";
    }

    private String firstMotherRollBatchNo(String... values) {
        for (String value : values) {
            String motherRollBatchNo = deriveMotherRollBatchNo(value);
            if (hasText(motherRollBatchNo)) {
                return motherRollBatchNo;
            }
        }
        return "";
    }

    private String deriveSegmentBatchNo(String pieceNo) {
        if (!hasText(pieceNo) || "-".equals(pieceNo.trim())) {
            return "";
        }
        String value = normalizeBatchText(pieceNo);
        Matcher matcher = SEGMENT_BATCH_PATTERN.matcher(value);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        String withoutJVariant = stripJVariant(value);
        if (!withoutJVariant.equals(value)) {
            Matcher strippedMatcher = SEGMENT_BATCH_PATTERN.matcher(withoutJVariant);
            if (strippedMatcher.matches()) {
                return strippedMatcher.group(1);
            }
            return MOTHER_ROLL_BATCH_PATTERN.matcher(withoutJVariant).matches() ? "" : withoutJVariant;
        }
        return "";
    }

    private String deriveMotherRollBatchNo(String value) {
        if (!hasText(value) || "-".equals(value.trim())) {
            return "";
        }
        String normalized = stripJVariant(normalizeBatchText(value));
        Matcher matcher = MOTHER_ROLL_BATCH_PATTERN.matcher(normalized);
        return matcher.matches() ? matcher.group(1) : normalized;
    }

    private String normalizeMotherRollFilter(String value) {
        if (!hasText(value)) {
            return "";
        }
        String motherRollBatchNo = deriveMotherRollBatchNo(value);
        return hasText(motherRollBatchNo) ? motherRollBatchNo : value.trim();
    }

    private String normalizeSegmentFilter(String value) {
        if (!hasText(value)) {
            return "";
        }
        String segmentBatchNo = deriveSegmentBatchNo(value);
        return hasText(segmentBatchNo) ? segmentBatchNo : value.trim();
    }

    private String normalizeBatchText(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String stripJVariant(String value) {
        return J_VARIANT_SUFFIX_PATTERN.matcher(value.trim()).replaceFirst("");
    }

    private String normalizeCategory(String category, String inspectionType) {
        String normalized = firstNotBlank(category, uncategorizedCategory(inspectionType));
        if ("OK".equalsIgnoreCase(normalized) || "PASS".equalsIgnoreCase(normalized)) {
            return uncategorizedCategory(inspectionType);
        }
        String trimmed = normalized.trim();
        if (INSPECTION_TYPE_FINAL_INSPECTION.equals(inspectionType)
                && FINAL_INSPECTION_BLACK_DOT.equals(trimmed)) {
            return FINAL_INSPECTION_APPEARANCE_BLACK_DOT;
        }
        return trimmed;
    }

    private String inspectionTypeName(String inspectionType) {
        if (INSPECTION_TYPE_FINAL_INSPECTION.equals(inspectionType)) {
            return "终检";
        }
        if (INSPECTION_TYPE_SUBMISSION.equals(inspectionType)) {
            return "送检";
        }
        return "自检";
    }

    private String uncategorizedCategory(String inspectionType) {
        if (INSPECTION_TYPE_FINAL_INSPECTION.equals(inspectionType)) {
            return UNCATEGORIZED_FINAL_INSPECTION;
        }
        if (INSPECTION_TYPE_SUBMISSION.equals(inspectionType)) {
            return UNCATEGORIZED_SUBMISSION;
        }
        return UNCATEGORIZED_SELF_CHECK;
    }

    private String processName(String processCode) {
        return switch (safe(processCode).trim().toUpperCase(Locale.ROOT)) {
            case "SLITTING" -> "分切";
            case "PRESS_SLOT" -> "压槽";
            case "ADHESIVE2" -> "粘胶2";
            case "CUT_ROUND" -> "裁切";
            default -> "-";
        };
    }

    private String normalizeFilterCode(String value) {
        if (!hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String modelSeriesCode(String value) {
        String display = blankDisplay(value);
        return "-".equals(display) || display.length() <= 3 ? display : display.substring(0, 3);
    }

    private BigDecimal ratio(int count, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private int distinctCount(List<QmsDefectCategoryAnalysisRespVO.DetailRow> details,
                              java.util.function.Function<QmsDefectCategoryAnalysisRespVO.DetailRow, String> getter) {
        Set<String> values = new LinkedHashSet<>();
        for (QmsDefectCategoryAnalysisRespVO.DetailRow detail : details) {
            String value = getter.apply(detail);
            if (hasText(value) && !"-".equals(value)) {
                values.add(value);
            }
        }
        return values.size();
    }

    private boolean isNg(String value) {
        String normalized = safe(value).trim().toUpperCase(Locale.ROOT);
        return Set.of("NG", "FAIL", "FAILED", "ABNORMAL", "不合格", "异常").contains(normalized);
    }

    private boolean matchesText(String source, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        return safe(source).toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
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

    private String blankDisplay(String value) {
        return hasText(value) && !"-".equals(value.trim()) ? value.trim() : "-";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATETIME_FORMATTER.format(value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static int positiveQuantity(Integer quantity) {
        return quantity == null || quantity <= 0 ? 1 : quantity;
    }

    private record DateRange(LocalDateTime startTime, LocalDateTime endExclusiveTime) {
    }

    private record VisualDefectItem(String category, Integer quantity) {
    }

    private static class PivotAccumulator {

        private final String groupKey;
        private final String modelSeriesCode;
        private final String modelCode;
        private final String motherRollBatchNo;
        private final String segmentBatchNo;
        private int totalCount;
        private final Map<String, Integer> defectCounts = new LinkedHashMap<>();

        PivotAccumulator(QmsDefectCategoryAnalysisRespVO.DetailRow detail, String groupLevel) {
            this.groupKey = GROUP_LEVEL_SEGMENT.equals(groupLevel)
                    ? String.join("|", detail.getModelSeriesCode(), detail.getMotherRollBatchNo(), detail.getSegmentBatchNo())
                    : String.join("|", detail.getModelSeriesCode(), detail.getMotherRollBatchNo());
            this.modelSeriesCode = detail.getModelSeriesCode();
            this.modelCode = detail.getModelCode();
            this.motherRollBatchNo = detail.getMotherRollBatchNo();
            this.segmentBatchNo = GROUP_LEVEL_SEGMENT.equals(groupLevel) ? detail.getSegmentBatchNo() : "";
        }

        void add(String category, int count) {
            totalCount += count;
            defectCounts.merge(category, count, Integer::sum);
        }

        QmsDefectCategoryAnalysisRespVO.PivotRow toRow(List<String> orderedCategories) {
            Map<String, Integer> orderedCounts = new LinkedHashMap<>();
            for (String category : orderedCategories) {
                orderedCounts.put(category, defectCounts.getOrDefault(category, 0));
            }
            return QmsDefectCategoryAnalysisRespVO.PivotRow.builder()
                    .groupKey(groupKey)
                    .modelSeriesCode(modelSeriesCode)
                    .modelCode(modelCode)
                    .motherRollBatchNo(motherRollBatchNo)
                    .segmentBatchNo(segmentBatchNo)
                    .totalCount(totalCount)
                    .defectCounts(orderedCounts)
                    .build();
        }
    }
}
