package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 各工序表单填写记录签核确认 Request VO")
@Data
public class HcProcessFormRecordSignerConfirmReqVO {

    @Schema(description = "记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "记录编号不能为空")
    private Long id;

    @Schema(description = "经身份认证的确认人用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "确认人不能为空")
    private Long confirmUserId;

}
