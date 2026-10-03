package cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备台账 Select Option Response VO")
@Data
public class HcEquipmentSelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "设备编码")
    private String code;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "工作中心编码")
    private String workCenterCode;

    @Schema(description = "工作中心名称")
    private String workCenterName;

    @Schema(description = "适用垫型")
    private String applicablePadType;

    @Schema(description = "适用垫型名称")
    private String applicablePadTypeName;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "运行状态")
    private String workStatus;

    @Schema(description = "当前工序编号")
    private String currentOperationCode;

    @Schema(description = "当前工序名称")
    private String currentOperationName;

}
