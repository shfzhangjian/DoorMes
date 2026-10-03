package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶胶板边库库存 Response VO")
@Data
public class HcAdhesiveGlueBoardStockRespVO {

    private Long id;
    private Long toolingLedgerId;
    private String accessoryCategory;
    private String accessoryCategoryName;
    private String glueBoardMaterialCode;
    private String glueBoardMaterialName;
    private String glueBoardModel;
    private String glueBoardBatchNo;
    private String sourceWarehouseCode;
    private String sourceWarehouseName;
    private String edgeWarehouseCode;
    private String edgeWarehouseName;
    private BigDecimal receiveStartPosition;
    private BigDecimal receiveLength;
    private String stockMeasureMode;
    private BigDecimal receiveCount;
    private BigDecimal usedLength;
    private BigDecimal usedCount;
    private BigDecimal availableStartPosition;
    private BigDecimal availableLength;
    private BigDecimal availableCount;
    private BigDecimal lossLength;
    private BigDecimal lossCount;
    private String lifetimeMode;
    private BigDecimal lifetimeLimitLength;
    private BigDecimal lifetimeLimitCount;
    private BigDecimal lifeUsedLength;
    private BigDecimal lifeUsedCount;
    private String stockStatus;
    private String qualityStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionSubmitTime;

    private Long latestInspectionId;
    private String latestInspectionNo;
    private String latestInspectionResult;

    private Long receiverId;
    private String receiverName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate receiveDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receiveTime;

    private Integer printCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime printTime;

    private String erpTransferNo;
    private BigDecimal transferQty;
    private String transferUnit;
    private BigDecimal unpackQty;
    private String unpackUnit;
    private String erpTransferStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime erpTransferTime;

    private String erpTransferMessage;
    private String remark;
    private String extraJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
