package cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 生产事实调账接口对象。 */
public class HcProductionFactAdjustVO {

    private HcProductionFactAdjustVO() {
    }

    @Data
    public static class ScopeReqVO {
        @NotBlank(message = "计划号不能为空")
        @Size(max = 64, message = "计划号长度不能超过 64 个字符")
        private String planNo;
        @NotNull(message = "粘胶2计划工序不能为空")
        private Long planOperationId;
        @NotBlank(message = "分段批号不能为空")
        @Size(max = 100, message = "分段批号长度不能超过 100 个字符")
        private String segmentBatchNo;
    }

    @Data
    public static class PreviewReqVO extends ScopeReqVO {
        @NotBlank(message = "目标型号不能为空")
        private String targetModelCode;
        @NotBlank(message = "目标料号不能为空")
        private String targetMaterialCode;
        @NotNull(message = "换型指令不能为空")
        private Long instructionId;
    }

    @Data
    public static class CreateReqVO extends PreviewReqVO {
        @NotBlank(message = "调账原因不能为空")
        @Size(max = 500, message = "调账原因长度不能超过 500 个字符")
        private String adjustReason;
        @Size(max = 1000, message = "实物确认依据长度不能超过 1000 个字符")
        private String evidenceRemark;
    }

    @Data
    public static class ApproveReqVO {
        @NotNull(message = "调账单不能为空")
        private Long id;
        @NotNull(message = "请指定审核结论")
        private Boolean approved;
        @Size(max = 500, message = "审核说明长度不能超过 500 个字符")
        private String approveRemark;
    }

    @Data
    public static class ExecuteReqVO {
        @NotNull(message = "调账单不能为空")
        private Long id;
        @Size(max = 500, message = "执行说明长度不能超过 500 个字符")
        private String executionRemark;
    }

    @Data
    public static class PageReqVO extends PageParam {
        private String keyword;
        private String status;
    }

    @Data
    public static class OperationOptionRespVO {
        private Long id;
        private String opCode;
        private String opName;
    }

    @Data
    public static class SegmentOptionRespVO {
        private String segmentBatchNo;
        private Integer reportCount;
    }

    @Data
    public static class ProductOptionRespVO {
        private Long productModelId;
        private String modelCode;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String specification;
    }

    @Data
    public static class InstructionOptionRespVO {
        private Long id;
        private String instructionNo;
        private String targetModelCode;
        private String targetMaterialCode;
        private Integer targetQty;
        private Integer completedQty;
        private String executeStatus;
    }

    @Data
    public static class PreviewRespVO {
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private String operationCode;
        private String operationName;
        private String segmentBatchNo;
        private Long instructionId;
        private String instructionNo;
        private String sourceModelCode;
        private String sourceMaterialCode;
        private String sourceMaterialName;
        private ProductOptionRespVO targetProduct;
        private Integer adhesive2ReportCount;
        private Integer cutRoundReportCount;
        private Integer outputStockCount;
        private Integer cutInspectionDetailCount;
        private Integer fqcOrderCount;
        private Integer fqcSubmissionDetailCount;
        private Integer faiOrderCount;
        private Integer processFormCount;
        private Integer packagingCount;
        private Integer finishedStockCount;
        private Integer unsafeFqcOrderCount;
        private Integer unsafeFaiOrderCount;
        private boolean eligible;
        private List<String> blockingReasons;
    }

    @Data
    public static class OrderRespVO {
        private Long id;
        private String adjustNo;
        private String adjustType;
        private String status;
        private String planNo;
        private Long planOperationId;
        private String operationName;
        private String segmentBatchNo;
        private String instructionNo;
        private String sourceModelCode;
        private String sourceMaterialCode;
        private String targetModelCode;
        private String targetMaterialCode;
        private String targetMaterialName;
        private String targetSpecification;
        private String adjustReason;
        private String evidenceRemark;
        private String impactSummaryJson;
        private String approveRemark;
        private String executionRemark;
        private String applicantName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime appliedTime;
        private String approverName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime approvedTime;
        private String executorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime executedTime;
    }

    @Data
    public static class DetailRespVO {
        private Integer seqNo;
        private Long adhesive2ReportId;
        private String productionBatchNo;
        private String oldModelCode;
        private String oldMaterialCode;
        private String newModelCode;
        private String newMaterialCode;
        private String executionStatus;
        private String executionRemark;
    }
}
