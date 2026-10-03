package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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
 * 成品库存操作流水。
 *
 * <p>与 {@code mes_inv_finished_stock} 的实时状态表分离，保存每次操作发生时的库存与库位快照。
 * 即使实时库存后续被物理清理，仍可追溯完整操作过程。</p>
 */
@TableName("mes_inv_finished_stock_txn_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFinishedStockTxnLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long finishedStockId;
    private String txnNo;
    private String txnType;
    private LocalDateTime txnTime;
    private String stockNo;
    private String outerBoxNo;
    private String innerUnitNo;
    private String sliceBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String batchNo;
    private Integer qty;
    private String qualityStatus;
    private String warehouseCode;
    private String warehouseName;
    private String locationCode;
    private String locationName;
    private String beforeStockStatus;
    private String afterStockStatus;
    private String refDocType;
    private Long refDocId;
    private String refDocNo;
    private String operatorName;
    private String remark;
}
