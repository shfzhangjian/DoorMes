package cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 湿法导布研发样品消耗登记 Request VO")
@Data
public class HcGuideClothRndConsumeSaveReqVO {

    @Schema(description = "导布当前记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "导布当前记录不能为空")
    private Long guideClothRecordId;

    @Schema(description = "本次研发消耗是否同时更换导布")
    private Boolean guideClothChanged;

    @Schema(description = "更换导布时选择的湿法导布耗材领用台账ID；选择更换时必填")
    private Long guideClothLedgerId;

    @Schema(description = "更换后的新导布批号；选择更换时必填")
    private String guideClothNewBatchNo;

    @Schema(description = "导布更换原因；选择更换时必填")
    private String guideClothReplaceReason;

    @Schema(description = "研发产品型号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品型号不能为空")
    private String productModelCode;

    @Schema(description = "研发产品料号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品料号不能为空")
    private String productMaterialCode;

    @Schema(description = "研发产品批次", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品批次不能为空")
    private String productBatchNo;

    @Schema(description = "PET型号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "PET型号不能为空")
    private String petModel;

    @Schema(description = "选择的湿法PET耗材领用台账ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择湿法PET耗材领用台账")
    private Long petLedgerId;

    @Schema(description = "PET批号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "PET批号不能为空")
    private String petBatchNo;

    @Schema(description = "湿法投入kg", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "湿法投入kg不能为空")
    @DecimalMin(value = "0.001", message = "湿法投入kg必须大于0")
    private BigDecimal wetInputKg;

    @Schema(description = "湿法产出m", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "湿法产出m不能为空")
    @DecimalMin(value = "0", message = "湿法产出m不能小于0")
    private BigDecimal wetOutputMeter;

    @Schema(description = "消耗时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @NotNull(message = "消耗时间不能为空或格式不正确")
    private LocalDateTime consumeTime;

    @Schema(description = "登记说明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登记说明不能为空")
    private String remark;
}
