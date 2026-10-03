package cn.iocoder.yudao.module.mes.controller.admin.hc.productionreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 工序报工综合报表详情。
 *
 * <p>该 DTO 只读聚合生产事实，不向任何报工、库存或质量表写入数据。</p>
 */
@Schema(description = "管理后台 - 工序报工综合报表详情")
@Data
public class HcProcessReportOverviewDetailRespVO {

    private PlanSummary plan = new PlanSummary();

    private List<OperationSummary> operations = new ArrayList<>();

    /** 不能按 planOperationId 归属的表单，仍完整保留供追溯。 */
    private List<FormRecord> unassignedForms = new ArrayList<>();

    /** 不能按 planOperationId 归属的报工，仍完整保留供追溯。 */
    private List<ReportRecord> unassignedReports = new ArrayList<>();

    @Data
    public static class PlanSummary {
        private Long id;
        private String planNo;
        private LocalDate planDate;
        private String planStatus;
        private LocalDate productionStartDate;
        private LocalDate productionEndDate;
        private String materialCode;
        private String materialName;
        private String motherMaterialCode;
        private String motherMaterialName;
        private String modelCode;
        private String modelName;
        private String motherModelCode;
        private String motherModelName;
        private String sizeSpec;
        private BigDecimal targetQty;
        private String targetUom;
        private String batchNo;
        private String productionBatchNo;
        private String parentProductionBatchNo;
    }

    @Data
    public static class OperationSummary {
        private Long id;
        private Integer opSeq;
        private String opCode;
        private String opName;
        private String operationStatus;
        private String operationStatusText;
        private String workCenterCode;
        private String workCenterName;
        private String equipmentCode;
        private String equipmentName;
        private BigDecimal requiredQty;
        private String uom;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime finishTime;
        private String finishRemark;

        private Integer reportCount = 0;
        /** 各工序生产记录表服务返回的聚合结果数量。 */
        private Integer productionRecordCount = 0;
        private Integer startReportCount = 0;
        private Integer endReportCount = 0;
        private BigDecimal reportedQty = BigDecimal.ZERO;
        private BigDecimal goodQty = BigDecimal.ZERO;
        private BigDecimal scrapQty = BigDecimal.ZERO;
        private String outputPostStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime latestReportTime;
        private String latestRecorderName;
        private String latestConfirmerName;

        private Integer formCount = 0;
        private Integer confirmedFormCount = 0;
        private Integer abnormalFormCount = 0;
        private Integer mirroredFormCount = 0;
        private List<FormRecord> forms = new ArrayList<>();
        /** 与各工序“生产记录表”页面同口径的数据。 */
        private List<ReportRecord> productionRecords = new ArrayList<>();
        private List<ReportRecord> reports = new ArrayList<>();
    }

    @Data
    public static class ReportRecord {
        private Long id;
        private Long planOperationId;
        private String sourceType;
        private String sourceTable;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate reportDate;
        private String modelCode;
        private String padType;
        private String materialCode;
        /** 生产记录的业务角色，例如 FIRST_ORIGINAL/FIRST_ALLOCATION/SECOND。 */
        private String recordRole;
        private String sourceBizType;
        private Long sourceDetailId;
        private String segmentMark;
        private String reportType;
        private String sourceMenuCode;
        private String operationStatus;
        private String reportStatus;
        private String batchNo;
        private String productionBatchNo;
        private String parentProductionBatchNo;
        private BigDecimal feedQty;
        private String inputUom;
        private BigDecimal goodQty;
        private BigDecimal scrapQty;
        private BigDecimal reportQty;
        private String reportUom;
        private BigDecimal outputQty;
        private String outputUom;
        private String outputPostStatus;
        private BigDecimal inputLength;
        private BigDecimal startPosition;
        private BigDecimal endPosition;
        private BigDecimal outputLength;
        private BigDecimal lossLength;
        private String selfCheck;
        private String defectCode;
        private String productQualityStatus;
        private String qualityLockReason;
        private String inspectionStatus;
        private String inspectionResult;
        private String inspectorName;
        private Integer innerUnitCount;
        private Integer innerPieceCount;
        private Integer outerBoxCount;
        private Integer outerPieceCount;
        private Integer inboundPieceCount;
        private String faiNo;
        private String faiStatus;
        private String faiJudgment;
        private String extraJson;
        private String remark;
        private String recorderName;
        private String confirmerName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime reportTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime startTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime endTime;
        /** 各工序生产记录服务的完整展示字段，保留原字段名。 */
        private java.util.Map<String, Object> productionData;
        private String detailJson;
    }

    @Data
    public static class FormRecord {
        private String sourceType;
        private Long id;
        private Long mirrorRecordId;
        private Long sourceProcessFormRecordId;
        private Long planOperationId;
        private String bizType;
        private Long bizId;
        private String recordNo;
        private Long templateId;
        private Long versionId;
        private String templateCode;
        private String templateName;
        private String processCode;
        private String processName;
        private String formType;
        private String formTypeName;
        private String recordScope;
        private String triggerTimingCode;
        private String triggerTimingName;
        private String modelCode;
        private String modelName;
        private String batchNo;
        private String equipmentCode;
        private String equipmentName;
        private String recordStatus;
        private String docStatus;
        private String resultStatus;
        private String inspectionResult;
        private String headerDataJson;
        private String contextJson;
        private String formRemark;
        private String confirmRemark;
        private String fillUserName;
        private String recordUserName;
        private String confirmUserName;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate recordDate;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime fillTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime recordTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime confirmTime;
        private List<FormItem> items = new ArrayList<>();
    }

    @Data
    public static class FormItem {
        private Long id;
        private Long recordId;
        private Long templateItemId;
        private Integer itemSeq;
        private String fieldKey;
        private String fieldLabel;
        private String itemName;
        private String itemCategory;
        private String stepNode;
        private String standardText;
        private String unit;
        private String valueMode;
        private String controlType;
        private String dualLabel1;
        private String dualLabel2;
        private String actualValue;
        private String actualValue2;
        private BigDecimal actualNumber;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime actualTime;
        private String resultFlag;
        private String abnormalRemark;
        private String sourceRowJson;
    }
}
