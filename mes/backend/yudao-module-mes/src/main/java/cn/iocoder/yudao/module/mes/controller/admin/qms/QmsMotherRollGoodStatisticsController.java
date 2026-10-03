package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsStageRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsMotherRollGoodStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - QMS Mother Roll Good Statistics")
@RestController
@RequestMapping("/mes/quality/statistics/mother-roll-good-statistics")
@Validated
public class QmsMotherRollGoodStatisticsController {

    private static final DateTimeFormatter PROCESS_PIVOT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> DEFAULT_PROCESS_PIVOT_BASE_COLUMNS = List.of(
            "motherRollBatchNo", "modelCode", "sizeSpec", "segmentBatchNo");
    private static final List<String> DEFAULT_PROCESS_PIVOT_STAGE_COLUMNS = List.of(
            "FORMULA.doneQty", "FORMULA.theoreticalOutputQty", "FORMULA.goodYieldRate",
            "FORMULA.inspectionQty", "FORMULA.inspectionNgQty",
            "WET.doneQty", "WET.theoreticalOutputQty", "WET.goodYieldRate",
            "WET.inspectionQty", "WET.inspectionNgQty",
            "GRINDING.inputQty", "GRINDING.doneQty", "GRINDING.motherOutputQty", "GRINDING.theoreticalOutputQty",
            "GRINDING.goodYieldRate", "GRINDING.inspectionQty", "GRINDING.inspectionNgQty",
            "ADHESIVE1.inputQty", "ADHESIVE1.doneQty", "ADHESIVE1.motherOutputQty", "ADHESIVE1.theoreticalOutputQty",
            "ADHESIVE1.goodYieldRate", "ADHESIVE1.inspectionQty", "ADHESIVE1.inspectionNgQty",
            "SLITTING.doneQty", "SLITTING.segmentDefectQty", "SLITTING.inspectionQty", "SLITTING.inspectionNgQty",
            "SLITTING.pendingQty", "SLITTING.inputQty", "SLITTING.motherOutputQty", "SLITTING.theoreticalOutputQty", "SLITTING.goodYieldRate",
            "PRESS_SLOT.doneQty", "PRESS_SLOT.segmentDefectQty", "PRESS_SLOT.inspectionQty",
            "PRESS_SLOT.processProductionInspectionQty", "PRESS_SLOT.inspectionNgQty",
            "PRESS_SLOT.coaInspectionQty", "PRESS_SLOT.coaInspectionNgQty",
            "PRESS_SLOT.pendingQty", "PRESS_SLOT.inputQty", "PRESS_SLOT.motherOutputQty", "PRESS_SLOT.theoreticalOutputQty", "PRESS_SLOT.goodYieldRate",
            "ADHESIVE2.doneQty", "ADHESIVE2.segmentDefectQty", "ADHESIVE2.inspectionQty",
            "ADHESIVE2.processProductionInspectionQty", "ADHESIVE2.inspectionNgQty",
            "ADHESIVE2.coaInspectionQty", "ADHESIVE2.coaInspectionNgQty",
            "ADHESIVE2.glueBoardInspectionQty", "ADHESIVE2.pendingQty", "ADHESIVE2.inputQty", "ADHESIVE2.motherOutputQty",
            "ADHESIVE2.theoreticalOutputQty", "ADHESIVE2.goodYieldRate",
            "CUT_ROUND.doneQty", "CUT_ROUND.segmentDefectQty", "CUT_ROUND.inspectionQty",
            "CUT_ROUND.processProductionInspectionQty", "CUT_ROUND.inspectionNgQty",
            "CUT_ROUND.coaInspectionQty", "CUT_ROUND.coaInspectionNgQty",
            "CUT_ROUND.pendingQty", "CUT_ROUND.inputQty", "CUT_ROUND.motherOutputQty", "CUT_ROUND.theoreticalOutputQty",
            "CUT_ROUND.goodYieldRate", "CUT_ROUND.finalInspectionOutputQty", "CUT_ROUND.finalInspectionYieldRate",
            "CUT_ROUND.goodTargetRate",
            "SHIPPING_INSPECTION.inspectionQty", "SHIPPING_INSPECTION.inspectionNgQty");
    private static final List<String> PROCESS_PIVOT_BASE_COLUMN_ORDER = List.of(
            "motherRollBatchNo", "modelCode", "materialCode", "sizeSpec", "segmentBatchNo");
    private static final List<String> PROCESS_PIVOT_STAGE_ORDER = List.of(
            "FORMULA", "WET", "GRINDING", "ADHESIVE1", "SLITTING", "PRESS_SLOT",
            "ADHESIVE2", "CUT_ROUND", "SHIPPING_INSPECTION");
    private static final List<String> METER_REPORT_STAGE_CODES = List.of("GRINDING", "ADHESIVE1");
    private static final List<String> PIECE_SELF_CHECK_STAGE_CODES = List.of("SLITTING", "PRESS_SLOT",
            "ADHESIVE2", "CUT_ROUND");
    private static final List<String> PIECE_INPUT_STAGE_CODES = List.of("SLITTING", "PRESS_SLOT",
            "ADHESIVE2", "CUT_ROUND");
    private static final List<String> COA_INSPECTION_STAGE_CODES = List.of("PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final List<String> PROCESS_PIVOT_STAGE_SUB_COLUMN_ORDER = List.of(
            "batch", "startPosition", "processLength", "doneQty", "segmentDefectQty",
            "inspectionQty", "processProductionInspectionQty", "inspectionNgQty", "coaInspectionQty", "coaInspectionNgQty",
            "glueBoardInspectionQty", "pendingQty", "defectQty",
            "segmentInputQty", "inputQty", "motherOutputQty", "theoreticalOutputQty",
            "goodYieldRate", "finalInspectionOutputQty", "finalInspectionYieldRate", "goodTargetRate", "lastReportTime");
    private static final Map<String, String> PROCESS_PIVOT_BASE_LABELS = Map.of(
            "planNo", "计划号",
            "motherRollBatchNo", "母批批号",
            "planStatus", "状态",
            "modelCode", "型号",
            "materialCode", "料号",
            "sizeSpec", "尺寸",
            "segmentBatchNo", "磨皮分段片号");
    private static final Map<String, String> PROCESS_PIVOT_STAGE_LABELS = Map.of(
            "FORMULA", "配料",
            "WET", "湿法",
            "GRINDING", "磨皮",
            "ADHESIVE1", "粘胶1",
            "SLITTING", "分切",
            "PRESS_SLOT", "压槽",
            "ADHESIVE2", "粘胶2",
            "CUT_ROUND", "裁切",
            "SHIPPING_INSPECTION", "发货检验");
    private static final Map<String, String> PROCESS_PIVOT_STAGE_UNITS = Map.of(
            "FORMULA", "kg",
            "WET", "m",
            "GRINDING", "m",
            "ADHESIVE1", "m",
            "SLITTING", "片",
            "PRESS_SLOT", "片",
            "ADHESIVE2", "片",
            "CUT_ROUND", "片",
            "SHIPPING_INSPECTION", "片");
    private static final Map<String, String> PROCESS_PIVOT_STAGE_BATCH_LABELS = Map.of(
            "FORMULA", "母批批号",
            "WET", "母批批号",
            "GRINDING", "分段批号",
            "ADHESIVE1", "分段批号",
            "SLITTING", "片号",
            "PRESS_SLOT", "片号",
            "ADHESIVE2", "片号",
            "CUT_ROUND", "片号",
            "SHIPPING_INSPECTION", "片号");
    private static final Map<String, String> PROCESS_PIVOT_STAGE_SUB_LABELS = Map.ofEntries(
            Map.entry("batch", "批号"),
            Map.entry("startPosition", "起位置"),
            Map.entry("processLength", "长度"),
            Map.entry("segmentInputQty", "分段投入"),
            Map.entry("inputQty", "投入米数"),
            Map.entry("doneQty", "完工"),
            Map.entry("motherOutputQty", "母卷产出"),
            Map.entry("finalInspectionOutputQty", "终检产出"),
            Map.entry("finalInspectionYieldRate", "终检良率（%）"),
            Map.entry("theoreticalOutputQty", "理论产量"),
            Map.entry("goodYieldRate", "良品率（%）"),
            Map.entry("goodTargetRate", "母卷良品达标率（%）"),
            Map.entry("glueBoardInspectionQty", "胶板送检(m)"),
            Map.entry("inspectionQty", "送检"),
            Map.entry("processProductionInspectionQty", "本工序损耗"),
            Map.entry("inspectionNgQty", "检验NG"),
            Map.entry("coaInspectionQty", "COA送检"),
            Map.entry("coaInspectionNgQty", "COA NG"),
            Map.entry("pendingQty", "未加工"),
            Map.entry("segmentDefectQty", "分段自检NG"),
            Map.entry("defectQty", "NG"),
            Map.entry("lastReportTime", "时间"));


