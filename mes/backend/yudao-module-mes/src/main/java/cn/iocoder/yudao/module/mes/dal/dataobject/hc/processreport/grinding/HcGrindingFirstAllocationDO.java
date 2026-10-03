package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 一磨前置分配加工单元。 */
@TableName("mes_sfc_grinding_first_allocation_detail")
@KeySequence("mes_sfc_grinding_first_allocation_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingFirstAllocationDO extends BaseDO {

    @TableId
    private Long id;
    private Long allocationModeId;
    /** 每个加工单元各自对应一条正式一磨明细。 */
    private Long firstDetailId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String motherBatchNo;
    /** P / Q / R / S / NONE。 */
    private String segmentMark;
    private String productionBatchNo;
    private BigDecimal startPosition;
    private BigDecimal confirmedLength;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long sandpaperStateId;
    private BigDecimal sandpaperLife;
    private String sandpaperBatchNo;
    private Long guideClothStateId;
    private Integer guideClothLife;
    private String guideClothBatchNo;
    private Long checkRecordId;
    private Long operatorId;
    private String operatorName;
    private String detailStatus;
    private String remark;
    private Long tenantId;
}
