package cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备耗材状态分页 Request VO")
@Data
public class HcEquipmentConsumableStatePageReqVO extends PageParam {

    @Schema(description = "设备编号")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "工序编号")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "耗材类型")
    private String consumableType;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "预警标记")
    private Integer warningFlag;

    @Schema(description = "状态")
    private String status;
}
