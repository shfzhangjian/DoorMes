package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则匹配测试 Response VO")
@Data
public class HcLotRuleMatchRespVO {

    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private Integer versionNo;
    private Integer priority;
    private String message;
}
