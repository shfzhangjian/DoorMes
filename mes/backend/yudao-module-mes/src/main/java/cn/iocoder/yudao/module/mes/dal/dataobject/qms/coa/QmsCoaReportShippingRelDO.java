package cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_qms_coa_report_shipping_rel")
@KeySequence("mes_qms_coa_report_shipping_rel_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCoaReportShippingRelDO extends BaseDO {

    @TableId
    private Long id;
    private Long reportId;
    private String coaNo;
    private Integer revisionNo;
    private Long shippingNoticeId;
    private String shippingNoticeNo;
    private Long shippingNoticeItemId;
    private Long finishedStockId;
    private String stockNo;
    private String outerBoxNo;
    private String innerUnitNo;
    private String sliceBatchNo;
    private String actualSliceBatchNo;
    private String productionBatchNo;
    private String customerBatchNo;
    private String customerModelCode;
    private String materialCode;
    private String modelCode;
    private BigDecimal shippingQuantity;
    private String shippingUnit;
    private String qualityStatus;
    private Long oqcOrderId;
    private String oqcStatus;
    private String shippingInspectionResult;
    private Long tenantId;
}
