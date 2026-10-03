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

@TableName("mes_inv_fg_shipping_notice")
@KeySequence("mes_inv_fg_shipping_notice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFgShippingNoticeDO extends BaseDO {

    @TableId
    private Long id;

    private String noticeNo;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String productType;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String productSize;
    private String orderNo;
    private String erpOrderNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shippingTime;
    private String externalProductModel;
    private String externalProductCode;
    private String externalProductInfo;
    private Integer requiredShipQty;
    private String requiredSliceRange;
    private String requiredBatchNo;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate requiredProductionDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate requiredExpiryDate;
    private String packingRequirement;
    private String shippingConfirmName;
    private String outboundConfirmName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime outboundConfirmTime;
    private String outboundConfirmRemark;
    private Integer noticeQty;
    private Integer lockedQty;
    private Integer changeVersion;
    private String noticeStatus;
    private String recorderName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;
    private String cancelName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String remark;
    private Long tenantId;
}
