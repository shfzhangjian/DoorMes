package cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 型号编码规则测试生成 Response VO")
@Data
public class HcModelRuleGenerateRespVO {

    @Schema(description = "生成编码")
    private String generatedCode;
}
