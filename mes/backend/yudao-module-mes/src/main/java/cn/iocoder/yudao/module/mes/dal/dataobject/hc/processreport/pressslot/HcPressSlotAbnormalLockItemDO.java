package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot;

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

@TableName("mes_sfc_press_slot_abnormal_lock_item")
@KeySequence("mes_sfc_press_slot_abnormal_lock_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPressSlotAbnormalLockItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long lockId;
    private Long reportId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String motherBatchNo;
    private String productionBatchNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;
    private String confirmerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lockStartTime;
    private String lockStatus;
    private Long abnormalFaiId;
    private String abnormalFaiNo;
    private String abnormalSampleBatchNo;
    private String abnormalResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime abnormalSubmitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime abnormalFeedbackTime;
    private String abnormalInspectorName;
    private Long releaseFaiId;
    private String releaseFaiNo;
    private String releaseSampleBatchNo;
    private String releaseResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseSubmitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseFeedbackTime;
    private String releaseInspectorName;
    private String releaseOpinion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;
    private Long tenantId;
}
