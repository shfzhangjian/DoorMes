package cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionfactadjust;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 调账单逐片明细，首期以粘胶2报工片为最小事实单元。 */
@TableName("mes_pp_production_fact_adjust_detail")
@KeySequence("mes_pp_production_fact_adjust_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcProductionFactAdjustDetailDO extends BaseDO {

    @TableId
    private Long id;
    private Long adjustOrderId;
    private Integer seqNo;
    private Long adhesive2ReportId;
    private String productionBatchNo;
    private String oldModelCode;
    private String oldMaterialCode;
    private String newModelCode;
    private String newMaterialCode;
    private String executionStatus;
    private String executionRemark;
    private Long tenantId;
}
