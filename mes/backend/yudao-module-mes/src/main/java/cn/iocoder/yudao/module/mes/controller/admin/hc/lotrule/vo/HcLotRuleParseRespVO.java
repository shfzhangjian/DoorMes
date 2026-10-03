package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则解析 Response VO")
@Data
public class HcLotRuleParseRespVO {

    @Schema(description = "原始批号")
    private String lotNo;

    @Schema(description = "解析结果")
    private Map<String, String> segmentValues;
}
