package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - SRM供应商资源状态调整 Request VO")
@Data
public class SrmSupplierResourceStatusAdjustReqVO {

    @Schema(description = "来源类型：固定 REGISTERED，供应商资源统一使用 mes_supplier")
    @NotBlank(message = "来源类型不能为空")
    @Pattern(regexp = "REGISTERED", message = "来源类型必须为 REGISTERED")
    private String sourceType;

    @Schema(description = "供应商资源ID，对应 mes_supplier.id")
    private Long supplierId;

    @Schema(description = "调整后的资源状态")
    @NotBlank(message = "资源状态不能为空")
    @Pattern(regexp = "QUALIFIED|UNQUALIFIED|FROZEN|ELIMINATED|EXITED|PENDING",
            message = "资源状态必须为合格、不合格、冻结、淘汰、退出或考察中")
    private String status;

    @Schema(description = "调整原因")
    @NotBlank(message = "调整原因不能为空")
    @Size(max = 500, message = "调整原因不能超过 500 个字符")
    private String reason;

}
