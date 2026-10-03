// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/execution/vo/MesActionExecuteReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.execution.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - MES 生产动作执行/报工 Request VO")
@Data
public class MesActionExecuteReqVO {

    @Schema(description = "派工细单ID (上下文核心)", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "派工单ID不能为空")
    private Long subOrderId;

    @Schema(description = "SOP动作定义ID (关联 RouteProcessAction)", requiredMode = Schema.RequiredMode.REQUIRED, example = "9121")
    @NotNull(message = "动作ID不能为空")
    private Long actionId;

    @Schema(description = "实际执行工位ID (人机料法环 - 机)", requiredMode = Schema.RequiredMode.REQUIRED, example = "688")
    @NotNull(message = "执行工位不能为空")
    private Long stationId;

    @Schema(description = "表单提交数据 (Model JSON)", example = "{\"f_123\": 80.5, \"barcode\": \"M001\"}")
    private Map<String, Object> actionValue;

    @Schema(description = "校验结果 (前端预判)", example = "true")
    private Boolean isPass;

    @Schema(description = "失败/异常原因 (当 isPass=false 时必填)", example = "厚度超标")
    private String failureReason;

    @Schema(description = "备注")
    private String remark;
}
