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

@TableName("mes_sfc_grinding_source_balance")
@KeySequence("mes_sfc_grinding_source_balance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingSourceBalanceDO extends BaseDO {

    @TableId
    private Long id;

    private String balanceKey;
    private String sourceType;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private Long sourcePlanOperationId;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String modelName;
    private BigDecimal totalLength;
    private BigDecimal usedFirstLength;
    private BigDecimal usedSecondLength;
    private BigDecimal reservedLength;
    private BigDecimal availableLength;
    private LocalDateTime lastReportTime;
    private String status;
    private String remark;
    private Long tenantId;
}
