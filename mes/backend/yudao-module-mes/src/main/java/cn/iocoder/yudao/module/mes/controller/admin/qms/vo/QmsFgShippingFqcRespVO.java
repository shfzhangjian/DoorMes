package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 发货成品检验 Response VO")
@Data
public class QmsFgShippingFqcRespVO {

    private Long id;
    private String fqcNo;
    private String reportNo;
    private String workOrderNo;
    private Long sourceReportId;
    private String sourceReportNo;
    private String sourceModule;
    private String materialCode;
    private String materialName;
    private String specification;
    private String productModel;
    private String productBatchNo;
    private String batchNo;
    private BigDecimal produceQty;
    private Integer sampleQty;
    private Integer submissionDetailCount;
    private Integer okQty;
    private Integer ngQty;
    private String submissionType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submissionTime;
    private String submitterName;
    private Long standardId;
    private String standardNo;
    private String standardVersion;
    private String status;
    private String judgment;
    private Long inspectorId;
    private String inspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;
    private Long qaInspectorId;
    private String qaInspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime qaTime;
    private String releaseResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;
    private String remark;
    private String entryMode;
    private String entryLayout;
    private Integer entryProgress;
    private Integer requiredItemCount;
    private Integer completedItemCount;
    private Integer abnormalItemCount;
    private Boolean sheetLocked;
    private String lastReturnReason;
    private Boolean recheckFlag;
    private String originalInspectionNo;
    private String rejectRootInspectionNo;
    private String rejectPrevInspectionNo;
    private String productType;
    private String shippingNoticeNo;
    private String customerName;
    private String erpOrderNo;
    private String alignmentStatus;
    private String mismatchReason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private List<ShippingDetail> shippingDetails;
    private List<AlignmentRow> alignmentRows;
    private List<AlignmentCandidate> alignmentCandidates;
    private List<QmsFqcRespVO.FqcItem> items;
    private List<QmsFqcRespVO.FqcAbnormal> abnormals;

    @Data
    public static class ShippingDetail {
        private Long id;
        private Long fqcId;
        private String fqcNo;
        private Long shippingNoticeId;
        private String shippingNoticeNo;
        private Long shippingNoticeItemId;
        private Long shippingPickItemId;
        private Long finishedStockId;
        private String stockNo;
        private String actualSliceBatchNo;
        private String sliceBatchNo;
        private Long customerId;
        private String customerCode;
        private String customerName;
        private String erpOrderNo;
        private String customerProductBatchNo;
        private String packageSliceNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String internalItemCode;
        private String productSize;
        private Integer shippingQty;
        private String rowJudgment;
        private String defectCode;
        private String defectName;
        private String ngReason;
        private String alignmentStatus;
        private String mismatchReason;
        private Integer entryProgress;
        private Integer requiredItemCount;
        private Integer completedItemCount;
        private Integer abnormalItemCount;
        private Long inspectorId;
        private String inspectorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionTime;
        private String remark;
        private Boolean recheckDetailFlag;
        private List<QmsFqcRespVO.FqcItem> items;
    }

    @Data
    public static class AlignmentCandidate {
        private Long shippingDetailId;
        private Long fqcId;
        private String fqcNo;
        private Long shippingPickItemId;
        private Long sourceNoticeItemId;
        private Long finishedStockId;
        private String stockNo;
        private String actualSliceBatchNo;
        private String sliceBatchNo;
        private String internalModelCode;
        private String internalItemCode;
        private String customerProductBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String productSize;
        private String qualityStatus;
        private String lockStatus;
        private String packageNo;
        private String rowJudgment;
        private String alignmentStatus;
    }

    @Data
    public static class AlignmentRow {
        private Long id;
        private Long fqcId;
        private String fqcNo;
        private Long shippingNoticeItemId;
        private Long shippingDetailId;
        private Long shippingPickItemId;
        private Long finishedStockId;
        private String stockNo;
        private String actualSliceBatchNo;
        private String sliceBatchNo;
        private String customerProductBatchNo;
        private String packageSliceNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String internalItemCode;
        private String productSize;
        private Integer shippingQty;
        private String rowJudgment;
        private String alignmentStatus;
        private String mismatchReason;
    }
}
