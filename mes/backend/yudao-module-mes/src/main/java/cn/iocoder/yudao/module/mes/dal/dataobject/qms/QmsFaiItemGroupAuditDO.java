package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_fai_item_group_audit")
@KeySequence("mes_qms_fai_item_group_audit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFaiItemGroupAuditDO extends BaseDO {

    @TableId
    private Long id;

    private Long faiId;

    private String faiNo;

    private Long faiItemId;

    private String inspectionItem;

    private String groupKey;

    private String samplePosition;

    private String auditResult;

    private String auditRemark;

    private Long auditUserId;

    private String auditUserName;

    private LocalDateTime auditTime;

    private String itemRecheckStatus;

    private Boolean recheckItemFlag;

    private Long tenantId;
}
