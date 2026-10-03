package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_adhesive_aqc_task")
@KeySequence("mes_sfc_adhesive_aqc_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcAdhesiveAqcTaskDO extends BaseDO {

    @TableId
    private Long id;

    private String taskType;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long adhesiveReportId;
    private Long glueBoardUsageId;
    private String glueBoardMaterialCode;
    private String glueBoardBatchNo;
    private BigDecimal sampleStartPosition;
    private BigDecimal sampleLength;
    private String taskStatus;
    private Long submitterId;
    private String submitterName;
    private LocalDateTime submitTime;
    private String feedbackResult;
    private LocalDateTime feedbackTime;
    private String feedbackRemark;
    private BigDecimal lockStartPosition;
    private LocalDate recordDate;
    private String extraJson;
    private Long tenantId;
}
