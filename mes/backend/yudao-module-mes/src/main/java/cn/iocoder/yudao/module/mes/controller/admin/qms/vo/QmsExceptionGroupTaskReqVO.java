package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS异常事件临时小组任务 Request VO")
@Data
public class QmsExceptionGroupTaskReqVO {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "小组名称")
    private String groupName;

    @Schema(description = "任务类型：INVESTIGATION_GROUP/ROOT_CAUSE_PREVENTIVE")
    private String taskType;

    @Schema(description = "任务状态")
    private String taskStatus;

    @Schema(description = "任务填写人ID")
    private Long executorUserId;

    @Schema(description = "任务填写人")
    private String executorUserName;

    @Schema(description = "围堵建议")
    private String containmentSuggestion;

    @Schema(description = "围堵期限")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime containmentDeadline;

    @Schema(description = "围堵部门ID")
    private Long containmentDeptId;

    @Schema(description = "围堵部门")
    private String containmentDeptName;

    @Schema(description = "根因分析人ID")
    private Long rootCauseOwnerId;

    @Schema(description = "根因分析人")
    private String rootCauseOwnerName;

    @Schema(description = "根因分析安排")
    private String rootCauseAssignment;

    @Schema(description = "纠正预防安排")
    private String preventiveAssignment;

    @Schema(description = "实际完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime actualFinishTime;

    @Schema(description = "措施说明")
    private String actionDescription;

    @Schema(description = "根因4M1E分类")
    private String rootCauseCategory;

    @Schema(description = "根因分析")
    private String rootCause;

    @Schema(description = "纠正预防措施")
    private String preventiveAction;

    @Schema(description = "附件证明")
    private List<String> attachmentUrls;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "小组成员")
    private List<QmsExceptionGroupTaskMemberReqVO> members;
}
