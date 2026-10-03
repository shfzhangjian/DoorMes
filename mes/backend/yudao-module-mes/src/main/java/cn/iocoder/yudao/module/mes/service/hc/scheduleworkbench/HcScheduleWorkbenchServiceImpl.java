package cn.iocoder.yudao.module.mes.service.hc.scheduleworkbench;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.service.hc.planorder.HcPlanBatchPreviewService;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcWetWaterChangeApplyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.scheduleworkbench.HcScheduleWorkbenchMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcScheduleWorkbenchServiceImpl implements HcScheduleWorkbenchService {

    private static final int MAX_DATE_COLUMN_COUNT = 31;
    private static final int EXPORT_DATE_COLUMN_START_INDEX = 9;
    private static final int STAGE_FORMULA = 10;
    private static final int STAGE_WET = 20;
    private static final int STAGE_GRINDING_SECOND = 40;
    private static final int STAGE_ADHESIVE1 = 50;
    private static final int STAGE_SLITTING = 60;
    private static final int STAGE_PRESS_SLOT = 70;
    private static final int STAGE_ADHESIVE2 = 80;
    private static final int STAGE_CUT_ROUND = 90;
    private static final int STAGE_PACKAGE_IN = 100;
    private static final int STAGE_PACKAGE_OUT = 110;
    private static final String PLAN_MODE_DISCRETE_POST = "DISCRETE_POST";
    private static final String SOURCE_TYPE_DISCRETE_NG = "NG_INVENTORY";
    private static final String SOURCE_TYPE_DISCRETE_WIP = "DISCRETE_WIP";
    private static final String PROD_TYPE_TRIAL_PROCESS = "TRIAL_PROCESS";
    private static final String LOCK_MARK_DISCRETE_POST = "DISCRETE_POST_PROCESS";
    private static final Set<String> SIMPLE_MODE_OPERATION_NAMES = Set.of(
            "配料", "湿法", "磨皮", "磨皮1", "磨皮2", "粘胶1", "分切", "压槽", "粘胶2", "裁切");
    private static final Pattern ADHESIVE_SUFFIX_PATTERN = Pattern.compile("-J\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PIECE_SUFFIX_PATTERN = Pattern.compile("^(.+[PQRS])\\d{3,}[A-Z]?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CUT_PIECE_SUFFIX_PATTERN = Pattern.compile("^(.+[PQRS]\\d{3,})[A-Z]$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SEGMENT_STANDARD_PATTERN = Pattern.compile("^[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]([PQRS])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SEGMENT_END_PATTERN = Pattern.compile("([PQRS])(?:-\\w+)?$", Pattern.CASE_INSENSITIVE);

    @Resource
    private HcScheduleWorkbenchMapper scheduleWorkbenchMapper;

    @Resource
    private HcWetWaterChangeApplyMapper wetWaterChangeApplyMapper;

    @Resource
    private HcPlanBatchPreviewService planBatchPreviewService;

    @Override
    public HcScheduleWorkbenchRespVO getWorkbench(HcScheduleWorkbenchReqVO reqVO) {
        normalizeReq(reqVO);
        List<HcScheduleWorkbenchMapper.PlanRow> planRows = scheduleWorkbenchMapper.selectPlanRows(reqVO);
        List<Long> planIds = planRows.stream()
                .map(HcScheduleWorkbenchMapper.PlanRow::planId)
                .filter(Objects::nonNull)
                .toList();
        List<String> dateColumns = buildDateColumns(reqVO.getStartDate(), reqVO.getEndDate());
        Map<Long, HcScheduleWorkbenchRespVO.Row> rowMap = buildRowMap(planRows, dateColumns);
        planBatchPreviewService.previewMissingRootBatches(planIds)
                .forEach((planId, batchNo) -> rowMap.get(planId).setMotherBatchPreviewNo(batchNo));
        if (!planIds.isEmpty()) {
            List<HcScheduleWorkbenchMapper.PlanOperationScopeRow> planOperationRows =
                    scheduleWorkbenchMapper.selectPlanOperationScopeRows(planIds);
            fillProgressQuantities(rowMap, scheduleWorkbenchMapper.selectProgressQuantityRows(planIds));
            fillChangeoverSummaries(rowMap,
                    scheduleWorkbenchMapper.selectAdhesive2ModelChangeoverSummaryRows(planIds),
                    scheduleWorkbenchMapper.selectCutRoundSizeSourceRows(planIds));
            fillScheduledOperations(rowMap, scheduleWorkbenchMapper.selectScheduleOperationRows(
                    planIds, reqVO.getStartDate(), reqVO.getEndDate(), reqVO.getOperationName()));
            fillFacts(rowMap, scheduleWorkbenchMapper.selectFactRows(
                    planIds, reqVO.getStartDate(), reqVO.getEndDate(), reqVO.getOperationName()));
            fillOperationCompletion(rowMap, planOperationRows,
                    scheduleWorkbenchMapper.selectOperationCompletionRows(planIds),
                    scheduleWorkbenchMapper.selectSegmentProgressRows(planIds));
        }

        HcScheduleWorkbenchRespVO respVO = new HcScheduleWorkbenchRespVO();
        respVO.setStartDate(reqVO.getStartDate());
        respVO.setEndDate(reqVO.getEndDate());
        respVO.setDateColumns(dateColumns);
        respVO.setMetrics(buildMetrics(reqVO));
        respVO.setRows(new ArrayList<>(rowMap.values()));
        respVO.setNgSummaries(buildNgSummaries(reqVO.getStartDate(), reqVO.getEndDate()));
        respVO.setWetWaterChangeApplies(buildWetWaterChangeApplies(reqVO.getStartDate(), reqVO.getEndDate()));
        return respVO;
    }

    @Override
    public HcScheduleWorkbenchExportRespVO getWorkbenchExportData(HcScheduleWorkbenchReqVO reqVO) {
        HcScheduleWorkbenchRespVO workbench = getWorkbench(reqVO);
        HcScheduleWorkbenchExportRespVO exportRespVO = new HcScheduleWorkbenchExportRespVO();
        exportRespVO.setHead(buildExportHead(workbench.getDateColumns()));
        exportRespVO.setRows(buildExportRows(workbench));
        exportRespVO.setScheduleColumnStartIndex(EXPORT_DATE_COLUMN_START_INDEX);
        exportRespVO.setRowStyles(buildExportRowStyles(workbench));
        return exportRespVO;
    }

    @Override
    public List<HcScheduleWorkbenchRespVO.ChangeoverDetail> getChangeoverDetails(Long planId, String type) {
        if (planId == null) {
            return List.of();
        }
        String normalizedType = StrUtil.trimToEmpty(type).toUpperCase(Locale.ROOT);
        if ("SIZE".equals(normalizedType)) {
            return scheduleWorkbenchMapper.selectCutRoundSizeChangeoverDetailRows(planId).stream()
                    .map(this::buildSizeChangeoverDetail)
                    .filter(Objects::nonNull)
                    .toList();
        }
        if ("MODEL".equals(normalizedType)) {
            return scheduleWorkbenchMapper.selectAdhesive2ModelChangeoverDetailRows(planId).stream()
                    .map(this::buildModelChangeoverDetail)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return List.of();
    }

    private List<List<String>> buildExportHead(List<String> dateColumns) {
        List<List<String>> head = new ArrayList<>();
        List.of("序号", "计划时间", "母批批号", "计划类型", "型号", "执行要求", "规格", "计划交期", "完成情况")
                .forEach(name -> head.add(List.of(name)));
        for (String date : emptyListIfNull(dateColumns)) {
            head.add(List.of(exportDateLabel(date)));
        }
        return head;
    }

    private List<List<String>> buildExportRows(HcScheduleWorkbenchRespVO workbench) {
        List<List<String>> exportRows = new ArrayList<>();
        List<String> dateColumns = emptyListIfNull(workbench.getDateColumns());
        List<HcScheduleWorkbenchRespVO.Row> rows = emptyListIfNull(workbench.getRows());
        for (int i = 0; i < rows.size(); i++) {
            HcScheduleWorkbenchRespVO.Row row = rows.get(i);
            List<String> exportRow = new ArrayList<>();
            exportRow.add(String.valueOf(i + 1));
            exportRow.add(joinNonBlank(dateText(row.getPlanDate()), row.getPlanNo()));
            exportRow.add(rowMotherBatchNo(row));
            exportRow.add(prodTypeLabel(row));
            exportRow.add(exportChangeoverText(row.getModelCode(), row.getModelChangeoverSummaries()));
            exportRow.add(defaultText(row.getRequirement()));
            exportRow.add(exportChangeoverText(row.getSizeSpec(), row.getSizeChangeoverSummaries()));
            exportRow.add(dateText(row.getDueDate()));
            exportRow.add(defaultText(row.getCompletionStatus()));
            Map<String, HcScheduleWorkbenchRespVO.Cell> cells =
                    row.getCells() == null ? Map.of() : row.getCells();
            for (String date : dateColumns) {
                exportRow.add(exportCellText(cells.get(date)));
            }
            exportRows.add(exportRow);
        }
        return exportRows;
    }

    private String exportChangeoverText(String planValue,
                                        List<HcScheduleWorkbenchRespVO.ChangeoverSummary> summaries) {
        String planText = defaultText(planValue);
        List<String> lines = new ArrayList<>();
        lines.add(StrUtil.isBlank(planText) ? "-" : planText);
        emptyListIfNull(summaries).stream()
                .filter(summary -> summary != null && defaultInt(summary.getQty()) > 0
                        && StrUtil.isNotBlank(summary.getActualValue()))
                .forEach(summary -> lines.add("改 " + summary.getActualValue()
                        + "（" + defaultInt(summary.getQty()) + StrUtil.blankToDefault(summary.getUnit(), "片") + "）"));
        return String.join("\n", lines);
    }

    private String exportCellText(HcScheduleWorkbenchRespVO.Cell cell) {
        List<HcScheduleWorkbenchExportRespVO.ExportCellBlock> blocks = buildExportCellBlocks(cell);
        if (!blocks.isEmpty()) {
            return blocks.stream()
                    .map(block -> "■■ " + block.getText())
                    .collect(Collectors.joining("\n"));
        }
        return cell == null ? "" : "-";
    }

    private List<HcScheduleWorkbenchExportRespVO.ExportRowStyle> buildExportRowStyles(
            HcScheduleWorkbenchRespVO workbench) {
        List<HcScheduleWorkbenchExportRespVO.ExportRowStyle> rowStyles = new ArrayList<>();
        List<String> dateColumns = emptyListIfNull(workbench.getDateColumns());
        List<HcScheduleWorkbenchRespVO.Row> rows = emptyListIfNull(workbench.getRows());
        for (int i = 0; i < rows.size(); i++) {
            HcScheduleWorkbenchRespVO.Row row = rows.get(i);
            Map<String, HcScheduleWorkbenchRespVO.Cell> cells =
                    row.getCells() == null ? Map.of() : row.getCells();
            Map<Integer, HcScheduleWorkbenchExportRespVO.ExportCellStyle> cellStyles = new LinkedHashMap<>();
            int maxBlockCount = 1;
            for (int dateIndex = 0; dateIndex < dateColumns.size(); dateIndex++) {
                List<HcScheduleWorkbenchExportRespVO.ExportCellBlock> blocks =
                        buildExportCellBlocks(cells.get(dateColumns.get(dateIndex)));
                if (blocks.isEmpty()) {
                    continue;
                }
                HcScheduleWorkbenchExportRespVO.ExportCellStyle cellStyle =
                        new HcScheduleWorkbenchExportRespVO.ExportCellStyle();
                cellStyle.setBlocks(blocks);
                cellStyles.put(EXPORT_DATE_COLUMN_START_INDEX + dateIndex, cellStyle);
                maxBlockCount = Math.max(maxBlockCount, blocks.size());
            }
            if (cellStyles.isEmpty()) {
                continue;
            }
            HcScheduleWorkbenchExportRespVO.ExportRowStyle rowStyle =
                    new HcScheduleWorkbenchExportRespVO.ExportRowStyle();
            rowStyle.setRowIndex(i);
            rowStyle.setRowHeightInPoints(exportRowHeightInPoints(maxBlockCount));
            rowStyle.setCellStyles(cellStyles);
            rowStyles.add(rowStyle);
        }
        return rowStyles;
    }

    private float exportRowHeightInPoints(int blockCount) {
        return Math.min(409F, Math.max(28F, blockCount * 18F + 10F));
    }

    private List<HcScheduleWorkbenchExportRespVO.ExportCellBlock> buildExportCellBlocks(
            HcScheduleWorkbenchRespVO.Cell cell) {
        if (cell == null) {
            return List.of();
        }
        List<HcScheduleWorkbenchRespVO.PlannedOperation> plannedOperations = plannedOperationDetails(cell);
        List<HcScheduleWorkbenchExportRespVO.ExportCellBlock> factBlocks = sortedFacts(cell).stream()
                .filter(item -> !isHiddenCardOperation(item.getOperationName()))
                .filter(item -> isSimpleModeOperation(item.getOperationName()))
                .map(item -> buildFactExportBlock(item, findPlannedOperation(plannedOperations, item.getOperationName())))
                .filter(Objects::nonNull)
                .toList();
        if (!factBlocks.isEmpty()) {
            return factBlocks;
        }
        return List.of();
    }

    private HcScheduleWorkbenchExportRespVO.ExportCellBlock buildFactExportBlock(
            HcScheduleWorkbenchRespVO.Fact fact,
            HcScheduleWorkbenchRespVO.PlannedOperation plannedOperation) {
        String operationName = defaultText(fact.getOperationName());
        if (StrUtil.isBlank(operationName)) {
            return null;
        }
        HcScheduleWorkbenchExportRespVO.ExportCellBlock block =
                new HcScheduleWorkbenchExportRespVO.ExportCellBlock();
        String segmentText = segmentMark(fact.getSegmentBatchNo());
        block.setText(StrUtil.isBlank(segmentText) ? operationName : segmentText + " " + operationName);
        block.setColorType(statusColorType(factStatusClass(fact, plannedOperation)));
        return block;
    }

    private List<HcScheduleWorkbenchRespVO.Fact> sortedFacts(HcScheduleWorkbenchRespVO.Cell cell) {
        return emptyListIfNull(cell.getFacts()).stream()
                .sorted(Comparator.comparing(HcScheduleWorkbenchRespVO.Fact::getLastReportTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(fact -> defaultText(fact.getOperationName())))
                .toList();
    }

    private List<HcScheduleWorkbenchRespVO.PlannedOperation> plannedOperationDetails(
            HcScheduleWorkbenchRespVO.Cell cell) {
        if (cell.getPlannedOperationDetails() != null && !cell.getPlannedOperationDetails().isEmpty()) {
            return cell.getPlannedOperationDetails();
        }
        return emptyListIfNull(cell.getPlannedOperations()).stream()
                .map(operationName -> {
                    HcScheduleWorkbenchRespVO.PlannedOperation operation =
                            new HcScheduleWorkbenchRespVO.PlannedOperation();
                    operation.setOperationName(operationName);
                    operation.setStatusCode("");
                    operation.setStatusLabel("计划工序");
                    return operation;
                })
                .toList();
    }

    private HcScheduleWorkbenchRespVO.PlannedOperation findPlannedOperation(
            List<HcScheduleWorkbenchRespVO.PlannedOperation> plannedOperations, String operationName) {
        return plannedOperations.stream()
                .filter(item -> isSameOperationName(item.getOperationName(), operationName))
                .findFirst()
                .orElse(null);
    }

    private boolean isSimpleModeOperation(String operationName) {
        return SIMPLE_MODE_OPERATION_NAMES.contains(defaultText(operationName));
    }

    private boolean isSameOperationName(String left, String right) {
        String leftName = defaultText(left).trim();
        String rightName = defaultText(right).trim();
        if (StrUtil.isBlank(leftName) || StrUtil.isBlank(rightName)) {
            return false;
        }
        if (leftName.equals(rightName)) {
            return true;
        }
        return "磨皮".equals(leftName) && ("磨皮1".equals(rightName) || "磨皮2".equals(rightName));
    }

    private String factStatusClass(HcScheduleWorkbenchRespVO.Fact fact,
            HcScheduleWorkbenchRespVO.PlannedOperation plannedOperation) {
        String plannedStatusClass = plannedOperation == null ? "is-status-planned"
                : statusClassFromLabel(plannedOperation.getStatusLabel(), plannedOperation.getStatusCode());
        if ("is-status-maintenance".equals(plannedStatusClass) || "is-status-paused".equals(plannedStatusClass)) {
            return plannedStatusClass;
        }
        return isFactCompleted(fact) ? "is-status-completed" : "is-status-processing";
    }

    private boolean isFactCompleted(HcScheduleWorkbenchRespVO.Fact fact) {
        return Boolean.TRUE.equals(fact.getCompletedFlag());
    }

    private String statusClassFromLabel(String statusLabel, String statusCode) {
        String text = (defaultText(statusCode) + " " + defaultText(statusLabel)).toUpperCase(Locale.ROOT);
        if (text.matches(".*(MAINT|REPAIR|维修|保养).*")) {
            return "is-status-maintenance";
        }
        if (text.matches(".*(PAUSE|SUSPEND|暂停).*")) {
            return "is-status-paused";
        }
        if (text.matches(".*(FINISH|COMPLETE|DONE|已完成).*")) {
            return "is-status-completed";
        }
        if (text.matches(".*(RUNNING|PROCESS|加工中|执行中).*")) {
            return "is-status-processing";
        }
        return "is-status-planned";
    }

    private String statusColorType(String statusClass) {
        return switch (statusClass) {
            case "is-status-completed" -> "COMPLETED";
            case "is-status-maintenance" -> "MAINTENANCE";
            case "is-status-paused" -> "PAUSED";
            case "is-status-processing" -> "PROCESSING";
            default -> "PLANNED";
        };
    }

    private String segmentMark(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return "";
        }
        var standardMatcher = SEGMENT_STANDARD_PATTERN.matcher(text);
        if (standardMatcher.find()) {
            return standardMatcher.group(1).toUpperCase(Locale.ROOT);
        }
        var endMatcher = SEGMENT_END_PATTERN.matcher(text);
        return endMatcher.find() ? endMatcher.group(1).toUpperCase(Locale.ROOT) : "";
    }

    private <T> List<T> emptyListIfNull(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String exportDateLabel(String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        String week = switch (parsedDate.getDayOfWeek()) {
            case MONDAY -> "一";
            case TUESDAY -> "二";
            case WEDNESDAY -> "三";
            case THURSDAY -> "四";
            case FRIDAY -> "五";
            case SATURDAY -> "六";
            case SUNDAY -> "日";
        };
        return String.format(Locale.ROOT, "%02d-%02d %s", parsedDate.getMonthValue(), parsedDate.getDayOfMonth(), week);
    }

    private String rowMotherBatchNo(HcScheduleWorkbenchRespVO.Row row) {
        if (isDiscretePostPlan(row)) {
            String inventoryText = normalizeDiscreteBatchLines(row.getInventorySourceBatchNos());
            if (StrUtil.isNotBlank(inventoryText)) {
                return inventoryText;
            }
        }
        return firstNonBlank(row.getInventorySourceBatchNos(), row.getMotherRollNo(), row.getProductionBatchNo(),
                row.getBatchNo(), row.getMotherBatchPreviewNo());
    }

    private String prodTypeLabel(HcScheduleWorkbenchRespVO.Row row) {
        if (isDiscretePostPlan(row)) {
            return "试验加工";
        }
        return prodTypeLabel(row == null ? null : row.getProdType());
    }

    private String prodTypeLabel(String prodType) {
        if (PROD_TYPE_TRIAL_PROCESS.equalsIgnoreCase(defaultText(prodType))) {
            return "试验加工";
        }
        if ("MASS".equals(prodType)) {
            return "量产计划";
        }
        if ("RND_TRIAL".equals(prodType)) {
            return "研发试制";
        }
        return defaultText(prodType);
    }

    private boolean isDiscretePostPlan(HcScheduleWorkbenchMapper.PlanRow row) {
        return row != null && (PLAN_MODE_DISCRETE_POST.equalsIgnoreCase(defaultText(row.planMode()))
                || SOURCE_TYPE_DISCRETE_NG.equalsIgnoreCase(defaultText(row.sourceType()))
                || SOURCE_TYPE_DISCRETE_WIP.equalsIgnoreCase(defaultText(row.sourceType()))
                || StrUtil.containsIgnoreCase(row.requirement(), LOCK_MARK_DISCRETE_POST));
    }

    private boolean isDiscretePostPlan(HcScheduleWorkbenchRespVO.Row row) {
        return row != null && (PLAN_MODE_DISCRETE_POST.equalsIgnoreCase(defaultText(row.getPlanMode()))
                || SOURCE_TYPE_DISCRETE_NG.equalsIgnoreCase(defaultText(row.getSourceType()))
                || SOURCE_TYPE_DISCRETE_WIP.equalsIgnoreCase(defaultText(row.getSourceType()))
                || StrUtil.containsIgnoreCase(row.getRequirement(), LOCK_MARK_DISCRETE_POST));
    }

    private String cleanRequirement(HcScheduleWorkbenchMapper.PlanRow row) {
        String text = defaultText(row == null ? null : row.requirement()).trim();
        if (!isDiscretePostPlan(row)) {
            return text;
        }
        if ("null".equalsIgnoreCase(text)) {
            return "";
        }
        text = text.replaceFirst("(?i)^null\\s*[;；]?\\s*", "").trim();
        int markerIndex = text.toUpperCase(Locale.ROOT).indexOf(LOCK_MARK_DISCRETE_POST);
        if (markerIndex < 0) {
            return text;
        }
        return text.substring(0, markerIndex).replaceAll("[;；\\s]+$", "");
    }

    private String normalizeDiscreteBatchLines(String value) {
        return List.of(defaultText(value).split("[、,，;；\\s]+")).stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    private String dateText(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String quantityText(BigDecimal value, String unit) {
        if (value == null || BigDecimal.ZERO.compareTo(value) == 0) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString() + StrUtil.nullToDefault(unit, "");
    }

    private String joinNonBlank(String... values) {
        return List.of(values).stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("\n"));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String defaultText(String value) {
        return StrUtil.blankToDefault(value, "");
    }

    private boolean isHiddenCardOperation(String operationName) {
        String name = String.valueOf(operationName == null ? "" : operationName).trim();
        return name.contains("入库") || name.contains("出库");
    }

    private void normalizeReq(HcScheduleWorkbenchReqVO reqVO) {
        LocalDate today = LocalDate.now();
        if (reqVO.getStartDate() == null) {
            reqVO.setStartDate(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        }
        if (reqVO.getEndDate() == null) {
            reqVO.setEndDate(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
        }
        if (reqVO.getEndDate().isBefore(reqVO.getStartDate())) {
            LocalDate startDate = reqVO.getStartDate();
            reqVO.setStartDate(reqVO.getEndDate());
            reqVO.setEndDate(startDate);
        }
        if (ChronoUnit.DAYS.between(reqVO.getStartDate(), reqVO.getEndDate()) >= MAX_DATE_COLUMN_COUNT) {
            reqVO.setStartDate(reqVO.getEndDate().minusDays(MAX_DATE_COLUMN_COUNT - 1L));
        }
        reqVO.setKeyword(trimToNull(reqVO.getKeyword()));
        reqVO.setModelCode(trimToNull(reqVO.getModelCode()));
        reqVO.setSizeSpec(trimToNull(reqVO.getSizeSpec()));
        reqVO.setOperationName(trimToNull(reqVO.getOperationName()));
    }

    private List<String> buildDateColumns(LocalDate startDate, LocalDate endDate) {
        List<String> dates = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            dates.add(cursor.toString());
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private Map<Long, HcScheduleWorkbenchRespVO.Row> buildRowMap(
            List<HcScheduleWorkbenchMapper.PlanRow> planRows, List<String> dateColumns) {
        Map<Long, HcScheduleWorkbenchRespVO.Row> rowMap = new LinkedHashMap<>();
        for (HcScheduleWorkbenchMapper.PlanRow row : planRows) {
            HcScheduleWorkbenchRespVO.Row respRow = new HcScheduleWorkbenchRespVO.Row();
            respRow.setPlanId(row.planId());
            respRow.setPlanNo(row.planNo());
            respRow.setPlanDate(row.planDate());
            respRow.setPlanMode(row.planMode());
            respRow.setSourceType(row.sourceType());
            respRow.setProdType(row.prodType());
            respRow.setPlanStatus(row.planStatus());
            respRow.setModelCode(row.modelCode());
            respRow.setModelChangeoverSummaries(new ArrayList<>());
            respRow.setMotherRollNo(row.motherRollNo());
            respRow.setInventorySourceBatchNos(row.inventorySourceBatchNos());
            respRow.setRequirement(cleanRequirement(row));
            respRow.setSizeSpec(row.sizeSpec());
            respRow.setSizeChangeoverSummaries(new ArrayList<>());
            respRow.setQuantity(BigDecimal.ZERO);
            respRow.setQuantityUnit("片");
            respRow.setQuantityOperationName("");
            respRow.setWetRollQty(BigDecimal.ZERO);
            respRow.setSecondGrindingQty(BigDecimal.ZERO);
            respRow.setSlittingConfirmedQty(BigDecimal.ZERO);
            respRow.setAdhesive2Qty(BigDecimal.ZERO);
            respRow.setAdhesive2ChangeoverFlag(false);
            respRow.setPickedQty(BigDecimal.ZERO);
            respRow.setDueDate(row.dueDate());
            respRow.setBatchNo(row.batchNo());
            respRow.setProductionBatchNo(row.productionBatchNo());
            respRow.setSplitPlanNos(row.splitPlanNos());
            respRow.setFrontProcessEnabled(Boolean.TRUE.equals(row.frontProcessFlag()));
            respRow.setPostProcessEnabled(Boolean.TRUE.equals(row.postProcessFlag()));
            respRow.setCompletionStatus("0/0");
            respRow.setCells(buildEmptyCells(dateColumns));
            rowMap.put(row.planId(), respRow);
        }
        return rowMap;
    }

    private Map<String, HcScheduleWorkbenchRespVO.Cell> buildEmptyCells(List<String> dateColumns) {
        Map<String, HcScheduleWorkbenchRespVO.Cell> cells = new LinkedHashMap<>();
        for (String date : dateColumns) {
            HcScheduleWorkbenchRespVO.Cell cell = new HcScheduleWorkbenchRespVO.Cell();
            cell.setDate(date);
            cell.setPlannedOperations(new ArrayList<>());
            cell.setPlannedOperationDetails(new ArrayList<>());
            cell.setFacts(new ArrayList<>());
            cell.setNgQty(0);
            cell.setStatusLabel("");
            cells.put(date, cell);
        }
        return cells;
    }

    private void fillProgressQuantities(Map<Long, HcScheduleWorkbenchRespVO.Row> rowMap,
                                        List<HcScheduleWorkbenchMapper.ProgressQuantityRow> quantityRows) {
        for (HcScheduleWorkbenchMapper.ProgressQuantityRow quantityRow : quantityRows) {
            HcScheduleWorkbenchRespVO.Row row = rowMap.get(quantityRow.planId());
            if (row == null) {
                continue;
            }
            row.setWetRollQty(defaultDecimal(quantityRow.wetRollQty()));
            row.setSecondGrindingQty(defaultDecimal(quantityRow.secondGrindingQty()));
            row.setSlittingConfirmedQty(defaultDecimal(quantityRow.slittingConfirmedQty()));
            row.setAdhesive2Qty(defaultDecimal(quantityRow.adhesive2Qty()));
            row.setAdhesive2ChangeoverFlag(defaultInt(quantityRow.adhesive2ChangeoverCount()) > 0);
            row.setPickedQty(defaultDecimal(quantityRow.pickedQty()));
        }
    }

    private void fillChangeoverSummaries(
            Map<Long, HcScheduleWorkbenchRespVO.Row> rowMap,
            List<HcScheduleWorkbenchMapper.ModelChangeoverSummaryRow> modelRows,
            List<HcScheduleWorkbenchMapper.CutRoundSizeSourceRow> sizeRows) {
        for (HcScheduleWorkbenchMapper.ModelChangeoverSummaryRow modelRow : emptyListIfNull(modelRows)) {
            HcScheduleWorkbenchRespVO.Row row = rowMap.get(modelRow.planId());
            if (row == null || StrUtil.isBlank(modelRow.actualValue())) {
                continue;
            }
            HcScheduleWorkbenchRespVO.ChangeoverSummary summary = new HcScheduleWorkbenchRespVO.ChangeoverSummary();
            summary.setType("MODEL");
            summary.setSourceOperation("粘胶2");
            summary.setPlanValue(firstNonBlank(modelRow.planValue(), row.getModelCode()));
            summary.setActualValue(modelRow.actualValue());
            summary.setQty(defaultInt(modelRow.qty()));
            summary.setUnit("片");
            row.getModelChangeoverSummaries().add(summary);
            row.setAdhesive2ChangeoverFlag(true);
        }

        Map<String, HcScheduleWorkbenchRespVO.ChangeoverSummary> sizeSummaryMap = new LinkedHashMap<>();
        for (HcScheduleWorkbenchMapper.CutRoundSizeSourceRow sizeRow : emptyListIfNull(sizeRows)) {
            HcScheduleWorkbenchRespVO.Row row = rowMap.get(sizeRow.planId());
            if (row == null) {
                continue;
            }
            String planValue = normalizeCutSizeMm(firstNonBlank(sizeRow.planValue(), row.getSizeSpec()));
            String actualValue = resolveCutSizeMm(sizeRow.extraJson(), sizeRow.productionBatchNo());
            if (StrUtil.isBlank(planValue) || StrUtil.isBlank(actualValue) || Objects.equals(planValue, actualValue)) {
                continue;
            }
            String key = sizeRow.planId() + "|" + actualValue;
            HcScheduleWorkbenchRespVO.ChangeoverSummary summary = sizeSummaryMap.get(key);
            if (summary == null) {
                summary = new HcScheduleWorkbenchRespVO.ChangeoverSummary();
                summary.setType("SIZE");
                summary.setSourceOperation("裁切");
                summary.setPlanValue(planValue);
                summary.setActualValue(actualValue);
                summary.setActualSuffix(resolveCutSizeSuffix(actualValue));
                summary.setQty(0);
                summary.setUnit("片");
                sizeSummaryMap.put(key, summary);
                row.getSizeChangeoverSummaries().add(summary);
            }
            summary.setQty(defaultInt(summary.getQty()) + 1);
        }
    }

    private HcScheduleWorkbenchRespVO.ChangeoverDetail buildModelChangeoverDetail(
            HcScheduleWorkbenchMapper.ChangeoverDetailRow row) {
        String planValue = StrUtil.trimToEmpty(row.planValue());
        String actualValue = StrUtil.trimToEmpty(row.actualValue());
        if (StrUtil.isBlank(actualValue) || Objects.equals(planValue, actualValue)) {
            return null;
        }
        HcScheduleWorkbenchRespVO.ChangeoverDetail detail = buildBaseChangeoverDetail(row);
        detail.setPlanValue(planValue);
        detail.setActualValue(actualValue);
        detail.setActualSuffix("");
        return detail;
    }

    private HcScheduleWorkbenchRespVO.ChangeoverDetail buildSizeChangeoverDetail(
            HcScheduleWorkbenchMapper.ChangeoverDetailRow row) {
        String planValue = normalizeCutSizeMm(row.planValue());
        String actualValue = resolveCutSizeMm(row.extraJson(), row.productionBatchNo());
        if (StrUtil.isBlank(planValue) || StrUtil.isBlank(actualValue) || Objects.equals(planValue, actualValue)) {
            return null;
        }
        HcScheduleWorkbenchRespVO.ChangeoverDetail detail = buildBaseChangeoverDetail(row);
        detail.setPlanValue(planValue);
        detail.setActualValue(actualValue);
        detail.setActualSuffix(resolveCutSizeSuffix(actualValue));
        return detail;
    }

    private HcScheduleWorkbenchRespVO.ChangeoverDetail buildBaseChangeoverDetail(
            HcScheduleWorkbenchMapper.ChangeoverDetailRow row) {
        HcScheduleWorkbenchRespVO.ChangeoverDetail detail = new HcScheduleWorkbenchRespVO.ChangeoverDetail();
        detail.setPlanId(row.planId());
        detail.setSourceReportId(row.sourceReportId());
        detail.setSourceOperation(row.sourceOperation());
        detail.setProductionBatchNo(row.productionBatchNo());
        detail.setGlueBoardModel(row.glueBoardModel());
        detail.setReporterName(row.reporterName());
        detail.setReportTime(row.reportTime());
        detail.setReportStatus(row.reportStatus());
        return detail;
    }

    private String resolveCutSizeMm(String extraJson, String productionBatchNo) {
        Map<String, Object> extra = parseExtra(extraJson);
        return normalizeCutSizeMm(firstNonBlank(
                extraString(extra, "actualSizeRule"),
                extraString(extra, "actualSize"),
                extraString(extra, "actualSizeSuffix"),
                productionBatchNo));
    }

    private String normalizeCutSizeMm(String value) {
        String normalized = normalizeCutRoundActualSizeRule(value);
        if (StrUtil.isNotBlank(normalized)) {
            return normalized.replace("mm", "");
        }
        String text = StrUtil.trimToEmpty(value)
                .replaceAll("(?i)-J\\d+$", "")
                .replaceAll("(?i)-S\\d+$", "")
                .toUpperCase(Locale.ROOT);
        if (text.matches(".*[PQRS]\\d{3,}A$")) {
            return "775";
        }
        if (text.matches(".*[PQRS]\\d{3,}B$")) {
            return "740";
        }
        return text.replace("MM", "");
    }

    private String normalizeCutRoundActualSizeRule(String value) {
        String text = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        if (text.contains("740") || "B".equals(text)) {
            return "740mm";
        }
        if (text.contains("775") || "A".equals(text)) {
            return "775mm";
        }
        return "";
    }

    private String resolveCutSizeSuffix(String sizeMm) {
        if ("740".equals(normalizeCutSizeMm(sizeMm))) {
            return "B";
        }
        if ("775".equals(normalizeCutSizeMm(sizeMm))) {
            return "A";
        }
        return "";
    }

    private Map<String, Object> parseExtra(String extraJson) {
        if (StrUtil.isBlank(extraJson)) {
            return Map.of();
        }
        try {
            Map<String, Object> map = JsonUtils.parseObject(extraJson, new TypeReference<>() {
            });
            return map == null ? Map.of() : map;
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private String extraString(Map<String, Object> extra, String key) {
        Object value = extra == null ? null : extra.get(key);
        return value == null ? "" : StrUtil.trimToEmpty(String.valueOf(value));
    }

    private void fillScheduledOperations(Map<Long, HcScheduleWorkbenchRespVO.Row> rowMap,
                                         List<HcScheduleWorkbenchMapper.ScheduleOperationRow> operationRows) {
        for (HcScheduleWorkbenchMapper.ScheduleOperationRow operation : operationRows) {
            HcScheduleWorkbenchRespVO.Row row = rowMap.get(operation.planId());
            if (row == null || operation.scheduleDate() == null) {
                continue;
            }
            String date = operation.scheduleDate().toString();
            HcScheduleWorkbenchRespVO.Cell cell = row.getCells().get(date);
            if (cell == null || StrUtil.isBlank(operation.operationName())) {
                continue;
            }
            cell.getPlannedOperations().add(operation.operationName());
            HcScheduleWorkbenchRespVO.PlannedOperation plannedOperation =
                    new HcScheduleWorkbenchRespVO.PlannedOperation();
            plannedOperation.setPlanOperationId(operation.planOperationId());
            plannedOperation.setOperationName(operation.operationName());
            plannedOperation.setStatusCode(operation.operationStatus());
            plannedOperation.setStatusLabel(resolveOperationStatusLabel(operation.operationStatus()));
            cell.getPlannedOperationDetails().add(plannedOperation);
            if (isFrontProcessOperation(operation.operationName(), null)) {
                row.setFrontProcessEnabled(true);
            } else {
                row.setPostProcessEnabled(true);
            }
            cell.setStatusLabel(resolveOperationStatusLabel(operation.operationStatus()));
        }
    }

    private void fillFacts(Map<Long, HcScheduleWorkbenchRespVO.Row> rowMap,
                           List<HcScheduleWorkbenchMapper.FactRow> factRows) {
        for (HcScheduleWorkbenchMapper.FactRow factRow : factRows) {
            HcScheduleWorkbenchRespVO.Row row = rowMap.get(factRow.planId());
            if (row == null || factRow.reportDate() == null) {
                continue;
            }
            HcScheduleWorkbenchRespVO.Cell cell = row.getCells().get(factRow.reportDate().toString());
            if (cell == null) {
                continue;
            }
            HcScheduleWorkbenchRespVO.Fact fact = new HcScheduleWorkbenchRespVO.Fact();
            fact.setOperationName(factRow.operationName());
            fact.setFactSource(factRow.factSource());
            fact.setSegmentBatchNo(factRow.segmentBatchNo());
            fact.setInputQty(defaultDecimal(factRow.inputQty()));
            fact.setInputUnit(factRow.inputUnit());
            fact.setReportQty(defaultDecimal(factRow.reportQty()));
            fact.setReportUnit(factRow.reportUnit());
            fact.setPendingQty(defaultDecimal(factRow.pendingQty()));
            fact.setPendingUnit(factRow.pendingUnit());
            fact.setLossNgQty(defaultDecimal(factRow.lossNgQty()));
            fact.setLossNgUnit(factRow.lossNgUnit());
            fact.setRecordCount(defaultInt(factRow.recordCount()));
            fact.setNgQty(defaultInt(factRow.ngQty()));
            fact.setCoaFlag(Boolean.TRUE.equals(factRow.coaFlag()));
            fact.setCoaNgFlag(Boolean.TRUE.equals(factRow.coaNgFlag()));
            fact.setCompletedFlag(Boolean.TRUE.equals(factRow.completedFlag()));
            fact.setEquipmentNames(factRow.equipmentNames());
            fact.setLastReportTime(factRow.lastReportTime());
            fact.setGlueBoardModel(factRow.glueBoardModel());
            fact.setFirstInspectionResult(factRow.firstInspectionResult());
            cell.getFacts().add(fact);
            cell.setNgQty(defaultInt(cell.getNgQty()) + defaultInt(factRow.ngQty()));
            if (cell.getStatusLabel() == null || cell.getStatusLabel().isBlank()) {
                cell.setStatusLabel(defaultInt(factRow.ngQty()) > 0 ? "有NG" : "有报工");
            }
        }
    }

    private void fillOperationCompletion(Map<Long, HcScheduleWorkbenchRespVO.Row> rowMap,
                                         List<HcScheduleWorkbenchMapper.PlanOperationScopeRow> operationRows,
                                         List<HcScheduleWorkbenchMapper.OperationCompletionRow> completionRows,
                                         List<HcScheduleWorkbenchMapper.SegmentProgressRow> segmentRows) {
        Map<Long, List<HcScheduleWorkbenchMapper.PlanOperationScopeRow>> operationMap = operationRows.stream()
                .collect(Collectors.groupingBy(HcScheduleWorkbenchMapper.PlanOperationScopeRow::planId));
        Map<Long, HcScheduleWorkbenchMapper.OperationCompletionRow> legacyCompletionMap = completionRows.stream()
                .filter(row -> row.planId() != null)
                .collect(Collectors.toMap(HcScheduleWorkbenchMapper.OperationCompletionRow::planId,
                        row -> row, (left, right) -> left));
        Map<Long, Map<String, SegmentProgress>> segmentMap = buildSegmentProgressMap(segmentRows);
        rowMap.values().forEach(row -> row.setCompletionStatus("0/0"));
        for (HcScheduleWorkbenchRespVO.Row row : rowMap.values()) {
            List<HcScheduleWorkbenchMapper.PlanOperationScopeRow> planOperations =
                    operationMap.getOrDefault(row.getPlanId(), List.of());
            Map<String, SegmentProgress> planSegments = segmentMap.getOrDefault(row.getPlanId(), Map.of());
            boolean hasSecondGrinding = hasSecondGrinding(planOperations, planSegments);
            if (!hasSecondGrinding) {
                HcScheduleWorkbenchMapper.OperationCompletionRow legacy = legacyCompletionMap.get(row.getPlanId());
                int total = Math.max(defaultInt(legacy == null ? null : legacy.totalCount()), 0);
                int finished = Math.max(defaultInt(legacy == null ? null : legacy.finishedCount()), 0);
                row.setCompletionStatus((total > 0 && finished >= total ? 1 : 0) + "/1");
                continue;
            }
            List<SegmentProgress> denominatorSegments = planSegments.values().stream()
                    .filter(segment -> shouldCountSegment(segment, true))
                    .toList();
            int totalCount = denominatorSegments.size();
            int finishedCount = (int) denominatorSegments.stream()
                    .filter(SegmentProgress::isCutSegmentFinished)
                    .count();
            row.setCompletionStatus(finishedCount + "/" + totalCount);
        }
    }

    private Map<Long, Map<String, SegmentProgress>> buildSegmentProgressMap(
            List<HcScheduleWorkbenchMapper.SegmentProgressRow> segmentRows) {
        Map<Long, Map<String, SegmentProgress>> progressMap = new HashMap<>();
        for (HcScheduleWorkbenchMapper.SegmentProgressRow row : segmentRows) {
            if (row.planId() == null) {
                continue;
            }
            String pieceKey = isPieceProgressSource(row.sourceType()) ? normalizePieceBatchNo(row.segmentBatchNo()) : null;
            String segmentKey = isPieceProgressSource(row.sourceType())
                    ? normalizeSegmentBatchNo(pieceKey)
                    : normalizeSegmentBatchNo(row.segmentBatchNo());
            if (StrUtil.isBlank(segmentKey)) {
                continue;
            }
            SegmentProgress progress = progressMap
                    .computeIfAbsent(row.planId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(segmentKey, key -> new SegmentProgress());
            progress.add(row.sourceType(), defaultInt(row.sourceCount()), pieceKey);
        }
        return progressMap;
    }

    private boolean isPieceProgressSource(String sourceType) {
        return "PIECE_DENOMINATOR".equals(sourceType);
    }

    private boolean hasSecondGrinding(List<HcScheduleWorkbenchMapper.PlanOperationScopeRow> operations,
                                      Map<String, SegmentProgress> segments) {
        if (operations.stream().anyMatch(row -> isSecondGrindingOperation(row.operationName(), row.operationCode()))) {
            return true;
        }
        return segments.values().stream().anyMatch(segment -> shouldCountSegment(segment, true));
    }

    private boolean shouldCountSegment(SegmentProgress segment, boolean pieceStage) {
        if (!pieceStage) {
            return segment.secondSegmentCount > 0;
        }
        if (segment.expectedPieceCount() > 0) {
            return true;
        }
        return segment.slittingConfirmedCount == 0 && segment.secondSegmentCount > 0;
    }

    private List<HcScheduleWorkbenchRespVO.Metric> buildMetrics(HcScheduleWorkbenchReqVO reqVO) {
        LocalDate today = LocalDate.now();
        return List.of(
                metric("scheduleTotal", "排程总数", scheduleWorkbenchMapper.selectScheduleTotal(reqVO), "单",
                        "当前日期范围内的排程/计划数量"),
                metric("todayStartedEquipment", "本日设备开工数",
                        scheduleWorkbenchMapper.selectTodayStartedEquipmentCount(today), "台",
                        "今日发生开工或当前生产中的去重设备数"),
                metric("weekWetQty", "本周前道报工量", scheduleWorkbenchMapper.selectWetReportQty(
                        reqVO.getStartDate(), reqVO.getEndDate()), "m", "湿法完成报工总量"),
                metric("weekSlittingQty", "本周分切数", scheduleWorkbenchMapper.selectSlittingConfirmedCount(
                        reqVO.getStartDate(), reqVO.getEndDate()), "片", "分切扫码确认片数"),
                metric("weekAdhesive2Pieces", "本周粘胶片数", scheduleWorkbenchMapper.selectAdhesive2PieceCount(
                        reqVO.getStartDate(), reqVO.getEndDate()), "片", "粘胶2确认报工片数"),
                metric("weekNgQty", "本周NG", scheduleWorkbenchMapper.selectNgCount(
                        reqVO.getStartDate(), reqVO.getEndDate()), "项", "各工序自检/缺陷/检验NG合计"),
                metric("weekShippedQty", "本周发货数量", scheduleWorkbenchMapper.selectShippedQty(
                        reqVO.getStartDate(), reqVO.getEndDate()), "片", "成品发货出库已发货数量")
        );
    }

    private List<HcScheduleWorkbenchRespVO.NgSummary> buildNgSummaries(LocalDate startDate, LocalDate endDate) {
        return scheduleWorkbenchMapper.selectNgSummaryRows(startDate, endDate).stream()
                .map(row -> {
                    HcScheduleWorkbenchRespVO.NgSummary summary = new HcScheduleWorkbenchRespVO.NgSummary();
                    summary.setOperationName(row.operationName());
                    summary.setNgQty(defaultInt(row.ngQty()));
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private List<HcScheduleWorkbenchRespVO.WetWaterChangeApply> buildWetWaterChangeApplies(
            LocalDate startDate, LocalDate endDate) {
        return wetWaterChangeApplyMapper.selectByDateRange(startDate, endDate).stream()
                .map(apply -> BeanUtil.copyProperties(apply, HcScheduleWorkbenchRespVO.WetWaterChangeApply.class))
                .collect(Collectors.toList());
    }

    private HcScheduleWorkbenchRespVO.Metric metric(String code, String label, BigDecimal value,
                                                    String unit, String description) {
        HcScheduleWorkbenchRespVO.Metric metric = new HcScheduleWorkbenchRespVO.Metric();
        metric.setCode(code);
        metric.setLabel(label);
        metric.setValue(defaultDecimal(value));
        metric.setUnit(unit);
        metric.setDescription(description);
        return metric;
    }

    private String resolveOperationStatusLabel(String operationStatus) {
        String status = StrUtil.blankToDefault(operationStatus, "").toUpperCase(Locale.ROOT);
        if ("FINISHED".equals(status) || "COMPLETED".equals(status) || "DONE".equals(status)) {
            return "已完成";
        }
        if ("RUNNING".equals(status) || "PROCESSING".equals(status)) {
            return "加工中";
        }
        if ("PAUSED".equals(status) || "SUSPENDED".equals(status)) {
            return "暂停";
        }
        if ("MAINTENANCE".equals(status) || "MAINTAINING".equals(status)
                || "REPAIR".equals(status) || "LINE_MAINTENANCE".equals(status)) {
            return "产线维修保养";
        }
        if ("CANCELLED".equals(status)) {
            return "取消";
        }
        return "待执行";
    }

    private boolean isFrontProcessOperation(String operationName, String operationCode) {
        int stage = resolveOperationStage(operationName, operationCode);
        return stage >= STAGE_FORMULA && stage <= STAGE_GRINDING_SECOND;
    }

    private boolean isSecondGrindingOperation(String operationName, String operationCode) {
        String text = normalizeOperationText(operationName, operationCode);
        return text.contains("磨皮2")
                || text.contains("二磨")
                || text.contains("二次磨皮")
                || text.contains("GRINDING2")
                || text.contains("GRINDING_SECOND")
                || text.contains("ROUGH_GRINDING_SECOND");
    }

    private int resolveOperationStage(String operationName, String operationCode) {
        String text = normalizeOperationText(operationName, operationCode);
        if (text.contains("包装出库") || text.contains("OUTBOUND")) {
            return STAGE_PACKAGE_OUT;
        }
        if (text.contains("包装") || text.contains("内包") || text.contains("PACKAGE")) {
            return STAGE_PACKAGE_IN;
        }
        if (text.contains("裁切") || text.contains("裁圆") || text.contains("CUT")) {
            return STAGE_CUT_ROUND;
        }
        if (text.contains("粘胶2") || text.contains("粘胶二") || text.contains("ADHESIVE2") || text.contains("ADH2")) {
            return STAGE_ADHESIVE2;
        }
        if (text.contains("压槽") || text.contains("PRESS")) {
            return STAGE_PRESS_SLOT;
        }
        if (text.contains("分切") || text.contains("SLIT")) {
            return STAGE_SLITTING;
        }
        if (text.contains("粘胶1") || text.contains("粘胶一") || text.contains("ADHESIVE1") || text.contains("ADH1")
                || text.contains("背胶")) {
            return STAGE_ADHESIVE1;
        }
        if (text.contains("磨皮") || text.contains("粗磨") || text.contains("GRINDING")) {
            return STAGE_GRINDING_SECOND;
        }
        if (text.contains("湿法") || text.contains("WET")) {
            return STAGE_WET;
        }
        if (text.contains("配料") || text.contains("配方") || text.contains("FORMULA")) {
            return STAGE_FORMULA;
        }
        return 0;
    }

    private String normalizeOperationText(String operationName, String operationCode) {
        return (StrUtil.blankToDefault(operationCode, "") + " " + StrUtil.blankToDefault(operationName, ""))
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeSegmentBatchNo(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String text = ADHESIVE_SUFFIX_PATTERN.matcher(value.trim()).replaceFirst("");
        var matcher = PIECE_SUFFIX_PATTERN.matcher(text);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return text;
    }

    private String normalizePieceBatchNo(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String text = ADHESIVE_SUFFIX_PATTERN.matcher(value.trim()).replaceFirst("")
                .toUpperCase(Locale.ROOT);
        var matcher = CUT_PIECE_SUFFIX_PATTERN.matcher(text);
        return matcher.matches() ? matcher.group(1) : text;
    }

    private static final class SegmentProgress {

        private int secondSegmentCount;
        private int slittingConfirmedCount;
        private int cutCompletedCount;
        private int splitOutPieceCount;
        private int splitInPressPieceCount;
        private int splitInAdhesive2PieceCount;
        private final Set<String> denominatorPieceKeys = new LinkedHashSet<>();

        private void add(String sourceType, int count, String pieceKey) {
            if (count <= 0 || sourceType == null) {
                return;
            }
            switch (sourceType) {
                case "PIECE_DENOMINATOR" -> addPiece(denominatorPieceKeys, pieceKey);
                case "SECOND_SEGMENT" -> secondSegmentCount += count;
                case "SLITTING_CONFIRMED" -> slittingConfirmedCount += count;
                case "CUT_SEGMENT_FINISHED" -> cutCompletedCount += count;
                case "SPLIT_OUT_PIECE" -> splitOutPieceCount += count;
                case "SPLIT_IN_PRESS_PIECE" -> splitInPressPieceCount += count;
                case "SPLIT_IN_ADHESIVE2_PIECE" -> splitInAdhesive2PieceCount += count;
                default -> {
                    // 新增证据类型时默认忽略，避免影响排程工作台加载。
                }
            }
        }

        private void addPiece(Set<String> pieceKeys, String pieceKey) {
            if (StrUtil.isNotBlank(pieceKey)) {
                pieceKeys.add(pieceKey);
            }
        }

        private int expectedPieceCount() {
            if (!denominatorPieceKeys.isEmpty()) {
                return Math.max(denominatorPieceKeys.size() + splitInPressPieceCount + splitInAdhesive2PieceCount
                        - splitOutPieceCount, 0);
            }
            return Math.max(slittingConfirmedCount + splitInPressPieceCount + splitInAdhesive2PieceCount
                    - splitOutPieceCount, 0);
        }

        private boolean isCutSegmentFinished() {
            return cutCompletedCount > 0;
        }

    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

}
