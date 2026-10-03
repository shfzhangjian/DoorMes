package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
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

/**
 * 质量任务中心下达任务。
 */
@TableName("mes_qms_dispatch_task")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsDispatchTaskDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private String taskType;
    /** Source: INSPECTION_RECORD / MANUAL_BATCH. */
    private String sourceMode;
    /** Execution carrier: NATIVE / TASK_OWNED. */
    private String executionMode;
    /** Object: BATCH / EXECUTION_PIECE. */
    private String objectMode;
    /** Sampling: SOURCE_ITEMS / SPECIFIED_PIECE / QUANTITY_ONLY. */
    private String sampleSelectionMode;
    /** Required sample count for this task. */
    private Integer requiredSampleQty;
    private String triggerSource;
    private String checkType;
    private String inspectionScene;
    private String objectType;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long executionId;
    private String executionNo;
    private String executionRoute;
    private Long sourceExecutionId;
    private String sourceExecutionNo;
    private Long rootExecutionId;
    private String rootExecutionNo;
    private Long recheckGroupId;
    private Integer currentRoundNo;
    private Integer recheckCount;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialSpec;
    private String productModel;
    private String operationCode;
    private String operationName;
    private Long machineId;
    private String machineCode;
    private String machineName;
    private String lotNo;
    private String receiptNo;
    private String supplierName;
    private LocalDate arrivalDate;
    private BigDecimal checkQty;
    private String unit;
    private Long standardId;
    private String standardNo;
    private String standardName;
    private String standardVersion;
    private Long assigneeUserId;
    private String assigneeUserName;
    private Long assigneeDeptId;
    private String assigneeDeptName;
    private String priority;
    private LocalDateTime requiredFinishTime;
    private String dispatchStatus;
    private LocalDateTime dispatchTime;
    private String taskInstruction;
    private String processInstanceId;
    private String processStatus;
    private String sourceStatus;
    private String sourceJudgment;
    private String sourceInspectorName;
    private LocalDateTime sourceInspectionTime;
    private Integer itemCount;
    private Integer abnormalCount;
    private LocalDateTime lastSyncTime;
    private String cancelReason;
}
