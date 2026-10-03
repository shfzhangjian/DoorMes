package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 分切报工任务列表 Response VO")
@Data
public class HcSlittingReportTaskRespVO {

    private String id;
    private Long planId;
    private Long planOperationId;
    private String planNo;
    private String erpOrderNo;
    private String planType;
    private String product;
    private String materialCode;
    private String productName;
    private String motherMaterialCode;
    private String motherMaterialName;
    private String spec;
    private String modelCode;
    private String motherModelCode;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionEndDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate productionDate;

    private String batchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String process;
    private Long workCenterId;
    private String workCenterName;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private BigDecimal planQty;
    private String uom;
    private BigDecimal goodQty;
    private BigDecimal scrapQty;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private String requirements;
    private String recorderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;

    private String confirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;

    private String reportRemark;
    private String extraJson;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private BigDecimal availableSourceLength;
    private Integer confirmedSourceCount;
}
