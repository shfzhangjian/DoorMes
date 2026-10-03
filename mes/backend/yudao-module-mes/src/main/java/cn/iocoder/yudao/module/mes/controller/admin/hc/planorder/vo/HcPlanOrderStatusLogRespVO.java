package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产计划主表状态操作日志 Response VO")
@Data
public class HcPlanOrderStatusLogRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "生产计划ID")
    private Long planId;

    @Schema(description = "生产计划编号快照")
    private String planNo;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "操作前状态")
    private String fromStatus;

    @Schema(description = "操作后状态")
    private String toStatus;

    @Schema(description = "原因/备注说明")
    private String reasonRemark;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

}
