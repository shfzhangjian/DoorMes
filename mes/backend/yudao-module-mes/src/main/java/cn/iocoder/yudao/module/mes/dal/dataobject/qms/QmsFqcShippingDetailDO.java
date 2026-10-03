package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

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

@TableName("mes_qms_fqc_shipping_detail")
@KeySequence("mes_qms_fqc_shipping_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcShippingDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long fqcId;
    private String fqcNo;
    private Long shippingNoticeId;
    private String shippingNoticeNo;
    private Long shippingNoticeItemId;
    private Long shippingPickItemId;
    private Long finishedStockId;
    private String stockNo;
    private String actualSliceBatchNo;
    private String sliceBatchNo;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String erpOrderNo;
    private String customerProductBatchNo;
    private String packageSliceNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String internalItemCode;
    private String productSize;
    private Integer shippingQty;
    private String rowJudgment;
    private String defectCode;
    private String defectName;
    private String ngReason;
    private String alignmentStatus;
    private String mismatchReason;
    private Long inspectorId;
    private String inspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;
    private String remark;
    private Boolean recheckDetailFlag;
    private Long tenantId;
}
