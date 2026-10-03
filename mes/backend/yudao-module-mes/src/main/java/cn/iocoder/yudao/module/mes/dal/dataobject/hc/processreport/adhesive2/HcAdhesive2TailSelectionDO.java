package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_adhesive2_tail_selection")
@KeySequence("mes_sfc_adhesive2_tail_selection_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcAdhesive2TailSelectionDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long planId;
    private Long planOperationId;
    private String sourceProductionBatchNo;
    private String actualSizeRule;
    private String actualSizeSuffix;
}
