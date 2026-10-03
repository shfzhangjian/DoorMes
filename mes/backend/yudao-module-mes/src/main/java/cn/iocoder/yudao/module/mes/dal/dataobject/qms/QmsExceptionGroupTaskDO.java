package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@TableName(value = "mes_qms_exception_group_task", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsExceptionGroupTaskDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long exceptionId;
    private String exceptionNo;
    private String groupName;
    private String taskType;
    private String taskStatus;
    private Integer replyCount;

    private Long dispatcherUserId;
    private String dispatcherUserName;

    private Long executorUserId;
    private String executorUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dispatchTime;

    private String containmentSuggestion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime containmentDeadline;

    private Long containmentDeptId;
    private String containmentDeptName;
    private Long rootCauseOwnerId;
    private String rootCauseOwnerName;
    private String rootCauseAssignment;
    private String preventiveAssignment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualFinishTime;

    private String actionDescription;
    private String rootCauseCategory;
    private String rootCause;
    private String preventiveAction;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentUrls;

    private Long submitterUserId;
    private String submitterUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    private Long reviewerUserId;
    private String reviewerUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    private String reviewOpinion;
    private String remark;
    private Long tenantId;
}
