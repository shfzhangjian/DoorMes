package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务中心关联检验单 Request VO")
@Data
public class QmsDispatchTaskCreateReqVO {

    @Schema(description = "检验类型：IQC/FAI/IPQC/FQC/OQC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "检验类型不能为空")
    private String checkType;

    @Schema(description = "原检验记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原检验记录ID不能为空")
    private Long executionId;
}
