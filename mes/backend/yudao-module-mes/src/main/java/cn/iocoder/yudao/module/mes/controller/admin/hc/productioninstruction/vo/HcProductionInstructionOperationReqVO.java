package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工序获取生产指令 Request VO")
@Data
public class HcProductionInstructionOperationReqVO {

    @Schema(description = "计划工序 ID")
    private Long planOperationId;

    @Schema(description = "计划 ID")
    private Long planId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "标准工序编码")
    private String processCode;

    @Schema(description = "标准工序名称")
    private String processName;

    @Schema(description = "计划工序编码")
    private String operationCode;

    @Schema(description = "计划工序名称")
    private String operationName;

    @Schema(description = "分段批号")
    private String segmentBatchNo;

    @Schema(description = "指令类型")
    private String instructionType;

    @Schema(description = "执行状态")
    private String executeStatus;

    @Schema(description = "是否包含已确认指令")
    private Boolean includeConfirmed;

}
