package cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工作中心 Select Option Response VO")
@Data
public class HcWorkCenterSelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "工作中心编码")
    private String code;

    @Schema(description = "工序名称，兼容历史字段 process_stage")
    private String processStage;

    @Schema(description = "标准工序ID")
    private Long processId;

    @Schema(description = "标准工序编码")
    private String processCode;

    @Schema(description = "标准工序名称")
    private String processName;

    @Schema(description = "产线编码")
    private String lineCode;

    @Schema(description = "产线名称")
    private String lineName;

    @Schema(description = "产线短码")
    private String lineShortCode;

    @Schema(description = "批次号产线码")
    private String batchLineCode;

    @Schema(description = "状态")
    private Integer status;

}
