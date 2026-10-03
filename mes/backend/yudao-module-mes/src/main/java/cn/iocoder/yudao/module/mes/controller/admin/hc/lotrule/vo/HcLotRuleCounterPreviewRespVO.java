package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 批次实例台账下一批号预览 Response VO")
@Data
public class HcLotRuleCounterPreviewRespVO {

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "计数器类型")
    private String counterType;

    @Schema(description = "业务维度键")
    private String bizDimensionKey;

    @Schema(description = "计数键")
    private String counterKey;

    @Schema(description = "重置键")
    private String resetKey;

    @Schema(description = "当前已使用计数")
    private Integer currentSeq;

    @Schema(description = "下一次消耗流水")
    private Integer nextSeq;

    @Schema(description = "已生成最大流水")
    private Integer maxUsedSeq;

    @Schema(description = "下一母批号")
    private String nextLotNo;

    @Schema(description = "提示信息")
    private String warning;
}
