package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS异常事件部门/责任单位办理内容提交 Request VO")
@Data
public class QmsExceptionGroupTaskMemberConfirmReqVO {

    @Schema(description = "小组任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "小组任务ID不能为空")
    private Long taskId;

    @Schema(description = "会签成员ID")
    private Long memberId;

    @Schema(description = "动作：CONFIRM提交/DISAGREE不同意")
    private String actionCode;

    @Schema(description = "实际完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime actualFinishTime;

    @Schema(description = "围堵措施/措施说明")
    private String actionDescription;

    @Schema(description = "根因4M1E分类")
    private String rootCauseCategory;

    @Schema(description = "根因分析")
    private String rootCause;

    @Schema(description = "纠正预防措施与标准化")
    private String preventiveAction;

    @Schema(description = "附件证明")
    private List<String> attachmentUrls;

    @Schema(description = "确认备注")
    private String remark;
}
