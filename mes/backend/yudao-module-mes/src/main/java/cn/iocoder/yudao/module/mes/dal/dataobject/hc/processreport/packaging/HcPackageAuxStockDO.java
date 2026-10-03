package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
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

@TableName("mes_inv_package_aux_stock")
@KeySequence("mes_inv_package_aux_stock_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPackageAuxStockDO extends BaseDO {

    @TableId
    private Long id;

    private String auxCategory;
    private String auxCategoryName;
    private String materialCode;
    private String materialName;
    private String auxSpec;
    private String batchNo;
    private String sourceWarehouseCode;
    private String sourceWarehouseName;
    private String edgeWarehouseCode;
    private String edgeWarehouseName;
    private String stockMeasureMode;
    private BigDecimal receiveQty;
    private BigDecimal usedQty;
    private BigDecimal availableQty;
    private String stockStatus;
    private Long receiverId;
    private String receiverName;
    private LocalDate receiveDate;
    private LocalDateTime receiveTime;
    private Integer printCount;
    private LocalDateTime printTime;
    private String erpTransferNo;
    private BigDecimal transferQty;
    private String transferUnit;
    private BigDecimal unpackQty;
    private String unpackUnit;
    private String erpTransferStatus;
    private LocalDateTime erpTransferTime;
    private String erpTransferMessage;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
