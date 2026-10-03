package cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备台账 Simple Response VO")
@Data
public class HcEquipmentSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "状态")
    private Integer status;

}