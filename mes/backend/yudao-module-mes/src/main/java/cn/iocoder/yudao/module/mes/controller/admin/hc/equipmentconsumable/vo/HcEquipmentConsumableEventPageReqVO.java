package cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备耗材事件分页 Request VO")
@Data
public class HcEquipmentConsumableEventPageReqVO extends PageParam {

    @Schema(description = "状态ID")
    private Long stateId;

    @Schema(description = "湿法导布当前记录ID")
    private Long guideClothRecordId;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "设备编号")
    private String equipmentCode;

    @Schema(description = "工序编号")
    private String processCode;

    @Schema(description = "耗材类型")
    private String consumableType;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "操作人")
    private String operatorName;
}
