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

@TableName("mes_qms_sample_abnormal_lock")
@KeySequence("mes_qms_sample_abnormal_lock_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsSampleAbnormalLockDO extends BaseDO {

    @TableId
    private Long id;

    private String lockNo;
    private String lockStatus;
    private String objectType;
    private String objectNo;
    private String sourceProcessCode;
    private String sourceProcessName;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String motherBatchNo;
    private String segmentNo;
    private String productionBatchNo;

    private Long abnormalInspectionId;
    private String abnormalInspectionNo;
    private String abnormalInspectionType;
    private String abnormalOperationCode;
    private String abnormalOperationName;
    private String abnormalObjectNo;
    private String abnormalResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime abnormalSubmitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime abnormalFeedbackTime;

    private String abnormalSourceModule;
    private Long abnormalSourceReportId;
    private String abnormalSourceReportNo;

    private Long recheckInspectionId;
    private String recheckInspectionNo;
    private String recheckInspectionType;
    private String recheckOperationCode;
    private String recheckOperationName;
    private String recheckObjectNo;
    private String recheckResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recheckSubmitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recheckFeedbackTime;

    private String recheckSourceModule;
    private Long recheckSourceReportId;
    private String recheckSourceReportNo;
    private Integer recheckCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastRecheckApplyTime;

    private String lockReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;

    private String releaseReason;
    private Long tenantId;
}
