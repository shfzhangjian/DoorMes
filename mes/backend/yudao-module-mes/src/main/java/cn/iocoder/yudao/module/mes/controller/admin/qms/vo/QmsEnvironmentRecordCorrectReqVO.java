package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 温湿度历史记录修正 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsEnvironmentRecordCorrectReqVO extends QmsEnvironmentRecordSaveReqVO {

    @Schema(description = "历史修正原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "修正原因不能为空")
    private String correctionReason;
}
