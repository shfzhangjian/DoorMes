package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName(value = "mes_qms_exception_group_task_member", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsExceptionGroupTaskMemberDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long exceptionId;
    private Long taskId;
    private Long deptId;
    private String deptName;
    private Long userId;
    private String userName;
    private Long delegateUserId;
    private String delegateUserName;
    private LocalDateTime delegateTime;
    private Long actualHandlerUserId;
    private String actualHandlerUserName;
    private String memberRole;
    private Integer sortNo;
    private LocalDateTime actualFinishTime;
    private String actionDescription;
    private String rootCauseCategory;
    private String rootCause;
    private String preventiveAction;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentUrls;
    private String confirmStatus;
    private LocalDateTime confirmTime;
    private String confirmRemark;
    private Boolean overdueFlag;
    private Long tenantId;
}
