package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 流程实例调整步骤 Request VO")
@Data
public class BpmProcessInstanceAdjustStepReqVO {

    @Schema(description = "流程实例编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotBlank(message = "流程实例编号不能为空")
    private String id;

    @Schema(description = "目标节点定义 Key", requiredMode = Schema.RequiredMode.REQUIRED, example = "quality_confirm")
    @NotBlank(message = "目标节点不能为空")
    private String targetTaskDefinitionKey;

    @Schema(description = "调整原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "会签意见需要重新补充")
    @NotBlank(message = "调整原因不能为空")
    private String reason;

    @Schema(description = "是否清空业务已填内容")
    private Boolean clearBusinessData;

}
