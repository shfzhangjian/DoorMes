package cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - HC 拆批管理图谱 Response VO")
@Data
public class HcPlanSplitGraphRespVO {

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "计划日期")
    private LocalDate planDate;

    @Schema(description = "计划状态")
    private String planStatus;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "型号名称")
    private String modelName;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "主批号")
    private String batchNo;

    @Schema(description = "目标量")
    private BigDecimal targetQty;

    @Schema(description = "目标单位")
    private String targetUom;

    @Schema(description = "工序选项")
    private List<OperationOption> operations = new ArrayList<>();

    @Schema(description = "图谱节点")
    private List<Node> nodes = new ArrayList<>();

    @Schema(description = "图谱连线")
    private List<Edge> edges = new ArrayList<>();

    @Data
    public static class OperationOption {
        private Long operationId;
        private Integer opSeq;
        private String opCode;
        private String opName;
    }

    @Data
    public static class Node {
        private String nodeKey;
        private String stageCode;
        private String stageName;
        private Long operationId;
        private Integer opSeq;
        private String opCode;
        private String opName;
        private String operationStatus;
        private String batchNo;
        private String sourceBatchNo;
        private BigDecimal totalQty;
        private BigDecimal processedQty;
        private BigDecimal remainingQty;
        private String unit;
        private Boolean splitable;
        private String splitMode;
        private String sourceTable;
        private Boolean planned;
        private Boolean hasReport;
        private Boolean hasConfirmedReport;
        private String reportState;
        private Boolean transferIn;
        private String sourcePlanNo;
        private String sourceOperationName;
        private BigDecimal sourceSplitQty;
        private String sourceSplitUnit;
        private String blockedReason;
        private Boolean placeholder;
        private String remark;
        private List<Item> availableItems = new ArrayList<>();
    }

    @Data
    public static class Edge {
        private String from;
        private String to;
        private String label;
    }

    @Data
    public static class Item {
        private Long sourceId;
        private Long stockId;
        private String sourceTable;
        private String code;
        private String batchNo;
        private String sourceBatchNo;
        private BigDecimal qty;
        private String unit;
    }

}
