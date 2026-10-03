package cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 导布更换记录分页 Request VO")
@Data
public class HcGuideClothRecordPageReqVO extends PageParam {

    @Schema(description = "产线名")
    private String lineName;

    @Schema(description = "产线编号")
    private String lineCode;

    @Schema(description = "计划号")
    private String replacePlanNo;

    @Schema(description = "PET批号")
    private String petBatchNo;

    @Schema(description = "导布批号")
    private String guideClothBatchNo;

    @Schema(description = "PET型号")
    private String petModel;

    @Schema(description = "当前标记")
    private Integer currentFlag;
}
