package cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 设备台账新增/修改 Request VO")
@Data
public class HcEquipmentSaveReqVO {

    @Schema(description = "设备编码")
    @NotBlank(message = "设备编码不能为空")
    private String equipmentCode;

    @Schema(description = "设备名称")
    @NotBlank(message = "设备名称不能为空")
    private String equipmentName;

    @Schema(description = "所属工作中心ID")
    private Long workCenterId;

    @Schema(description = "所属工作中心编码")
    private String workCenterCode;

    @Schema(description = "所属工作中心名称")
    private String workCenterName;

    @Schema(description = "设备类型")
    private String equipmentType;

    @Schema(description = "适用垫型")
    private String applicablePadType;

    @Schema(description = "适用垫型名称")
    private String applicablePadTypeName;

    @Schema(description = "资产编号")
    private String assetNo;

    @Schema(description = "是否启用点检")
    private Boolean enableQcChecklist;

    @Schema(description = "是否启用清洁点检")
    private Boolean enableCleanChecklist;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "主键ID")
    private Long id;

}
