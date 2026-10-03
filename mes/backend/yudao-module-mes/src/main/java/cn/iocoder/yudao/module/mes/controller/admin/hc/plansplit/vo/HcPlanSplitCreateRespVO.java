package cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HC 拆批创建 Response VO")
@Data
public class HcPlanSplitCreateRespVO {

    @Schema(description = "拆批单ID")
    private Long splitOrderId;

    @Schema(description = "新计划ID")
    private Long newPlanId;

    @Schema(description = "新计划号")
    private String newPlanNo;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "提示")
    private String message;

}
