package cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 工序耗材使用更换流水分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcProcessMaterialLifeEventPageReqVO extends PageParam {

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "状态ID")
    private Long stateId;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "耗材类型")
    private String consumableType;

    @Schema(description = "耗材批号")
    private String batchNo;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "计划编号")
    private String planNo;

    @Schema(description = "计划对应母批号")
    private String motherBatchNo;
}
