package cn.iocoder.yudao.module.mes.dal.dataobject.hc.plansplit;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_pp_plan_split_detail")
@KeySequence("mes_pp_plan_split_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPlanSplitDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long splitOrderId;
    private String sourceTable;
    private Long sourceId;
    private Long stockId;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String sourceCode;
    private BigDecimal splitQty;
    private String unit;
    private String detailStatus;
    private String remark;

}