    @Resource
    private QmsMotherRollGoodStatisticsService qmsMotherRollGoodStatisticsService;

    @GetMapping("/page")
    @Operation(summary = "Page mother roll good statistics ledger")
    public CommonResult<PageResult<QmsMotherRollGoodStatisticsRespVO>> getMotherRollGoodStatisticsPage(
            @Valid QmsMotherRollGoodStatisticsPageReqVO pageReqVO) {
        return success(qmsMotherRollGoodStatisticsService.getMotherRollGoodStatisticsPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export mother roll good statistics ledger")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMotherRollGoodStatisticsExcel(@Valid QmsMotherRollGoodStatisticsPageReqVO pageReqVO,
            HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsMotherRollGoodStatisticsRespVO> list = qmsMotherRollGoodStatisticsService.getMotherRollGoodStatisticsList(pageReqVO);
        List<String> baseColumns = processPivotBaseColumns(pageReqVO);
        List<String> stageColumns = processPivotStageColumns(pageReqVO);
        List<List<String>> head = buildProcessPivotExportHead(pageReqVO);
        List<List<String>> exportRows = list.stream()
                .map(row -> buildProcessPivotExportRow(row, pageReqVO))
                .toList();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("母卷批次良品统计.xlsx"));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        FastExcelFactory.write(response.getOutputStream())
                .autoCloseStream(false)
                .head(head)
                .automaticMergeHead(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerWriteHandler(new QmsMotherRollGoodStatisticsExcelStyleHandler(head,
                        baseColumns.size(), exportRows.size(),
                        buildProcessPivotExportMergeRanges(list),
                        buildProcessPivotExportCellMergeRanges(list, baseColumns.size(), stageColumns)))
                .registerConverter(new LongStringConverter())
                .sheet("母卷批次良品统计")
                .doWrite(exportRows);
    }


    private List<List<String>> buildProcessPivotExportHead(QmsMotherRollGoodStatisticsPageReqVO reqVO) {
        List<List<String>> head = new ArrayList<>();
        for (String column : processPivotBaseColumns(reqVO)) {
            String label = PROCESS_PIVOT_BASE_LABELS.get(column);
            head.add(List.of(label, label));
        }
        for (String column : processPivotStageColumns(reqVO)) {
            String stageCode = stageCode(column);
            String subColumn = stageSubColumn(column);
            String stageLabel = processPivotStageHeaderLabel(stageCode);
            String subLabel = processPivotStageSubLabel(stageCode, subColumn);
            head.add(List.of(stageLabel, subLabel));
        }
        return head;
    }

    private String processPivotStageHeaderLabel(String stageCode) {
        String stageLabel = PROCESS_PIVOT_STAGE_LABELS.get(stageCode);
        String unit = PROCESS_PIVOT_STAGE_UNITS.get(stageCode);
        return StrUtil.isBlank(unit) ? stageLabel : stageLabel + "(" + unit + ")";
    }

    private String processPivotStageSubLabel(String stageCode, String subColumn) {
        if ("batch".equals(subColumn)) {
            return PROCESS_PIVOT_STAGE_BATCH_LABELS.getOrDefault(stageCode, "批号");
        }
        return switch (subColumn) {
            case "doneQty" -> stageDoneColumnLabel(stageCode);
            case "inputQty" -> stageInputColumnLabel(stageCode);
            case "motherOutputQty" -> stageMotherOutputColumnLabel(stageCode);
            case "inspectionQty" -> stageInspectionColumnLabel(stageCode);
            case "inspectionNgQty" -> stageInspectionNgColumnLabel(stageCode);
            case "defectQty" -> stageDefectColumnLabel(stageCode);
            case "lastReportTime" -> stageLastReportTimeLabel(stageCode);
            default -> PROCESS_PIVOT_STAGE_SUB_LABELS.getOrDefault(subColumn, subColumn);
        };
    }

    private List<String> buildProcessPivotExportRow(QmsMotherRollGoodStatisticsRespVO row,
            QmsMotherRollGoodStatisticsPageReqVO reqVO) {
        List<String> values = new ArrayList<>();
        for (String column : processPivotBaseColumns(reqVO)) {
            values.add(processPivotBaseValue(row, column));
        }
        for (String column : processPivotStageColumns(reqVO)) {
            String stageCode = stageCode(column);
            String subColumn = stageSubColumn(column);
            values.add(processPivotStageValue(stage(row.getStages(), stageCode), stageCode, subColumn));
        }
        return values;
    }

    private List<QmsMotherRollGoodStatisticsExcelStyleHandler.RowMergeRange> buildProcessPivotExportMergeRanges(
            List<QmsMotherRollGoodStatisticsRespVO> rows) {
        List<QmsMotherRollGoodStatisticsExcelStyleHandler.RowMergeRange> ranges = new ArrayList<>();
        int start = 0;
        while (start < rows.size()) {
            String mergeKey = processPivotPlanMergeKey(rows.get(start), start);
            int end = start;
            while (end + 1 < rows.size()
                    && mergeKey.equals(processPivotPlanMergeKey(rows.get(end + 1), end + 1))) {
                end++;
            }
            if (end > start) {
                ranges.add(new QmsMotherRollGoodStatisticsExcelStyleHandler.RowMergeRange(start, end));
            }
            start = end + 1;
        }
        return ranges;
    }

    private List<QmsMotherRollGoodStatisticsExcelStyleHandler.CellMergeRange> buildProcessPivotExportCellMergeRanges(
            List<QmsMotherRollGoodStatisticsRespVO> rows, int baseColumnCount, List<String> stageColumns) {
        List<QmsMotherRollGoodStatisticsExcelStyleHandler.CellMergeRange> ranges = new ArrayList<>();
        if (rows == null || rows.isEmpty() || stageColumns == null || stageColumns.isEmpty()) {
            return ranges;
        }
        for (int stageColumnIndex = 0; stageColumnIndex < stageColumns.size(); stageColumnIndex++) {
            String column = stageColumns.get(stageColumnIndex);
            int absoluteColumnIndex = baseColumnCount + stageColumnIndex;
            int start = 0;
            while (start < rows.size()) {
                String mergeKey = processPivotStageColumnMergeKey(rows.get(start), column, start);
                int end = start;
                while (end + 1 < rows.size()
                        && mergeKey.equals(processPivotStageColumnMergeKey(rows.get(end + 1), column, end + 1))) {
                    end++;
                }
                if (end > start) {
                    ranges.add(new QmsMotherRollGoodStatisticsExcelStyleHandler.CellMergeRange(
                            start, end, absoluteColumnIndex));
                }
                start = end + 1;
            }
        }
        return ranges;
    }

    private String processPivotPlanMergeKey(QmsMotherRollGoodStatisticsRespVO row, int rowIndex) {
        if (row.getId() != null) {
            return "ID:" + row.getId();
        }
        if (StrUtil.isNotBlank(row.getPlanNo())) {
            return "NO:" + row.getPlanNo();
        }
        return "ROW:" + rowIndex;
    }

    private String processPivotStageColumnMergeKey(QmsMotherRollGoodStatisticsRespVO row, String column, int rowIndex) {
        String stageCode = stageCode(column);
        String subColumn = stageSubColumn(column);
        QmsMotherRollGoodStatisticsStageRespVO stage = stage(row.getStages(), stageCode);
        if ("motherOutputQty".equals(subColumn) && stage != null && StrUtil.isNotBlank(stage.getMotherOutputMergeKey())) {
            return stage.getMotherOutputMergeKey();
        }
        if ("inputQty".equals(subColumn) && stage != null && StrUtil.isNotBlank(stage.getInputQtyMergeKey())) {
            return stage.getInputQtyMergeKey();
        }
        if ("finalInspectionOutputQty".equals(subColumn) && stage != null
                && StrUtil.isNotBlank(stage.getFinalInspectionOutputMergeKey())) {
            return stage.getFinalInspectionOutputMergeKey();
        }
        if ("finalInspectionYieldRate".equals(subColumn) && stage != null
                && StrUtil.isNotBlank(stage.getFinalInspectionYieldRateMergeKey())) {
            return stage.getFinalInspectionYieldRateMergeKey();
        }
        if ("theoreticalOutputQty".equals(subColumn) && stage != null
                && StrUtil.isNotBlank(stage.getTheoreticalOutputMergeKey())) {
            return stage.getTheoreticalOutputMergeKey();
        }
        if ("goodYieldRate".equals(subColumn) && stage != null
                && StrUtil.isNotBlank(stage.getGoodYieldRateMergeKey())) {
            return stage.getGoodYieldRateMergeKey();
        }
        if ("goodTargetRate".equals(subColumn) && stage != null
                && StrUtil.isNotBlank(stage.getGoodTargetRateMergeKey())) {
            return stage.getGoodTargetRateMergeKey();
        }
        if (row.getStageMergeKeys() != null && StrUtil.isNotBlank(row.getStageMergeKeys().get(stageCode))) {
            return row.getStageMergeKeys().get(stageCode);
        }
        return processPivotPlanMergeKey(row, rowIndex) + "|" + stageCode + "|" + subColumn + "|" + rowIndex;
    }

    private List<String> processPivotBaseColumns(QmsMotherRollGoodStatisticsPageReqVO reqVO) {
        List<String> columns = reqVO.getExportBaseColumns();
        if (columns == null || columns.isEmpty()) {
            return DEFAULT_PROCESS_PIVOT_BASE_COLUMNS;
        }
        List<String> validColumns = columns.stream()
                .filter(PROCESS_PIVOT_BASE_COLUMN_ORDER::contains)
                .toList();
        return validColumns.isEmpty() ? DEFAULT_PROCESS_PIVOT_BASE_COLUMNS : validColumns;
    }

    private List<String> processPivotStageColumns(QmsMotherRollGoodStatisticsPageReqVO reqVO) {
        List<String> columns = reqVO.getExportStageColumns();
        if (columns == null || columns.isEmpty()) {
            return DEFAULT_PROCESS_PIVOT_STAGE_COLUMNS;
        }
        List<String> validColumns = columns.stream()
                .filter(this::isValidProcessPivotStageColumn)
                .toList();
        return validColumns.isEmpty() ? DEFAULT_PROCESS_PIVOT_STAGE_COLUMNS : validColumns;
    }

    private boolean isValidProcessPivotStageColumn(String column) {
        String stageCode = stageCode(column);
        String subColumn = stageSubColumn(column);
        if (!PROCESS_PIVOT_STAGE_ORDER.contains(stageCode)
                || !PROCESS_PIVOT_STAGE_SUB_COLUMN_ORDER.contains(subColumn)) {
            return false;
        }
        if ("FORMULA".equals(stageCode) && "defectQty".equals(subColumn)) {
            return false;
        }
        if ("finalInspectionOutputQty".equals(subColumn) && !"CUT_ROUND".equals(stageCode)) {
            return false;
        }
        if ("finalInspectionYieldRate".equals(subColumn) && !"CUT_ROUND".equals(stageCode)) {
            return false;
        }
        if ("processProductionInspectionQty".equals(subColumn)
                && !List.of("PRESS_SLOT", "ADHESIVE2", "CUT_ROUND").contains(stageCode)) {
            return false;
        }
        if ("goodTargetRate".equals(subColumn) && !"CUT_ROUND".equals(stageCode)) {
            return false;
        }
        if (("segmentInputQty".equals(subColumn) || "segmentDefectQty".equals(subColumn))
                && !PIECE_INPUT_STAGE_CODES.contains(stageCode)) {
            return false;
        }
        if ("glueBoardInspectionQty".equals(subColumn) && !"ADHESIVE2".equals(stageCode)) {
            return false;
        }
        if (("coaInspectionQty".equals(subColumn) || "coaInspectionNgQty".equals(subColumn))
                && !COA_INSPECTION_STAGE_CODES.contains(stageCode)) {
            return false;
        }
        return !"inputQty".equals(subColumn)
                || METER_REPORT_STAGE_CODES.contains(stageCode)
                || PIECE_INPUT_STAGE_CODES.contains(stageCode);
    }

    private String stageCode(String column) {
        if (StrUtil.isBlank(column) || !column.contains(".")) {
            return "";
        }
        return StrUtil.subBefore(column, ".", false);
    }

    private String stageSubColumn(String column) {
        if (StrUtil.isBlank(column) || !column.contains(".")) {
            return "";
        }
        return StrUtil.subAfter(column, ".", false);
    }

    private String processPivotBaseValue(QmsMotherRollGoodStatisticsRespVO row, String column) {
        return switch (column) {
            case "planNo" -> blankToDash(row.getPlanNo());
            case "motherRollBatchNo" -> motherBatchText(row);
            case "planStatus" -> resolvePlanStatusName(row.getPlanStatus());
            case "modelCode" -> blankToDash(StrUtil.blankToDefault(row.getModelSeriesCode(),
                    StrUtil.blankToDefault(row.getModelCode(), row.getModelName())));
            case "materialCode" -> blankToDash(row.getMaterialCode());
            case "sizeSpec" -> blankToDash(StrUtil.blankToDefault(row.getSizeName(), row.getSizeSpec()));
            case "segmentBatchNo" -> blankToDash(row.getSegmentBatchNo());
            default -> "-";
        };
    }

    private String processPivotStageValue(QmsMotherRollGoodStatisticsStageRespVO stage, String stageCode, String subColumn) {
        return switch (subColumn) {
            case "batch" -> stageBatchText(stage, stageCode);
            case "startPosition" -> formatDecimal(stage == null ? null : stage.getStartPosition());
            case "processLength" -> formatDecimal(stage == null ? null : stage.getProcessLength());
            case "segmentInputQty" -> stageSegmentInputText(stage);
            case "inputQty" -> stageInputText(stage);
            case "doneQty" -> stageDoneText(stage);
            case "motherOutputQty" -> stageMotherOutputText(stage);
            case "finalInspectionOutputQty" -> stageFinalInspectionOutputText(stage);
            case "finalInspectionYieldRate" -> stageFinalInspectionYieldRateText(stage);
            case "processProductionInspectionQty" -> stageProcessLossText(stage);
            case "theoreticalOutputQty" -> stageTheoreticalOutputText(stage);
            case "goodYieldRate" -> stageGoodYieldRateText(stage);
            case "goodTargetRate" -> stageGoodTargetRateText(stage);
            case "glueBoardInspectionQty" -> stageGlueBoardInspectionText(stage);
            case "inspectionQty" -> stageInspectionText(stage);
            case "inspectionNgQty" -> stageInspectionNgText(stage);
            case "coaInspectionQty" -> stageCoaInspectionText(stage);
            case "coaInspectionNgQty" -> stageCoaInspectionNgText(stage);
            case "pendingQty" -> stagePendingText(stage);
            case "segmentDefectQty" -> stageSegmentDefectText(stage);
            case "defectQty" -> stageDefectText(stage);
            case "lastReportTime" -> formatDateTime(stage == null ? null : stage.getLastReportTime());
            default -> "-";
        };
    }

    private QmsMotherRollGoodStatisticsStageRespVO stage(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages, String code) {
        return stages == null ? null : stages.get(code);
    }

    private String motherBatchText(QmsMotherRollGoodStatisticsRespVO row) {
        return blankToDash(StrUtil.blankToDefault(
                StrUtil.blankToDefault(row.getMotherRollBatchNo(), row.getParentProductionBatchNo()),
                StrUtil.blankToDefault(row.getProductionBatchNo(), row.getBatchNo())));
    }

    private String stageBatchText(QmsMotherRollGoodStatisticsStageRespVO stage, String stageCode) {
        if (stage == null) {
            return "-";
        }
        String text = StrUtil.blankToDefault(stage.getOutputBatchNos(), stage.getSourceBatchNos());
        if (StrUtil.isBlank(text)) {
            return "-";
        }
        if ("ADHESIVE1".equals(stageCode)) {
            String cleaned = text
                    .replaceAll("\\s*\\d+(?:\\.\\d+)?\\s*(?:-|~|至|到)\\s*\\d+(?:\\.\\d+)?\\s*(?:m|米)", "")
                    .replaceAll("\\s{2,}", " ")
                    .replaceAll("\\s*([,，；;])\\s*", "$1 ")
                    .trim();
            return StrUtil.blankToDefault(cleaned, text);
        }
        return text;
    }

    private boolean isMeterReportStage(String stageCode) {
        return METER_REPORT_STAGE_CODES.contains(stageCode);
    }

    private String stageInputColumnLabel(String stageCode) {
        if (METER_REPORT_STAGE_CODES.contains(stageCode)) {
            return "投入米数";
        }
        if (PIECE_INPUT_STAGE_CODES.contains(stageCode)) {
            return "总投入";
        }
        return PROCESS_PIVOT_STAGE_SUB_LABELS.getOrDefault("inputQty", "投入");
    }

    private String stageDoneColumnLabel(String stageCode) {
        if ("WET".equals(stageCode)) {
            return "收卷米数(报工数)";
        }
        if (isMeterReportStage(stageCode)) {
            return "产出米数";
        }
        if (PIECE_INPUT_STAGE_CODES.contains(stageCode)) {
            return "分段自检OK";
        }
        return "完工";
    }

    private String stageMotherOutputColumnLabel(String stageCode) {
        if (PIECE_INPUT_STAGE_CODES.contains(stageCode)) {
            return "总产出";
        }
        return PROCESS_PIVOT_STAGE_SUB_LABELS.getOrDefault("motherOutputQty", "母卷产出");
    }

    private String stageInspectionColumnLabel(String stageCode) {
        if ("WET".equals(stageCode)) {
            return "NAP层送检(米)";
        }
        if (isMeterReportStage(stageCode)) {
            return "NAP留样米数/送检送数";
        }
        return "送检";
    }

    private String stageInspectionNgColumnLabel(String stageCode) {
        if (PIECE_INPUT_STAGE_CODES.contains(stageCode)) {
            return "送检NG";
        }
        return PROCESS_PIVOT_STAGE_SUB_LABELS.getOrDefault("inspectionNgQty", "检验NG");
    }

    private String stageDefectColumnLabel(String stageCode) {
        if ("WET".equals(stageCode) || isMeterReportStage(stageCode)) {
            return "固定损耗（米）";
        }
        if (PIECE_SELF_CHECK_STAGE_CODES.contains(stageCode)) {
            return "自检总NG";
        }
        return "NG";
    }

    private String stageLastReportTimeLabel(String stageCode) {
        if (PIECE_SELF_CHECK_STAGE_CODES.contains(stageCode)) {
            return "扫码确认时间";
        }
        if ("SHIPPING_INSPECTION".equals(stageCode)) {
            return "送检时间";
        }
        return "完工时间";
    }

    private String stageInputText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getInputQty());
    }

    private String stageSegmentInputText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getSegmentInputQty());
    }

    private String stageDoneText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getDoneQty());
    }

    private String stageMotherOutputText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatQty(stage.getMotherOutputQty(),
                StrUtil.blankToDefault(stage.getMotherOutputUnit(), stage.getReportUnit()));
    }

    private String stageFinalInspectionOutputText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatQty(stage.getFinalInspectionOutputQty(),
                StrUtil.blankToDefault(stage.getFinalInspectionOutputUnit(), "片"));
    }

    private String stageTheoreticalOutputText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null || !Boolean.TRUE.equals(stage.getTheoreticalOutputMatched())) {
            return "-";
        }
        return formatQty(stage.getTheoreticalOutputQty(),
                StrUtil.blankToDefault(stage.getTheoreticalOutputUnit(), stage.getReportUnit()));
    }

    private String stageGoodYieldRateText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null || stage.getGoodYieldRate() == null) {
            return "-";
        }
        return formatDecimal(stage.getGoodYieldRate()) + "%";
    }

    private String stageFinalInspectionYieldRateText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null || stage.getFinalInspectionYieldRate() == null) {
            return "-";
        }
        return formatDecimal(stage.getFinalInspectionYieldRate()) + "%";
    }

    private String stageGoodTargetRateText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null || stage.getGoodTargetRate() == null) {
            return "-";
        }
        return formatDecimal(stage.getGoodTargetRate()) + "%";
    }

    private String stageInspectionText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getInspectionQty());
    }

    private String stageProcessLossText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getProcessProductionInspectionQty());
    }

    private String stageCoaInspectionText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getCoaInspectionQty());
    }

    private String stageGlueBoardInspectionText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatQty(stage.getGlueBoardInspectionQty(), "m");
    }

    private String stageInspectionNgText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getInspectionNgQty());
    }

    private String stageCoaInspectionNgText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getCoaInspectionNgQty());
    }

    private String stagePendingText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null) {
            return "-";
        }
        return formatDecimal(stage.getPendingQty());
    }

    private String stageDefectText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getDefectQty());
    }

    private String stageSegmentDefectText(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getSegmentDefectQty());
    }

    private String formatDecimal(BigDecimal value) {
        return formatQty(value, "");
    }

    private String formatQty(BigDecimal value, String unit) {
        if (value == null) {
            return "-";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString() + StrUtil.nullToEmpty(unit);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(PROCESS_PIVOT_TIME_FORMATTER);
    }

    private String resolvePlanStatusName(String status) {
        return switch (String.valueOf(status).toUpperCase()) {
            case "DRAFT" -> "草稿";
            case "RELEASED" -> "已下达";
            case "CLOSED" -> "已关闭";
            default -> blankToDash(status);
        };
    }

    private String blankToDash(String value) {
        return StrUtil.blankToDefault(value, "-");
    }


}
