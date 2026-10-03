package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 压槽表单填写记录分页 Request VO")
@Data
public class HcPressSlotFormRecordPageReqVO extends PageParam {

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "压槽片号")
    private String pressSlotSliceNo;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "状态")
    private String status;
}
