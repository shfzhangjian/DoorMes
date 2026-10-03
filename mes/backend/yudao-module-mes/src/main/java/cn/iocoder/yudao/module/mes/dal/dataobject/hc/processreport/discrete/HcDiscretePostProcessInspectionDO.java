package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.discrete;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_discrete_post_process_inspection")
@KeySequence("mes_sfc_discrete_post_process_inspection_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcDiscretePostProcessInspectionDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private String inspectionTaskNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long sourceLockId;
    private Long sourceStockId;
    private String sourceBatchNo;
    private String sourceParentBatchNo;
    private String sourcePlanNo;
    private String reportSourceType;
    private Long reportSourceId;
    private String inspectionType;
    private String inspectionStatus;
    private String inspectionResult;
    private LocalDate reportDate;
    private LocalDateTime reportTime;
    private String reporterName;
    private String receiverName;
    private LocalDateTime inspectionTime;
    private String inspectorName;
    private String remark;

}
