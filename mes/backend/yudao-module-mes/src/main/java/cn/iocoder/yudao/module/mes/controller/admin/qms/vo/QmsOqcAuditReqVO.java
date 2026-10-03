package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - OQC出货检验审核 Request VO")
@Data
public class QmsOqcAuditReqVO {

    @NotNull(message = "OQC单据ID不能为空")
    private Long id;

    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    private String rejectReason;
}
