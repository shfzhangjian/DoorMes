package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 原物料不合格处置单来源检验 Response VO")
@Data
public class QmsRawMaterialNcInspectionRespVO {

    @Schema(description = "检验类型")
    private String inspectionType;

    @Schema(description = "检验类型名称")
    private String inspectionTypeName;

    @Schema(description = "检验单ID")
    private Long inspectionId;

    @Schema(description = "检验单号")
    private String inspectionNo;

    @Schema(description = "检验时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;

    @Schema(description = "供应商编码")
    private String supplierCode;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "数量")
    private BigDecimal quantity;

    @Schema(description = "单位")
    private String unitCode;

    @Schema(description = "审核人")
    private String qaInspectorName;

    @Schema(description = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime qaTime;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "异常类别")
    private String rawMaterialAbnormalCategory;

    @Schema(description = "异常类别名称")
    private String rawMaterialAbnormalCategoryName;

    @Schema(description = "异常摘要")
    private String abnormalSummary;

    @Schema(description = "是否已生成NCR")
    private Boolean ncrGenerated;

    @Schema(description = "NCR ID")
    private Long ncrId;

    @Schema(description = "NCR 单号")
    private String ncrNo;

    @Schema(description = "NCR 状态")
    private String ncrStatus;

    @Schema(description = "是否可生成NCR")
    private Boolean canGenerateNcr;

    @Schema(description = "复检链路ID")
    private Long recheckGroupId;

    @Schema(description = "复检状态：NONE、RECHECKING、RECHECK_OK、RECHECK_NG")
    private String recheckStatus;

    @Schema(description = "复检状态名称")
    private String recheckStatusName;

    @Schema(description = "驳回复检次数")
    private Integer recheckCount;

    @Schema(description = "当前展示轮次")
    private Integer recheckRoundNo;

    @Schema(description = "根检验单ID")
    private Long recheckRootInspectionId;

    @Schema(description = "根检验单号")
    private String recheckRootInspectionNo;

    @Schema(description = "上一轮检验单ID")
    private Long recheckPrevInspectionId;

    @Schema(description = "上一轮检验单号")
    private String recheckPrevInspectionNo;

    @Schema(description = "最新复检单ID")
    private Long recheckLatestInspectionId;

    @Schema(description = "最新复检单号")
    private String recheckLatestInspectionNo;

    @Schema(description = "驳回新检验单ID")
    private Long rejectNextInspectionId;

    @Schema(description = "驳回新检验单号")
    private String rejectNextInspectionNo;

    @Schema(description = "最新复检结果")
    private String recheckResult;

    @Schema(description = "最近一次驳回说明")
    private String rejectReason;

    @Schema(description = "最近一次驳回人")
    private String rejectUserName;

    @Schema(description = "最近一次驳回时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime rejectTime;

    @Schema(description = "是否允许驳回复检")
    private Boolean canRejectRecheck;
}
