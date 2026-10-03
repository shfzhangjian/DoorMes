package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 磨皮报工保存 Request VO")
@Data
public class HcRoughReportSaveReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    private String batchNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;

    @DecimalMin(value = "0", message = "投入米数不能为负数")
    private BigDecimal inputLength;

    @DecimalMin(value = "0", message = "报工量不能为负数")
    private BigDecimal reportQty;

    @DecimalMin(value = "0", message = "一次加工米数不能为负数")
    private BigDecimal firstProcessLength;

    @DecimalMin(value = "0", message = "一次加工损耗不能为负数")
    private BigDecimal firstLossLength;

    @DecimalMin(value = "0", message = "一次产出米数不能为负数")
    private BigDecimal firstOutputLength;

    @DecimalMin(value = "0", message = "一次NAP留样不能为负数")
    private BigDecimal firstNapSampleLength;

    @DecimalMin(value = "0", message = "二次加工米数不能为负数")
    private BigDecimal secondProcessLength;

    @DecimalMin(value = "0", message = "二次加工损耗不能为负数")
    private BigDecimal secondLossLength;

    @DecimalMin(value = "0", message = "二次产出米数不能为负数")
    private BigDecimal secondOutputLength;

    @DecimalMin(value = "0", message = "二次NAP留样不能为负数")
    private BigDecimal secondNapSampleLength;

    private String lastFirstSandpaperBatchNo;
    private BigDecimal lastFirstSandpaperLife;
    private BigDecimal lastFirstSandpaperLifeDays;
    private String lastSecondSandpaperBatchNo;
    private BigDecimal lastSecondSandpaperLife;
    private BigDecimal lastSecondSandpaperLifeDays;
    private String remark;
    private String recorderName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime recorderTime;

    private String confirmerName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmerTime;

    @Schema(description = "磨皮前端过程快照 JSON")
    private String extraJson;
}
