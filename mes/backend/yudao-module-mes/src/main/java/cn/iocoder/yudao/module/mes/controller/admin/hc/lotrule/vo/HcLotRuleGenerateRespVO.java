package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则生成 Response VO")
@Data
public class HcLotRuleGenerateRespVO {

    @Schema(description = "生成批号")
    private String lotNo;

    @Schema(description = "当前流水号")
    private Integer currentSeq;

    @Schema(description = "下一流水号")
    private Integer nextSeq;

    @Schema(description = "解析后的分段值")
    private Map<String, String> segmentValues;
}
