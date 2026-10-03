package cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备台账分页 Request VO")
@Data
public class HcEquipmentPageReqVO extends PageParam {

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "设备名称")
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

    @Schema(description = "资产编号")
    private String assetNo;

    @Schema(description = "是否启用点检")
    private Boolean enableQcChecklist;

    @Schema(description = "是否启用清洁点检")
    private Boolean enableCleanChecklist;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "运行状态")
    private String workStatus;

    @Schema(description = "当前计划号")
    private String currentPlanNo;

    @Schema(description = "当前工序编号")
    private String currentOperationCode;

    @Schema(description = "当前工序名称")
    private String currentOperationName;

    @Schema(description = "备注")
    private String remark;

}
