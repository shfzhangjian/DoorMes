package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 按设备同步最近已确认磨皮生产记录耗材 Request VO")
@Data
public class HcGrindingProductionRecordConsumableSyncReqVO {

    @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "设备不能为空")
    private Long equipmentId;
}
