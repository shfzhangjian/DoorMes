package cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 裁切备件状态分页 Request VO")
@Data
public class HcCutRoundSparePageReqVO extends PageParam {

    @Schema(description = "设备编号")
    private String equipmentCode;

    @Schema(description = "设备名称")
    private String equipmentName;

    @Schema(description = "备件类型")
    private String spareType;

    @Schema(description = "料号")
    private String materialCode;

    @Schema(description = "批号/编码")
    private String batchNo;

    @Schema(description = "预警标记")
    private Integer warningFlag;

    @Schema(description = "状态")
    private String status;
}
