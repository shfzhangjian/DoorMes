package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶2全部来源片统一选择尾号 Request VO")
@Data
public class HcAdhesive2TailBatchAssignReqVO {

    @Schema(description = "生产计划 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "生产计划不能为空")
    private Long planId;

    @Schema(description = "计划工序 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "计划工序不能为空")
    private Long planOperationId;

    @Schema(description = "实际尺寸，仅支持 775mm 或 740mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "775mm")
    @NotBlank(message = "实际尺寸不能为空")
    private String actualSizeRule;
}
