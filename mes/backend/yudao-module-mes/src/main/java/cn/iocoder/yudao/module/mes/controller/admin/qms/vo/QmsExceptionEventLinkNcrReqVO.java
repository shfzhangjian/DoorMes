package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件关联NCR Request VO")
@Data
public class QmsExceptionEventLinkNcrReqVO {

    @Schema(description = "异常ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "异常ID不能为空")
    private Long id;

    @Schema(description = "NCR ID")
    private Long ncrId;

    @Schema(description = "NCR 单号")
    private String ncrNo;

    @Schema(description = "NCR关联类型 NCR/RAW_MATERIAL_NCR")
    private String ncrType;

    @Schema(description = "备注")
    private String remark;
}
