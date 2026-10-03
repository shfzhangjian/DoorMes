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

/**
 * 磨皮母批级一磨模式锁。
 *
 * <p>没有模式锁的历史数据按 ORIGINAL 兼容；FIRST_ALLOCATED 只能由显式选择建立。</p>
 */
@TableName("mes_sfc_grinding_allocation_mode")
@KeySequence("mes_sfc_grinding_allocation_mode_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingAllocationModeDO extends BaseDO {

    @TableId
    private Long id;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String motherBatchNo;
    /** ORIGINAL / FIRST_ALLOCATED。 */
    private String allocationMode;
    /** 锁定模式时的来源可加工米数快照。 */
    private BigDecimal availableLengthAtLock;
    private LocalDateTime lockedTime;
    private Long lockedOperatorId;
    private String lockedOperatorName;
    private Long tenantId;
}
