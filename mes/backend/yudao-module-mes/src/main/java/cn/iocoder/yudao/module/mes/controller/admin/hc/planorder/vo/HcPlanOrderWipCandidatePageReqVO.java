package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 生产计划挂接半成品候选分页 Request VO")
@Data
public class HcPlanOrderWipCandidatePageReqVO extends PageParam {

    @Schema(description = "目标计划ID")
    private Long targetPlanId;

    @Schema(description = "目标计划号")
    private String targetPlanNo;

    @Schema(description = "目标工序ID")
    private Long targetOperationId;

    @Schema(description = "目标工序顺序")
    private Integer targetOpSeq;

    @Schema(description = "目标工序编码")
    private String targetOpCode;

    @Schema(description = "来源工序顺序")
    private Integer sourceOpSeq;

    @Schema(description = "来源计划号")
    private String sourcePlanNo;

    @Schema(description = "中间品批号")
    private String batchNo;

    @Schema(description = "来源半成品批号")
    private String sourceBatchNo;

    @Schema(description = "母卷批次号")
    private String sourceParentBatchNo;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "型号")
    private String modelNo;

    @Schema(description = "质量状态")
    private String qualityStatus;

    @Schema(description = "关键词：计划号/批号/物料/型号")
    private String keyword;

}
