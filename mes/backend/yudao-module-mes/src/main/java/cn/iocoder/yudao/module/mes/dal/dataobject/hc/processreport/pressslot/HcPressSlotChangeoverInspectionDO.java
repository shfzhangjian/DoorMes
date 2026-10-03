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

@TableName("mes_sfc_press_slot_changeover_inspection")
@KeySequence("mes_sfc_press_slot_changeover_inspection_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPressSlotChangeoverInspectionDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long sourceSlittingSliceId;
    private String pressSlotSliceNo;
    private String motherSegmentBatchNo;
    private String productionModelCode;
    private String productionMaterialCode;
    private String previousModelCode;
    private String currentPlanNo;
    private String inspectionStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submitTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime feedbackTime;
    private String feedbackResult;
    private String feedbackRemark;
    private String headerDataJson;
    private String detailItemsJson;
    private String recorderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
