package cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_md_tooling_consumable_ledger")
@KeySequence("mes_md_tooling_consumable_ledger_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcToolingConsumableLedgerDO extends BaseDO {

    @TableId
    private Long id;

    private String consumableType;
    private String consumableTypeName;
    private String processCode;
    private String processName;
    private String model;
    private String batchNo;
    private String erpMaterialCode;
    private BigDecimal receiveQty;

    /**
     * 实时累计消耗量；由查询时汇总，不落库。
     */
    @TableField(exist = false)
    private BigDecimal consumedQty;

    /**
     * 实时剩余量；由查询时汇总，不落库。
     */
    @TableField(exist = false)
    private BigDecimal balanceQty;

    private Long uomId;
    private String uomCode;
    private String uomName;
    private String uom;
    private LocalDateTime receiveTime;
    private Long receiverId;
    private String receiverName;
    private String usageStatus;
    private BigDecimal usedUpRemainQty;
    private LocalDate usedUpActualDate;
    private String usedUpRemark;
    private Long usedUpAuthUserId;
    private String usedUpAuthUserName;
    private LocalDateTime usedUpAuthTime;
    private BigDecimal returnQty;
    private String returnReason;
    private Long returnAuthUserId;
    private String returnAuthUserName;
    private LocalDateTime returnAuthTime;
    private String remark;
    private Long tenantId;
}
