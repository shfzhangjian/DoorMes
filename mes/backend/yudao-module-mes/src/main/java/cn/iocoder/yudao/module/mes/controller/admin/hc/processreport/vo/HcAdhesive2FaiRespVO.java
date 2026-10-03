package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶2 FAI 工艺参数点检摘要 Response VO")
@Data
public class HcAdhesive2FaiRespVO {

    @Schema(description = "FAI 主单 ID")
    private Long faiId;

    @Schema(description = "是否允许撤回未处理的成品COA")
    private Boolean canWithdraw;
    @Schema(description = "不能撤回的原因")
    private String withdrawBlockedReason;
    @Schema(description = "撤回原因")
    private String withdrawReason;

    @Schema(description = "FAI 工艺参数点检单号")
    private String faiNo;

    @Schema(description = "FAI 单据状态；字典:mes_fai_status")
    private String faiStatus;

    @Schema(description = "FAI 判定结果；字典:mes_qms_judgment_result")
    private String faiJudgment;

    @Schema(description = "命中的检验标准 ID")
    private Long faiStandardId;

    @Schema(description = "命中的检验标准编号")
    private String faiStandardNo;

    @Schema(description = "命中的检验标准版本")
    private String faiStandardVersion;

    @Schema(description = "提交首检申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime faiApplyTime;

    @Schema(description = "FAI 最近状态回写时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime faiReturnTime;

    @Schema(description = "最近首检驳回或 NG 说明")
    private String faiRejectReason;

    @Schema(description = "FAI 来源单号，每个粘胶2工单唯一")
    private String sourceReportNo;

    @Schema(description = "FAI 来源报工记录ID")
    private Long sourceReportId;

    @Schema(description = "产品批次/COA片号")
    private String productBatchNo;

    @Schema(description = "实际产品料号")
    private String materialCode;

    @Schema(description = "实际产品名称")
    private String materialName;

    @Schema(description = "实际产品规格")
    private String specification;

    @Schema(description = "实际产品型号")
    private String productModel;

    @Schema(description = "COA送检母卷批号")
    private String inspectionScopeBatchNo;

    @Schema(description = "检验场景：COA=历史压槽来源COA，POST_CONFIRM_COA=粘胶2成品COA，PROCESS_CHECK=过程加检")
    private String inspectionScene;

    @Schema(description = "提交备注")
    private String remark;

    @Schema(description = "胶板领用记录ID")
    private Long glueBoardUsageId;

    @Schema(description = "胶板物料编码")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板批号")
    private String glueBoardBatchNo;

    @Schema(description = "首样取样起点")
    private BigDecimal sampleStartPosition;

    @Schema(description = "首样送检长度")
    private BigDecimal sampleLength;

    @Schema(description = "是否允许粘胶2报工保存/扫码确认/工单完工")
    private Boolean allowReportSubmit;

    @Schema(description = "粘胶2显示文案")
    private String displayText;
}
