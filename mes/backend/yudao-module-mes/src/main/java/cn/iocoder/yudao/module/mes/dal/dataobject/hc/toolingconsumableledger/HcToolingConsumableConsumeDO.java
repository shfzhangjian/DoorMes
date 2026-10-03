package cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger;

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

@TableName("mes_md_tooling_consumable_consume")
@KeySequence("mes_md_tooling_consumable_consume_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcToolingConsumableConsumeDO extends BaseDO {

    @TableId
    private Long id;

    private Long ledgerId;
    private String consumableType;
    private String consumableTypeName;
    private String processCode;
    private String processName;
    private String model;
    private String batchNo;
    private BigDecimal consumeQty;
    private LocalDateTime consumeTime;
    private String planNo;
    private String productionBatchNo;
    private String productModelCode;
    private String productMaterialCode;
    private String productBatchNo;
    private BigDecimal productInputQty;
    private BigDecimal productOutputQty;
    private Long glueBoardStockId;
    private Long glueBoardUsageId;
    private Long planOperationId;
    private String consumeSource;
    private String consumeType;
    private String remark;
    private Long tenantId;
}
