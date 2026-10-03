package cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 工序耗材在用状态分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcProcessMaterialLifeStatePageReqVO extends PageParam {

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "设备编码")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "耗材类型")
    private String consumableType;

    @Schema(description = "耗材批号")
    private String batchNo;

    @Schema(description = "预警标识")
    private Integer warningFlag;

    @Schema(description = "状态")
    private String status;
}
