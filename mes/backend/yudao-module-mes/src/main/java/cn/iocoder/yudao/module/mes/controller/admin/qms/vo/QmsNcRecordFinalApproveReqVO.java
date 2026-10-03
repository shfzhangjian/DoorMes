package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - NCR终审 Request VO")
@Data
public class QmsNcRecordFinalApproveReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "终审处置结论", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "终审处置结论不能为空")
    private String finalDisposition;

    @Schema(description = "终审意见", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "终审意见不能为空")
    private String finalOpinion;

    @Schema(description = "终审后是否自动新建并挂接异常事件")
    private Boolean createExceptionFlag;
}
