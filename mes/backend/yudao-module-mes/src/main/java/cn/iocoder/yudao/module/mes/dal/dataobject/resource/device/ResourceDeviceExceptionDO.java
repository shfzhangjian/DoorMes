package cn.iocoder.yudao.module.mes.dal.dataobject.resource.device;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_resource_device_exception")
@KeySequence("mes_resource_device_exception_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDeviceExceptionDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String exceptionNo;
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String exceptionLevel;
    private String faultDesc;
    private Long reporterId;
    private String reporter;
    private LocalDateTime reportTime;
    private Long dispatcherId;
    private String dispatcher;
    private LocalDateTime dispatchTime;
    private String responseResult;
    private String responseRemark;
    private Long assigneeId;
    private String assignee;
    private LocalDateTime repairPlanTime;
    private String repairPlanRemark;
    private String faultReason;
    private String repairAction;
    private LocalDateTime repairTime;
    private String repairStatus;
    private String partChangeDesc;
    private String confirmResult;
    private String confirmRemark;
    private String unfixReason;
    private Long confirmerId;
    private String confirmer;
    private LocalDateTime confirmTime;
    private Long archiverId;
    private String archiver;
    private LocalDateTime archiveTime;
    private String archiveReason;
    private String faultCategory;
    private String faultSubCategory;
    private String impactScope;
    private String rootCause;
    private String preventiveAction;
    private String attachments;
    private String status;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
