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

@Schema(description = "管理后台 - 粘胶胶板领用保存 Request VO")
@Data
public class HcAdhesiveGlueBoardUsageSaveReqVO {

    private Long id;

    private Long planId;

    private Long planOperationId;

    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String operationCode;
    private String operationName;
    private Long glueBoardStockId;

    @NotBlank(message = "胶板料号不能为空")
    private String glueBoardMaterialCode;

    @NotBlank(message = "胶板批号不能为空")
    private String glueBoardBatchNo;

    @DecimalMin(value = "0", message = "领用起位置不能为负数")
    private BigDecimal receiveStartPosition;

    @DecimalMin(value = "0", message = "领用长度不能为负数")
    private BigDecimal receiveLength;

    @DecimalMin(value = "0", message = "领用数量不能为负数")
    private BigDecimal receiveCount;

    private String stockMeasureMode;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    private Long recorderId;
    private String recorderName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime recorderTime;
}
