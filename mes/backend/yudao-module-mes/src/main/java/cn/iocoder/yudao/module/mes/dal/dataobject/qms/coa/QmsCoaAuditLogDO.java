package cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_qms_coa_audit_log")
@KeySequence("mes_qms_coa_audit_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCoaAuditLogDO extends BaseDO {

    @TableId
    private Long id;
    private Long reportId;
    private String coaNo;
    private Integer revisionNo;
    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime actionTime;
    private String opinion;
    private String reason;
    private Long tenantId;
}
