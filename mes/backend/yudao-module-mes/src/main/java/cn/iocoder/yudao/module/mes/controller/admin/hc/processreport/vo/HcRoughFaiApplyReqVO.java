package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮操作看板提交首检申请 Request VO")
@Data
public class HcRoughFaiApplyReqVO {

    @NotNull(message = "计划ID不能为空")
    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planOperationId;

    @Schema(description = "触发原因；NEW_ORDER=新单首检，REWORK_RECHECK=驳回/取消后重提")
    private String triggerReason;

    @Schema(description = "检验标准匹配方式：MATERIAL_PROCESS=按物料编码与工段匹配；后端默认 MATERIAL_PROCESS")
    private String standardMatchMode;

    @Schema(description = "送检人")
    private String submitterName;

    @Schema(description = "备注")
    private String remark;
}
