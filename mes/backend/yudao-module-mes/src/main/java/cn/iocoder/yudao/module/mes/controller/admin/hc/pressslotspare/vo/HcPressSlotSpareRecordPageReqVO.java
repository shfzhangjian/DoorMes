package cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 压槽备件流水分页 Request VO")
@Data
public class HcPressSlotSpareRecordPageReqVO extends PageParam {

    @Schema(description = "备件状态ID")
    private Long spareId;

    @Schema(description = "设备ID")
    private Long equipmentId;

    @Schema(description = "设备编号")
    private String equipmentCode;

    @Schema(description = "备件类型")
    private String spareType;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "批号/编码")
    private String batchNo;

    @Schema(description = "操作人")
    private String operatorName;
}
