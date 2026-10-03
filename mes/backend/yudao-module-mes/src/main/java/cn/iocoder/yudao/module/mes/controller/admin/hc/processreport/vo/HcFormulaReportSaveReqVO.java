package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 配料报工提交 Request VO")
@Data
public class HcFormulaReportSaveReqVO {

    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @Schema(description = "生产批次号")
    private String batchNo;

    @Schema(description = "报工业务日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @Schema(description = "实际开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime startTime;

    @Schema(description = "实际结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    @Schema(description = "配料单号")
    private String batchingNo;

    @Schema(description = "投料批次号")
    private String feedBatchNo;

    @Schema(description = "配方ID")
    private Long recipeId;

    @Schema(description = "配方编码")
    private String recipeCode;

    @Schema(description = "配方名称")
    private String recipeName;

    @Schema(description = "良品量")
    @DecimalMin(value = "0", message = "良品量不能为负数")
    private BigDecimal goodQty;

    @Schema(description = "不良量")
    @DecimalMin(value = "0", message = "不良量不能为负数")
    private BigDecimal scrapQty;

    @Schema(description = "实投数量")
    @DecimalMin(value = "0", message = "实投数量不能为负数")
    private BigDecimal feedQty;

    @Schema(description = "搅拌开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime stirStartTime;

    @Schema(description = "搅拌结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime stirEndTime;

    @Schema(description = "记录人")
    private String recorderName;

    @Schema(description = "记录时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime recorderTime;

    @Schema(description = "确认人")
    private String confirmerName;

    @Schema(description = "确认时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmerTime;

    @Schema(description = "搅拌机台")
    private Long mixerEquipmentId;
    private String mixerEquipmentCode;
    private String mixerEquipmentName;

    @Schema(description = "泡发机台")
    private Long foamingEquipmentId;
    private String foamingEquipmentCode;
    private String foamingEquipmentName;

    @Schema(description = "粘度(mPa.s)")
    @DecimalMin(value = "0", message = "粘度不能为负数")
    private BigDecimal viscosity;

    @Schema(description = "浆料温度(℃)")
    private BigDecimal slurryTemperature;

    @Schema(description = "滤网批号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "滤网批号不能为空")
    @Size(max = 64, message = "滤网批号长度不能超过64个字符")
    private String filterBatchNo;

    @Schema(description = "投料重量(kg)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "投料重量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "投料重量必须大于0")
    private BigDecimal inputWeight;

    @Schema(description = "配料罐罐号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配料罐罐号不能为空")
    @Size(max = 64, message = "配料罐罐号长度不能超过64个字符")
    private String batchingTankNo;

    @Schema(description = "脱泡罐罐号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "脱泡罐罐号不能为空")
    @Size(max = 64, message = "脱泡罐罐号长度不能超过64个字符")
    private String defoamingTankNo;

    @Schema(description = "人工工时")
    @DecimalMin(value = "0", message = "人工工时不能为负数")
    private BigDecimal laborHours;

    @Schema(description = "报工类型")
    private String reportType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "不良原因编码")
    private String scrapReason;

    @Schema(description = "不良明细")
    @Valid
    private List<HcFormulaReportDefectReqVO> defects;
}
