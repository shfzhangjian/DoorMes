package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 质量任务中心分派 Request VO")
@Data
public class QmsDispatchTaskDispatchReqVO {

    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务ID不能为空")
    private Long id;

    @Schema(description = "执行人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行人不能为空")
    private Long assigneeUserId;

    @Schema(description = "执行人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "执行人姓名不能为空")
    private String assigneeUserName;

    @Schema(description = "执行人部门ID")
    private Long assigneeDeptId;

    @Schema(description = "执行人部门名称")
    private String assigneeDeptName;

    @Schema(description = "优先级：NORMAL/URGENT")
    private String priority;

    @Schema(description = "要求完成时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime requiredFinishTime;

    @Schema(description = "任务要求")
    private String taskInstruction;

}
