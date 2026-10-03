package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 压槽扫码前连续作业加检校验 Response VO")
@Data
public class HcPressSlotScanGateRespVO {

    @Schema(description = "是否允许继续扫码确认")
    private Boolean allowScan;

    @Schema(description = "非阻断提示文案")
    private String warningMessage;

    @Schema(description = "最近一次首检/过程加检后已扫码确认片数")
    private Long confirmedAfterInspectionCount;

    @Schema(description = "提醒阈值片数")
    private Integer warningThresholdCount;

    @Schema(description = "BOM配置加检提醒片数（保留原字段名兼容客户端，不再按片数阻断）")
    private Integer blockThresholdCount;

}
