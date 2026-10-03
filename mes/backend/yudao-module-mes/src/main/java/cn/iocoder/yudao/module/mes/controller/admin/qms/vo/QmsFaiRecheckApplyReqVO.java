package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FAI复检申请 Request VO")
@Data
public class QmsFaiRecheckApplyReqVO {

    @Schema(description = "来源FAI首件检验单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "来源FAI首件检验单不能为空")
    private Long sourceFaiId;

    @Schema(description = "申请原因")
    private String applyReason;
}
