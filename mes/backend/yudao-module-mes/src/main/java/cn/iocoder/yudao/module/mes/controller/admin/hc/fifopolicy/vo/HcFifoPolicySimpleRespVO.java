package cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 先进先出策略 Simple Response VO")
@Data
public class HcFifoPolicySimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "策略编码")
    private String policyCode;

    @Schema(description = "策略名称")
    private String policyName;

    @Schema(description = "状态")
    private String status;

}