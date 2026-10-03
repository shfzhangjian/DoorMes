package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 粘胶胶板边库领料保存 Request VO")
@Data
public class HcAdhesiveGlueBoardStockSaveReqVO {

    private String accessoryCategory;

    private String accessoryCategoryName;

    @NotBlank(message = "辅料料号不能为空")
    private String glueBoardMaterialCode;

    private String glueBoardMaterialName;

    private String glueBoardModel;

    @NotBlank(message = "辅料批号不能为空")
    private String glueBoardBatchNo;

    private String sourceWarehouseCode;
    private String sourceWarehouseName;
    private String edgeWarehouseCode;
    private String edgeWarehouseName;

    @DecimalMin(value = "0", message = "可用起位置不能为负数")
    private BigDecimal availableStartPosition;

    private BigDecimal availableLength;
    private BigDecimal availableCount;

    @DecimalMin(value = "0", message = "领料起位置不能为负数")
    private BigDecimal receiveStartPosition;

    private BigDecimal receiveLength;
    private BigDecimal receiveCount;
    private String stockMeasureMode;
    private String lifetimeMode;
    private BigDecimal lifetimeLimitLength;
    private BigDecimal lifetimeLimitCount;
    private BigDecimal lifeUsedLength;
    private BigDecimal lifeUsedCount;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate receiveDate;

    private Long receiverId;
    private String receiverName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime receiveTime;

    @NotBlank(message = "ERP移库单号不能为空")
    private String erpTransferNo;

    @DecimalMin(value = "0.001", message = "移库数量必须大于0")
    private BigDecimal transferQty;

    private String transferUnit;

    @DecimalMin(value = "0.001", message = "拆包量必须大于0")
    private BigDecimal unpackQty;

    private String unpackUnit;

    private String remark;
}
