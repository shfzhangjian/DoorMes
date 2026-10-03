package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则匹配测试 Request VO")
@Data
public class HcLotRuleMatchReqVO {

    @Schema(description = "业务对象", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "业务对象不能为空")
    private String bizType;

    @Schema(description = "产品分类")
    private String productCategoryCode;

    @Schema(description = "生产类型")
    private String prodType;

    @Schema(description = "生成时机", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "生成时机不能为空")
    private String generationTrigger;

    @Schema(description = "生成粒度", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "生成粒度不能为空")
    private String generationScope;

    @Schema(description = "产品型号")
    private String modelCode;
}
