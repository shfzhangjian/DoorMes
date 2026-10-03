package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_inv_finished_stock")
@KeySequence("mes_inv_finished_stock_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFinishedStockDO extends BaseDO {

    @TableId
    private Long id;

    private String stockNo;
    private String outerBoxNo;
    private String innerUnitNo;
    private String sliceBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String batchNo;
    private String productSize;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private Integer qty;
    private String qualityStatus;
    private String warehouseCode;
    private String warehouseName;
    private String locationCode;
    private String locationName;
    private String stockStatus;
    /** COA 限制独立于库存流转状态，不能覆盖发货占用。 */
    private Boolean coaFrozen;
    private String coaFreezeReason;
    private String inboundNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inboundTime;
    private Long tenantId;
}
