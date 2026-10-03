package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailyCompareRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailySyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailySyncStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotStageRespVO;
import cn.iocoder.yudao.module.mes.service.hc.planorder.HcPlanProcessPivotDailyService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - HC Plan Process Pivot Daily")
@RestController
@RequestMapping("/mes/hc/plan/process-pivot-daily")
@Validated
public class HcPlanProcessPivotDailyController {

    private static final DateTimeFormatter PROCESS_PIVOT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> DEFAULT_PROCESS_PIVOT_BASE_COLUMNS = List.of(
            "planNo", "motherRollBatchNo", "planStatus", "modelCode", "sizeSpec");
    private static final List<String> DEFAULT_PROCESS_PIVOT_STAGE_COLUMNS = List.of(
            "FORMULA.doneQty", "FORMULA.inspectionQty", "FORMULA.inspectionNgQty",
            "WET.doneQty", "WET.inspectionQty", "WET.inspectionNgQty",
            "GRINDING.batch", "GRINDING.doneQty", "GRINDING.inspectionQty", "GRINDING.inspectionNgQty",
            "ADHESIVE1.doneQty", "ADHESIVE1.inspectionQty", "ADHESIVE1.inspectionNgQty",
            "SLITTING.doneQty", "SLITTING.inspectionQty", "SLITTING.inspectionNgQty", "SLITTING.pendingQty",
            "PRESS_SLOT.doneQty", "PRESS_SLOT.inspectionQty", "PRESS_SLOT.inspectionNgQty", "PRESS_SLOT.pendingQty",
            "ADHESIVE2.doneQty", "ADHESIVE2.inspectionQty", "ADHESIVE2.inspectionNgQty", "ADHESIVE2.pendingQty",
            "CUT_ROUND.doneQty", "CUT_ROUND.inspectionQty", "CUT_ROUND.inspectionNgQty", "CUT_ROUND.pendingQty",
            "SHIPPING_INSPECTION.inspectionQty", "SHIPPING_INSPECTION.inspectionNgQty");
    private static final List<String> PROCESS_PIVOT_BASE_COLUMN_ORDER = List.of(
            "planNo", "motherRollBatchNo", "planStatus", "modelCode", "materialCode", "sizeSpec");
    private static final List<String> PROCESS_PIVOT_STAGE_ORDER = List.of(
            "FORMULA", "WET", "GRINDING", "ADHESIVE1", "SLITTING", "PRESS_SLOT",
            "ADHESIVE2", "CUT_ROUND", "SHIPPING_INSPECTION");
    private static final List<String> PROCESS_PIVOT_STAGE_SUB_COLUMN_ORDER = List.of(
            "batch", "startPosition", "processLength", "doneQty", "inspectionQty", "inspectionNgQty",
            "pendingQty", "defectQty", "lastReportTime");
    private static final Map<String, String> PROCESS_PIVOT_BASE_LABELS = Map.of(
            "planNo", "计划号",
            "motherRollBatchNo", "母批批号",
            "planStatus", "状态",
            "modelCode", "型号",
            "materialCode", "料号",
            "sizeSpec", "尺寸");
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
    private static final Map<String, String> PROCESS_PIVOT_STAGE_SUB_LABELS = Map.of(
            "batch", "批号",
            "startPosition", "起位置",
            "processLength", "长度",
            "doneQty", "完工",
            "inspectionQty", "送检",
            "inspectionNgQty", "检验NG",
            "pendingQty", "未加工",
            "defectQty", "NG/损耗",
            "lastReportTime", "最后时间");

    @Resource
    private HcPlanProcessPivotDailyService hcPlanProcessPivotDailyService;

    @GetMapping("/page")
    @Operation(summary = "分页查询生产进度日结版")
    public CommonResult<PageResult<HcPlanProcessPivotRespVO>> getPage(
            @Valid HcPlanProcessPivotPageReqVO pageReqVO) {
        return success(hcPlanProcessPivotDailyService.getPage(pageReqVO));
    }

