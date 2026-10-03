package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_inv_fg_shipping_pick_item")
@KeySequence("mes_inv_fg_shipping_pick_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFgShippingNoticePickItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long noticeId;
    private String noticeNo;
    private Long sourceNoticeItemId;
    private String pickSourceType;
    private Long sourceInnerPackItemId;
    private Long sourceCutRoundReportId;
    private Long finishedStockId;
    private String stockNo;
    private String outerBoxNo;
    private String innerUnitNo;
    private String packageNo;
    private String sliceBatchNo;
    private String actualSliceBatchNo;
    private String batchNo;
    private String internalModelCode;
    private String internalItemCode;
    private String customerProductBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String productSize;
    private Integer stockQty;
    private Integer availableQty;
    private Integer lockedQty;
    private String qualityStatus;
    private String warehouseCode;
    private String warehouseName;
    private String locationCode;
    private String locationName;
    private String inboundNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inboundTime;
    private String lockStatus;
    private String lockName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lockTime;
    private String cancelName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelTime;
    private Integer actualShipQty;
    private String shippingQualityNo;
    private String shippingInspectorName;
    private String shippingInspectionResult;
    private String shippingInspectionRemark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shippingInspectionTime;
    private String shippingPackageName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shippingPackageTime;
    private String shippingPackageRemark;
    private String shippedName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime shippedTime;
    private String remark;
    private Long tenantId;
}
