package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 原材料不合格处置单还原 Request VO")
@Data
public class QmsRawMaterialNcRecordRestoreReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "手工确认的完整NCR单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入完整NCR单号进行确认")
    @Size(max = 64, message = "NCR单号长度不能超过64个字符")
    private String confirmNcNo;

    @Schema(description = "还原原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "还原原因不能为空")
    @Size(max = 500, message = "还原原因长度不能超过500个字符")
    private String reason;
}
