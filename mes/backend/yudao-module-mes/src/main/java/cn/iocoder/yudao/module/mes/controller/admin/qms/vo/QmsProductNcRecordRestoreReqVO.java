package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QmsProductNcRecordRestoreReqVO {

    @NotNull(message = "产品NCR主键不能为空")
    private Long id;

    @NotBlank(message = "请输入完整NCR单号确认还原")
    @Size(max = 64, message = "NCR单号长度不能超过64个字符")
    private String confirmNcNo;

    @NotBlank(message = "还原原因不能为空")
    @Size(max = 500, message = "还原原因长度不能超过500个字符")
    private String reason;
}
