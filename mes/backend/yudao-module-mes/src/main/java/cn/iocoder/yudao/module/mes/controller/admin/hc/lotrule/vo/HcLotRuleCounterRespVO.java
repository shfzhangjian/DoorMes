package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则流水台账 Response VO")
@Data
public class HcLotRuleCounterRespVO {

    @Schema(description = "主键ID")
    private Long id;

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

    @Schema(description = "业务维度上下文 JSON")
    private String bizDimensionJson;

    @Schema(description = "计数键")
    private String counterKey;

    @Schema(description = "重置键")
    private String resetKey;

    @Schema(description = "当前已使用计数")
    private Integer currentSeq;

    @Schema(description = "当前已使用计数")
    private Integer currentValue;

    @Schema(description = "统计年度")
    private Integer statisticalYear;

    @Schema(description = "下一流水")
    private Integer nextSeq;

    @Schema(description = "下一批号预览")
    private String nextLotNo;

    @Schema(description = "是否已初始化流水")
    private Boolean initialized;

    @Schema(description = "流水提示")
    private String warning;

    @Schema(description = "最后生成批号")
    private String lastLotNo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
