package cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 裁切研发样品备件消耗登记 Request VO")
@Data
public class HcCutRoundSpareRndConsumeSaveReqVO {

    @Schema(description = "裁切设备 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "裁切设备不能为空")
    private Long equipmentId;

    @Schema(description = "产品型号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品型号不能为空")
    private String modelCode;

    @Schema(description = "垫型：BLACK_PAD/WHITE_PAD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择垫型")
    @jakarta.validation.constraints.Pattern(regexp = "BLACK_PAD|WHITE_PAD", message = "垫型仅支持黑垫或白垫")
    private String padType;

    @Schema(description = "产品批次", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品批次不能为空")
    private String productionBatchNo;

    @Schema(description = "裁切尺寸（775/740）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "裁切尺寸不能为空")
    private String cutSizeMm;

    @Schema(description = "裁切投入片数", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "裁切投入片数不能为空")
    @Min(value = 1, message = "裁切投入片数必须大于 0")
    private Integer cutInputPcs;

    @Schema(description = "裁切产出片数", requiredMode = Schema.RequiredMode.REQUIRED, example = "98")
    @NotNull(message = "裁切产出片数不能为空")
    @Min(value = 0, message = "裁切产出片数不能小于 0")
    private Integer cutOutputPcs;

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
