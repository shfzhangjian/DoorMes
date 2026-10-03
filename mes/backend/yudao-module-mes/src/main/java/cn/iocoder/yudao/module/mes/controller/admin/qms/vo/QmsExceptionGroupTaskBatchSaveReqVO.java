package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - QMS异常事件临时小组任务下达 Request VO")
@Data
public class QmsExceptionGroupTaskBatchSaveReqVO {

    @Schema(description = "异常ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "异常ID不能为空")
    private Long exceptionId;

    @Schema(description = "小组任务")
    @Valid
    private List<QmsExceptionGroupTaskReqVO> groupTasks;

    @Schema(description = "下达意见")
    private String opinion;
}
