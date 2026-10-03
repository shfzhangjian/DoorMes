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

@TableName("mes_pp_plan_split_order")
@KeySequence("mes_pp_plan_split_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPlanSplitOrderDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private String splitNo;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private Long sourceOperationId;
    private String sourceOperationCode;
    private String sourceOperationName;
    private String sourceNodeKey;
    private String stageCode;
    private String stageName;
    private String splitMode;
    private BigDecimal splitQty;
    private String unit;
    private Long targetPlanId;
    private String targetPlanNo;
    private Long targetStartOperationId;
    private String targetStartOperationCode;
    private String targetStartOperationName;
    private Long targetEndOperationId;
    private String targetEndOperationCode;
    private String targetEndOperationName;
    private Long targetMaterialId;
    private String targetMaterialCode;
    private String targetMaterialName;
    private Long targetModelId;
    private String targetModelCode;
    private String targetModelName;
    private String instructionText;
    private String splitStatus;
    private String remark;

}