    @GetMapping("/sync-status")
    @Operation(summary = "获取生产进度日结数据同步状态")
    public CommonResult<HcPlanProcessPivotDailySyncStatusRespVO> getSyncStatus() {
        return success(hcPlanProcessPivotDailyService.getSyncStatus());
    }

    @PostMapping("/sync")
    @Operation(summary = "差异增量同步生产进度最新业务状态")
    public CommonResult<HcPlanProcessPivotDailySyncRespVO> syncLatestSnapshot() {
        return success(hcPlanProcessPivotDailyService.syncLatestSnapshot());
    }

    @GetMapping("/compare-source")
    @Operation(summary = "比对生产进度日结数据与旧查询来源")
    public CommonResult<HcPlanProcessPivotDailyCompareRespVO> compareWithSource(
            @Valid HcPlanProcessPivotPageReqVO pageReqVO) {
        return success(hcPlanProcessPivotDailyService.compareWithSource(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出生产进度日结版")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid HcPlanProcessPivotPageReqVO pageReqVO,
            HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcPlanProcessPivotRespVO> list = hcPlanProcessPivotDailyService.getList(pageReqVO);
        List<List<String>> head = buildProcessPivotExportHead(pageReqVO);
        List<List<String>> exportRows = list.stream()
                .map(row -> buildProcessPivotExportRow(row, pageReqVO))
                .toList();
        response.addHeader("Content-Disposition",
                "attachment;filename=" + HttpUtils.encodeUtf8("生产进度（日结版）.xlsx"));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        FastExcelFactory.write(response.getOutputStream())
                .autoCloseStream(false)
                .head(head)
                .automaticMergeHead(false)
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy())
                .registerWriteHandler(new HcPlanProcessPivotExcelStyleHandler(head,
                        processPivotBaseColumns(pageReqVO).size(), exportRows.size(),
                        buildProcessPivotExportMergeRanges(list)))
                .registerConverter(new LongStringConverter())
                .sheet("生产进度（日结版）")
                .doWrite(exportRows);
    }

    private List<List<String>> buildProcessPivotExportHead(HcPlanProcessPivotPageReqVO reqVO) {
        List<List<String>> head = new ArrayList<>();
        for (String column : processPivotBaseColumns(reqVO)) {
            String label = PROCESS_PIVOT_BASE_LABELS.get(column);
            head.add(List.of(label, label));
        }
        for (String column : processPivotStageColumns(reqVO)) {
            String stageCode = stageCode(column);
            String subColumn = stageSubColumn(column);
            String stageLabel = processPivotStageHeaderLabel(stageCode);
            String subLabel = "batch".equals(subColumn)
                    ? PROCESS_PIVOT_STAGE_BATCH_LABELS.getOrDefault(stageCode, "批号")
                    : PROCESS_PIVOT_STAGE_SUB_LABELS.get(subColumn);
            head.add(List.of(stageLabel, subLabel));
        }
        return head;
    }

    private String processPivotStageHeaderLabel(String stageCode) {
        String stageLabel = PROCESS_PIVOT_STAGE_LABELS.get(stageCode);
        String unit = PROCESS_PIVOT_STAGE_UNITS.get(stageCode);
        return StrUtil.isBlank(unit) ? stageLabel : stageLabel + "(" + unit + ")";
    }

    private List<String> buildProcessPivotExportRow(HcPlanProcessPivotRespVO row,
            HcPlanProcessPivotPageReqVO reqVO) {
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

    private List<HcPlanProcessPivotExcelStyleHandler.RowMergeRange> buildProcessPivotExportMergeRanges(
            List<HcPlanProcessPivotRespVO> rows) {
        List<HcPlanProcessPivotExcelStyleHandler.RowMergeRange> ranges = new ArrayList<>();
        int start = 0;
        while (start < rows.size()) {
            String mergeKey = processPivotPlanMergeKey(rows.get(start), start);
            int end = start;
            while (end + 1 < rows.size()
                    && mergeKey.equals(processPivotPlanMergeKey(rows.get(end + 1), end + 1))) {
                end++;
            }
            if (end > start) {
                ranges.add(new HcPlanProcessPivotExcelStyleHandler.RowMergeRange(start, end));
            }
            start = end + 1;
        }
        return ranges;
    }

    private String processPivotPlanMergeKey(HcPlanProcessPivotRespVO row, int rowIndex) {
        if (row.getId() != null) {
            return "ID:" + row.getId();
        }
        if (StrUtil.isNotBlank(row.getPlanNo())) {
            return "NO:" + row.getPlanNo();
        }
        return "ROW:" + rowIndex;
    }

    private List<String> processPivotBaseColumns(HcPlanProcessPivotPageReqVO reqVO) {
        List<String> columns = reqVO.getExportBaseColumns();
        if (columns == null || columns.isEmpty()) {
            return DEFAULT_PROCESS_PIVOT_BASE_COLUMNS;
        }
        List<String> validColumns = columns.stream()
                .filter(PROCESS_PIVOT_BASE_COLUMN_ORDER::contains)
                .toList();
        return validColumns.isEmpty() ? DEFAULT_PROCESS_PIVOT_BASE_COLUMNS : validColumns;
    }

    private List<String> processPivotStageColumns(HcPlanProcessPivotPageReqVO reqVO) {
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
        return PROCESS_PIVOT_STAGE_ORDER.contains(stageCode)
                && PROCESS_PIVOT_STAGE_SUB_COLUMN_ORDER.contains(subColumn);
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

    private String processPivotBaseValue(HcPlanProcessPivotRespVO row, String column) {
        return switch (column) {
            case "planNo" -> blankToDash(row.getPlanNo());
            case "motherRollBatchNo" -> motherBatchText(row);
            case "planStatus" -> resolvePlanStatusName(row.getPlanStatus());
            case "modelCode" -> blankToDash(StrUtil.blankToDefault(row.getModelCode(), row.getModelName()));
            case "materialCode" -> blankToDash(row.getMaterialCode());
            case "sizeSpec" -> blankToDash(StrUtil.blankToDefault(row.getSizeName(), row.getSizeSpec()));
            default -> "-";
        };
    }

    private String processPivotStageValue(HcPlanProcessPivotStageRespVO stage, String stageCode, String subColumn) {
        return switch (subColumn) {
            case "batch" -> stageBatchText(stage, stageCode);
            case "startPosition" -> formatDecimal(stage == null ? null : stage.getStartPosition());
            case "processLength" -> formatDecimal(stage == null ? null : stage.getProcessLength());
            case "doneQty" -> stageDoneText(stage);
            case "inspectionQty" -> stageInspectionText(stage);
            case "inspectionNgQty" -> stageInspectionNgText(stage);
            case "pendingQty" -> stagePendingText(stage);
            case "defectQty" -> stageDefectText(stage);
            case "lastReportTime" -> formatDateTime(stage == null ? null : stage.getLastReportTime());
            default -> "-";
        };
    }

    private HcPlanProcessPivotStageRespVO stage(Map<String, HcPlanProcessPivotStageRespVO> stages, String code) {
        return stages == null ? null : stages.get(code);
    }

    private String motherBatchText(HcPlanProcessPivotRespVO row) {
        return blankToDash(StrUtil.blankToDefault(
                StrUtil.blankToDefault(row.getMotherRollBatchNo(), row.getParentProductionBatchNo()),
                StrUtil.blankToDefault(row.getProductionBatchNo(), row.getBatchNo())));
    }

    private String stageBatchText(HcPlanProcessPivotStageRespVO stage, String stageCode) {
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

    private String stageDoneText(HcPlanProcessPivotStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getDoneQty());
    }

    private String stageInspectionText(HcPlanProcessPivotStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getInspectionQty());
    }

    private String stageInspectionNgText(HcPlanProcessPivotStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getInspectionNgQty());
    }

    private String stagePendingText(HcPlanProcessPivotStageRespVO stage) {
        if (stage == null) {
            return "-";
        }
        return formatDecimal(stage.getPendingQty());
    }

    private String stageDefectText(HcPlanProcessPivotStageRespVO stage) {
        return stage == null ? "-" : formatDecimal(stage.getDefectQty());
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
