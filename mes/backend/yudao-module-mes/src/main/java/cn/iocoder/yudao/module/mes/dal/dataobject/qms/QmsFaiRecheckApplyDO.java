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

@TableName("mes_qms_fai_recheck_apply")
@KeySequence("mes_qms_fai_recheck_apply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFaiRecheckApplyDO extends BaseDO {

    @TableId
    private Long id;

    private String applyNo;

    private Long sourceFaiId;

    private String sourceFaiNo;

    private String sourceInspectionType;

    private String sourceModule;

    private Long sourceReportId;

    private String sourceReportNo;

    private String productBatchNo;

    private String processCategory;

    private String operationCode;

    private String operationName;

    private String applyReason;

    private Long applyUserId;

    private String applyUserName;

    private LocalDateTime applyTime;

    private String status;

    private Long auditUserId;

    private String auditUserName;

    private LocalDateTime auditTime;

    private String auditOpinion;

    private Long generatedFaiId;

    private String generatedFaiNo;

    private Long tenantId;
}
