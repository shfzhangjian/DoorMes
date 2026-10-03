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

@TableName("mes_qms_exception_event")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsExceptionEventDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String exceptionNo;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private String exceptionType;
    private String exceptionLevel;
    private String status;
    private String processInstanceId;
    private String currentNodeCode;
    private String currentNodeName;
    private Long currentHandlerUserId;
    private String currentHandlerUserName;
    private Long discoverDeptId;
    private String discoverDeptCode;
    private String discoverDeptName;
    private Long discovererId;
    private String discovererCode;
    private String discovererName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime discoverTime;

    private Long confirmDeptId;
    private String confirmDeptCode;
    private String confirmDeptName;
    private Long confirmerId;
    private String confirmerCode;
    private String confirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    private Boolean isRelatedProduct;
    private String relatedNcrNo;
    private String related8dNo;
    private String description;
    private String initialImpact;
    private Long containmentOwnerId;
    private String containmentOwnerName;
    private String containmentAction;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime containmentDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime containmentFinishTime;

    private String rootCauseCategory;
    private String rootCause;
    private Long actionDeptId;
    private String actionDeptName;
    private Long actionOwnerId;
    private String actionOwnerName;
    private String preventiveAction;
    private Long resultUploaderId;
    private String resultUploaderName;
    private String correctivePreventiveResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resultUploadTime;

    private String copyUserIds;
    private String copyUserNames;
    private String effectConfirm;
    private Boolean qaConfirmValid;
    private Long qaConfirmerId;
    private String qaConfirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    private Long closeUserId;
    private String closeUserName;
    private String remark;
    private Long tenantId;
}
