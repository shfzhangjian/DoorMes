package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive;

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

/**
 * 粘胶1可视化加工分段开工完工时间事实。
 */
@TableName("mes_sfc_adhesive_segment_timing")
@KeySequence("mes_sfc_adhesive_segment_timing_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcAdhesiveSegmentTimingDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long sourceGrindingSecondDetailId;
    private String motherBatchNo;
    private String segmentBatchNo;
    private String segmentMark;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long startOperatorId;
    private String startOperatorName;
    private Long endOperatorId;
    private String endOperatorName;
    private Long tenantId;

}
