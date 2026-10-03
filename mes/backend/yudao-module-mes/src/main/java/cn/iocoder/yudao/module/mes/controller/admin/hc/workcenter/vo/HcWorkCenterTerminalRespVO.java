package cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工位 IP 工作中心解析 Response VO")
@Data
public class HcWorkCenterTerminalRespVO {

    @Schema(description = "工位 IP")
    private String ip;

    @Schema(description = "是否匹配到工作中心")
    private Boolean matched;

    @Schema(description = "工作中心 ID")
    private Long workCenterId;

    @Schema(description = "工作中心编码")
    private String workCenterCode;

    @Schema(description = "工作中心名称")
    private String workCenterName;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "产线编码")
    private String lineCode;

    @Schema(description = "产线名称")
    private String lineName;

    @Schema(description = "产线短码")
    private String lineShortCode;

    @Schema(description = "批次号产线码")
    private String batchLineCode;

    @Schema(description = "绑定工位 IP")
    private String terminalIps;

}
