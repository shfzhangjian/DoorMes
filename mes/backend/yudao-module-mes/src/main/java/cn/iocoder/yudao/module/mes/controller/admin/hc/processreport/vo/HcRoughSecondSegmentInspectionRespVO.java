package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板二磨分段留样送检摘要 Response VO")
@Data
public class HcRoughSecondSegmentInspectionRespVO {

    @Schema(description = "二次磨皮明细ID")
    private Long secondDetailId;

    @Schema(description = "送检单ID；当前复用 FAI 首件检验单ID")
    private Long inspectionId;

    @Schema(description = "送检单号；当前复用 FAI 首件检验单号")
    private String inspectionNo;

    @Schema(description = "送检状态")
    private String inspectionStatus;

    @Schema(description = "送检结果")
    private String inspectionResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "送检时间")
    private LocalDateTime inspectionApplyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "质检反馈时间")
    private LocalDateTime inspectionReturnTime;

    @Schema(description = "驳回原因")
    private String inspectionRejectReason;

    @Schema(description = "是否允许重新送检")
    private Boolean reapplyAllowed;

    @Schema(description = "按钮显示文案")
    private String displayText;

    @Schema(description = "来源报工明细ID")
    private Long sourceReportId;

    @Schema(description = "来源报工明细号")
    private String sourceReportNo;

    @Schema(description = "来源模块")
    private String sourceModule;

    @Schema(description = "工序类别")
    private String processCategory;

    @Schema(description = "样品类型编码")
    private String sampleType;

    @Schema(description = "样品类型名称")
    private String sampleTypeName;

    @Schema(description = "留样送检米数")
    private BigDecimal sampleLength;

    @Schema(description = "加工母批号")
    private String motherBatchNo;

    @Schema(description = "加工母批号兼容字段")
    private String parentBatchNo;

    @Schema(description = "受检生产批次号")
    private String productBatchNo;

    @Schema(description = "受检生产批次号兼容字段")
    private String productionBatchNo;

    @Schema(description = "分段标记")
    private String segmentMark;
}
