package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 角色检验标准范围批量新增 Request VO")
@Data
public class QmsQualityStandardRoleScopeAddReqVO {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "范围类型：INCOMING/PROCESS/FINISHED/PACKAGING/PROCESS_GLUE_BOARD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "范围类型不能为空")
    private String scopeType;

    @Schema(description = "检验标准ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "检验标准不能为空")
    private List<Long> standardIds;
}
