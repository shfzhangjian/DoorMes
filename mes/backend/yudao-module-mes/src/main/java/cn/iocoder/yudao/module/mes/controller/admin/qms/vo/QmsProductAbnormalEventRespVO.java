package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 产品异常事件 Response VO")
@Data
public class QmsProductAbnormalEventRespVO {

    @Schema(description = "前端行唯一键")
    private String eventKey;

    @Schema(description = "检验来源类型")
    private String sourceType;

    @Schema(description = "检验类型")
    private String inspectionType;

    @Schema(description = "检验单ID")
    private Long inspectionId;

    @Schema(description = "检验标准ID")
    private Long standardId;

    @Schema(description = "检验单号")
    private String inspectionNo;

    @Schema(description = "工序")
    private String operationName;

    @Schema(description = "工序分类")
    private String processCategory;

    @Schema(description = "产品型号")
    private String productModel;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "产品批次")
    private String productBatchNo;

    @Schema(description = "检验数量")
    private BigDecimal inspectionQty;

    @Schema(description = "不合格数量")
    private BigDecimal unqualifiedQty;

    @Schema(description = "检验时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;

    @Schema(description = "检验不合格项总结")
    private String abnormalSummary;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "NCR生成状态：PENDING 待生成，GENERATED 已生成")
    private String ncrStatus;

    @Schema(description = "是否已生成NCR")
    private Boolean ncrGenerated;

    @Schema(description = "关联NCR ID")
    private Long ncrId;

    @Schema(description = "关联NCR单号")
    private String ncrNo;

    @Schema(description = "复检链路ID")
    private Long recheckGroupId;

    @Schema(description = "关联质量任务ID")
    private Long dispatchTaskId;

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

    @Schema(description = "是否允许生成NCR")
    private Boolean canGenerateNcr;
}
