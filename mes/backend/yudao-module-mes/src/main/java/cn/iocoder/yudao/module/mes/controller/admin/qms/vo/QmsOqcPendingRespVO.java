package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - OQC待检发货通知 Response VO")
@Data
public class QmsOqcPendingRespVO {

    private String productType;
    private Long shippingNoticeId;
    private Long shippingNoticeItemId;
    private String shippingNo;
    private String noticeNo;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String materialCode;
    private String materialName;
    private String specification;
    private String modelCode;
    private String productSize;
    private String batchNo;
    private String customerBatchNo;
    private BigDecimal shippingQty;
    private BigDecimal shippingPieceQty;
    private Long existingOqcId;
    private String existingOqcNo;
    private String existingStatus;
}
