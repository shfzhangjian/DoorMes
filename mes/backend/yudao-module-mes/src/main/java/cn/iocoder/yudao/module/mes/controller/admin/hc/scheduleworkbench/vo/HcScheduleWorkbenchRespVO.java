package cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - HC 排程工作台 Response VO")
@Data
public class HcScheduleWorkbenchRespVO {

    @Schema(description = "日期范围-开始")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "日期范围-结束")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "日期列")
    private List<String> dateColumns;

    @Schema(description = "顶部指标")
    private List<Metric> metrics;

    @Schema(description = "排程矩阵行")
    private List<Row> rows;

    @Schema(description = "本周NG明细")
    private List<NgSummary> ngSummaries;

    @Schema(description = "湿法换水申请")
    private List<WetWaterChangeApply> wetWaterChangeApplies;

    @Data
    public static class WetWaterChangeApply {

        @Schema(description = "申请ID")
        private Long id;

        @Schema(description = "换水开始日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate changeStartDate;

        @Schema(description = "换水结束日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate changeEndDate;

        @Schema(description = "换水区域说明")
        private String areaDesc;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "申请人ID")
        private Long applicantId;

        @Schema(description = "申请人")
        private String applicantName;

        @Schema(description = "申请时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime applyTime;

        @Schema(description = "同意确认人ID")
        private Long confirmerId;

        @Schema(description = "同意确认人")
        private String confirmerName;

        @Schema(description = "确认时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime confirmTime;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;

    }

    @Data
    public static class Metric {

        @Schema(description = "指标编码")
        private String code;

        @Schema(description = "指标名称")
        private String label;

        @Schema(description = "指标值")
        private BigDecimal value;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "辅助说明")
        private String description;

    }

    @Data
    public static class Row {

        @Schema(description = "计划ID")
        private Long planId;

        @Schema(description = "计划号")
        private String planNo;

        @Schema(description = "计划日期")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate planDate;

        @Schema(description = "计划模式")
        private String planMode;

        @Schema(description = "计划类型")
        private String sourceType;

        @Schema(description = "生产类型")
        private String prodType;

        @Schema(description = "计划状态")
        private String planStatus;

        @Schema(description = "型号")
        private String modelCode;

        @Schema(description = "型号改型汇总")
        private List<ChangeoverSummary> modelChangeoverSummaries;

        @Schema(description = "母卷号")
        private String motherRollNo;

        @Schema(description = "未开工母批预览，仅展示，不占用流水")
        private String motherBatchPreviewNo;

        @Schema(description = "利库来源母卷批次，去重后以顿号分隔")
        private String inventorySourceBatchNos;

        @Schema(description = "执行要求")
        private String requirement;

        @Schema(description = "配料+湿法标记")
        private Boolean frontProcessEnabled;

        @Schema(description = "后加工标记")
        private Boolean postProcessEnabled;

        @Schema(description = "拆批关联计划号")
        private String splitPlanNos;

        @Schema(description = "规格")
        private String sizeSpec;

        @Schema(description = "规格改型汇总")
        private List<ChangeoverSummary> sizeChangeoverSummaries;

        @Schema(description = "数量")
        private BigDecimal quantity;

        @Schema(description = "数量单位")
        private String quantityUnit;

        @Schema(description = "数量对应的最后工序")
        private String quantityOperationName;

        @Schema(description = "湿法收卷量(m)")
        private BigDecimal wetRollQty;

        @Schema(description = "二磨完成量(m)")
        private BigDecimal secondGrindingQty;

        @Schema(description = "分切已确认数量(片)")
        private BigDecimal slittingConfirmedQty;

        @Schema(description = "粘胶2已报工数量(片)")
        private BigDecimal adhesive2Qty;

        @Schema(description = "粘胶2是否存在换型")
        private Boolean adhesive2ChangeoverFlag;

        @Schema(description = "已配货数量(片)")
        private BigDecimal pickedQty;

        @Schema(description = "计划交期")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dueDate;

        @Schema(description = "完成情况")
        private String completionStatus;

        @Schema(description = "主批号")
        private String batchNo;

        @Schema(description = "生产批号")
        private String productionBatchNo;

        @Schema(description = "日期单元格，key 为 yyyy-MM-dd")
        private Map<String, Cell> cells;

    }

    @Data
    public static class ChangeoverSummary {

        @Schema(description = "差异类型，MODEL/SIZE")
        private String type;

        @Schema(description = "来源工序")
        private String sourceOperation;

        @Schema(description = "计划值")
        private String planValue;

        @Schema(description = "实际值")
        private String actualValue;

        @Schema(description = "实际尺寸尾号")
        private String actualSuffix;

        @Schema(description = "数量")
        private Integer qty;

        @Schema(description = "单位")
        private String unit;

    }

    @Data
    public static class ChangeoverDetail {

        @Schema(description = "计划ID")
        private Long planId;

        @Schema(description = "来源报工ID")
        private Long sourceReportId;

        @Schema(description = "来源工序")
        private String sourceOperation;

        @Schema(description = "计划值")
        private String planValue;

        @Schema(description = "实际值")
        private String actualValue;

        @Schema(description = "实际尺寸尾号")
        private String actualSuffix;

        @Schema(description = "片号")
        private String productionBatchNo;

        @Schema(description = "粘胶型号")
        private String glueBoardModel;

        @Schema(description = "报工人/确认人")
        private String reporterName;

        @Schema(description = "报工时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime reportTime;

        @Schema(description = "报工状态")
        private String reportStatus;

    }

    @Data
    public static class Cell {

        @Schema(description = "日期")
        private String date;

        @Schema(description = "计划安排工序")
        private List<String> plannedOperations;

        @Schema(description = "计划安排工序状态明细")
        private List<PlannedOperation> plannedOperationDetails;

        @Schema(description = "实际报工工序摘要")
        private List<Fact> facts;

        @Schema(description = "单元格NG数量")
        private Integer ngQty;

        @Schema(description = "单元格显示标签")
        private String statusLabel;

    }

    @Data
    public static class PlannedOperation {

        @Schema(description = "计划工序 ID")
        private Long planOperationId;

        @Schema(description = "工序名称")
        private String operationName;

        @Schema(description = "工序状态编码")
        private String statusCode;

        @Schema(description = "工序状态文案")
        private String statusLabel;

    }

    @Data
    public static class Fact {

        @Schema(description = "工序名称")
        private String operationName;

        @Schema(description = "事实来源")
        private String factSource;

        @Schema(description = "分段号")
        private String segmentBatchNo;

        @Schema(description = "投入量")
        private BigDecimal inputQty;

        @Schema(description = "投入单位")
        private String inputUnit;

        @Schema(description = "完成量")
        private BigDecimal reportQty;

        @Schema(description = "报工单位")
        private String reportUnit;

        @Schema(description = "未加工量")
        private BigDecimal pendingQty;

        @Schema(description = "未加工单位")
        private String pendingUnit;

        @Schema(description = "损耗/NG量")
        private BigDecimal lossNgQty;

        @Schema(description = "损耗/NG单位")
        private String lossNgUnit;

        @Schema(description = "记录数")
        private Integer recordCount;

        @Schema(description = "NG数量")
        private Integer ngQty;

        @Schema(description = "是否COA专用片")
        private Boolean coaFlag;

        @Schema(description = "COA专用片是否存在NG")
        private Boolean coaNgFlag;

        @Schema(description = "是否报工完成")
        private Boolean completedFlag;

        @Schema(description = "设备名称汇总")
        private String equipmentNames;

        @Schema(description = "最后报工时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastReportTime;

        @Schema(description = "粘胶型号")
        private String glueBoardModel;

        @Schema(description = "首检检验结果")
        private String firstInspectionResult;

    }

    @Data
    public static class NgSummary {

        @Schema(description = "工序名称")
        private String operationName;

        @Schema(description = "NG数量")
        private Integer ngQty;

    }

}
