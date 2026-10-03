package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 边库耗材领用台账退库登记 Request VO")
@Data
public class HcToolingConsumableLedgerReturnReqVO {

    @NotNull(message = "领用台账不能为空")
    private Long id;

    @NotBlank(message = "退库原因不能为空")
    @Size(max = 500, message = "退库原因不能超过500个字符")
    private String returnReason;

    private Long returnAuthUserId;

    @NotBlank(message = "认证人不能为空")
    @Size(max = 64, message = "认证人不能超过64个字符")
    private String returnAuthUserName;
}
