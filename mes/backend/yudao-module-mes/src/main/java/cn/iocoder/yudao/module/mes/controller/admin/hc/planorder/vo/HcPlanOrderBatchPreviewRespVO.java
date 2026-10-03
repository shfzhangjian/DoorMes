package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产计划产品批号预览 Response VO")
@Data
public class HcPlanOrderBatchPreviewRespVO {

    @Schema(description = "预览产品批号")
    private String batchNo;

    @Schema(description = "批次规则ID")
    private Long ruleId;

    @Schema(description = "批次规则编码")
    private String ruleCode;

    @Schema(description = "批次规则名称")
    private String ruleName;

    @Schema(description = "批次规则版本")
    private Integer ruleVersion;

    @Schema(description = "当前已使用流水")
    private Integer currentSeq;

    @Schema(description = "下一流水")
    private Integer nextSeq;

    @Schema(description = "已生成最大流水")
    private Integer maxUsedSeq;

    @Schema(description = "已建计划最大流水")
    private Integer maxPlannedSeq;

    @Schema(description = "提示信息")
    private String warning;

}
