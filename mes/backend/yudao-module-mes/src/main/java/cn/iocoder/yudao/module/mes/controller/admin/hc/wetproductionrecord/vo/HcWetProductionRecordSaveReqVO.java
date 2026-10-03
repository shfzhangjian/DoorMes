package cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 湿法生产记录新增/修改 Request VO")
@Data
public class HcWetProductionRecordSaveReqVO {

    private Long id;

    @Schema(description = "日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "日期不能为空")
    private LocalDate recordDate;

    @Schema(description = "型号")
    @NotBlank(message = "型号不能为空")
    private String modelCode;

    @Schema(description = "料号")
    @NotBlank(message = "料号不能为空")
    private String materialCode;

    @Schema(description = "批次")
    @NotBlank(message = "批次不能为空")
    private String batchNo;

    @Schema(description = "投入(kg)")
    private BigDecimal inputKg;

    @Schema(description = "产出(m)")
    private BigDecimal outputMeter;

    @Schema(description = "PET型号")
    @NotBlank(message = "PET型号不能为空")
    private String petModel;

    @Schema(description = "PET批号")
    @NotBlank(message = "PET批号不能为空")
    private String petBatchNo;

    @Schema(description = "导布批号")
    @NotBlank(message = "导布批号不能为空")
    private String guideClothBatchNo;

    @Schema(description = "导布累计使用次数")
    private Integer guideClothUseCount;

    @Schema(description = "导布更换，Y/N")
    @NotBlank(message = "导布更换不能为空")
    private String guideClothChanged;

    @Schema(description = "更换说明")
    private String changeDesc;

    @Schema(description = "记录人")
    @NotBlank(message = "记录人不能为空")
    private String recorderName;

    @Schema(description = "记录时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @NotNull(message = "记录时间不能为空")
    private LocalDateTime recordTime;

    @Schema(description = "备注")
    private String remark;
}
