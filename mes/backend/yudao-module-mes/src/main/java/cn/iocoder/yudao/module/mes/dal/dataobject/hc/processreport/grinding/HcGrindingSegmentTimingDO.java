package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

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
 * 磨皮母批/分段开工完工时间事实。
 *
 * 原有母批使用 FIRST + MOTHER；一磨按 P/Q/R/S、二磨按 P/Q/R/S/NONE 独立存储。
 */
@TableName("mes_sfc_grinding_segment_timing")
@KeySequence("mes_sfc_grinding_segment_timing_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingSegmentTimingDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String motherBatchNo;
    private String passType;
    private String segmentMark;
    /** 原有母批填写母批号；一次磨皮分段填写自动分段批号；二次磨皮不写入。 */
    private String segmentBatchNo;
    private Long firstDetailId;
    private Long secondDetailId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long startOperatorId;
    private String startOperatorName;
    private Long endOperatorId;
    private String endOperatorName;
    private Long tenantId;

}
