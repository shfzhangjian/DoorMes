package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 粘胶1提交 FAI 首检申请 Request VO")
@Data
public class HcAdhesiveFaiApplyReqVO {

    @NotNull(message = "计划ID不能为空")
    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planOperationId;

    @Schema(description = "来源二磨分段ID；为空时表示整体母卷首检")
    private Long sourceGrindingSecondDetailId;

    @Schema(description = "送检米数")
    private BigDecimal sampleLength;

    @Schema(description = "触发原因；NEW_ORDER=新单首检，REWORK_RECHECK=驳回/取消/NG后重提")
    private String triggerReason;

    @Schema(description = "检验标准匹配方式：MATERIAL_PROCESS=按物料编码与工段匹配；后端默认 MATERIAL_PROCESS")
    private String standardMatchMode;

    @Schema(description = "送检人")
    private String submitterName;

    @Schema(description = "胶板领用记录ID")
    private Long glueBoardUsageId;

    @Schema(description = "胶板料号")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板批号")
    private String glueBoardBatchNo;

    @Schema(description = "备注")
    private String remark;
}
