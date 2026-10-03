package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
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

@TableName("mes_qms_quality_standard_audit_snapshot")
@KeySequence("mes_qms_quality_standard_audit_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsQualityStandardAuditSnapshotDO extends BaseDO {

    @TableId
    private Long id;

    private Long standardId;

    private String applyType;

    private String mainSnapshotJson;

    private String itemsSnapshotJson;

    private Integer beforeAuditStatus;

    private Long beforeAuditorId;

    private String beforeAuditorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime beforeAuditTime;

    private Long changeUserId;

    private String changeUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime changeTime;

    private String auditResult;

    private String rejectReason;

    private Long auditUserId;

    private String auditUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditTime;

    private String snapshotStatus;

    private Long tenantId;
}
