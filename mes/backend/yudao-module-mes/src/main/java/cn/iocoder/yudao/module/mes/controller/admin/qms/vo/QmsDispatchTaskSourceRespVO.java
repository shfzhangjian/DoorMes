package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务复检来源 Response VO")
@Data
public class QmsDispatchTaskSourceRespVO {

    private Long id;
    private String executionNo;
    private String planNo;
    private String workOrderNo;
    private String operationCode;
    private String operationName;
    private String batchNo;
    private String shippingNoticeNo;
    private String receiptNo;
    private String supplierName;
    private LocalDate arrivalDate;
    private LocalDate productionDate;
    private String customerName;
    private String productModel;
    private String materialCode;
    private String materialName;
    private Long materialId;
    private String specification;
    private Long standardId;
    private String standardNo;
    private String standardName;
    private BigDecimal checkQty;
    private BigDecimal okQty;
    private BigDecimal ngQty;
    private String unit;
    private String status;
    private String judgment;
    private String abnormalSummary;
    private String remark;
    private String inspectorName;
    private LocalDateTime inspectionTime;
    private String auditorName;
    private LocalDateTime auditTime;
}
