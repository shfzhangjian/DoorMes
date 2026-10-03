package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@TableName("mes_qms_exception_group_task_confirm_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsExceptionGroupTaskConfirmLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long exceptionId;
    private String exceptionNo;
    private Long taskId;
    private String taskType;
    private Long replyId;
    private Integer replyNo;
    private Long memberId;
    private Long userId;
    private String userName;
    private String confirmAction;
    private String confirmStatus;
    private String confirmOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    private String taskStatusBefore;
    private String taskStatusAfter;
    private String remark;
    private Long tenantId;
}
