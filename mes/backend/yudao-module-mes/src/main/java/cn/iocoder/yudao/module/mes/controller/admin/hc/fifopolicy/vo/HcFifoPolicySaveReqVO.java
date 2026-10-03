package cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 先进先出策略新增/修改 Request VO")
@Data
public class HcFifoPolicySaveReqVO {

    @Schema(description = "策略编码")
    @NotBlank(message = "策略编码不能为空")
    private String policyCode;

    @Schema(description = "策略名称")
    @NotBlank(message = "策略名称不能为空")
    private String policyName;

    @Schema(description = "仓库编码")
    @NotBlank(message = "仓库编码不能为空")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;

    @Schema(description = "货主编码")
    private String ownerCode;

    @Schema(description = "适用范围")
    @NotBlank(message = "适用范围不能为空")
    private String matchScope;

    @Schema(description = "出库规则")
    @NotBlank(message = "出库规则不能为空")
    private String issueRule;

    @Schema(description = "优先字段")
    @NotBlank(message = "优先字段不能为空")
    private String priorityFields;

    @Schema(description = "状态")
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "主键ID")
    private Long id;

}