package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - IQC进料留样补确认 Request VO")
@Data
public class QmsIqcRetentionConfirmReqVO {

    @Schema(description = "IQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "IQC主单ID不能为空")
    private Long id;
}
