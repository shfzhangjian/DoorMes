package cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo;

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

@Schema(description = "管理后台 - 压槽研发样品备件消耗登记 Request VO")
@Data
public class HcPressSlotSpareRndConsumeSaveReqVO {

    @Schema(description = "压槽设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "压槽设备不能为空")
    private Long equipmentId;

    @Schema(description = "产品型号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品型号不能为空")
    private String modelCode;

    @Schema(description = "产品批次", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "产品批次不能为空")
    private String productionBatchNo;

    @Schema(description = "压槽投入片数", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "压槽投入片数不能为空")
    @Min(value = 1, message = "压槽投入片数必须大于 0")
    private Integer pressSlotInputPcs;

    @Schema(description = "压槽产出片数", requiredMode = Schema.RequiredMode.REQUIRED, example = "98")
    @NotNull(message = "压槽产出片数不能为空")
    @Min(value = 0, message = "压槽产出片数不能小于 0")
    private Integer pressSlotOutputPcs;

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
