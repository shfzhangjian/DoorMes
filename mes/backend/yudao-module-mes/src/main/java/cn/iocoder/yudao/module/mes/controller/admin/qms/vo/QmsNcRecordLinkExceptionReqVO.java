package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 不合格品处置单关联异常事件 Request VO")
@Data
public class QmsNcRecordLinkExceptionReqVO {

    @Schema(description = "NCR 主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "异常事件ID")
    private Long exceptionId;

    @Schema(description = "异常事件单号")
    private String exceptionNo;

    @Schema(description = "备注")
    private String remark;
}
