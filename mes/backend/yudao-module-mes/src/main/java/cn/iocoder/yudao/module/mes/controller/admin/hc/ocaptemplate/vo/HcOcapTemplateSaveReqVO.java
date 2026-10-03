package cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - OCAP模板新增/修改 Request VO")
@Data
public class HcOcapTemplateSaveReqVO {

    @Schema(description = "OCAP编码")
    @NotBlank(message = "OCAP编码不能为空")
    private String ocapCode;

    @Schema(description = "OCAP名称")
    @NotBlank(message = "OCAP名称不能为空")
    private String ocapName;

    @Schema(description = "业务工序")
    @NotBlank(message = "业务工序不能为空")
    private String businessStage;

    @Schema(description = "触发项目编码")
    @NotBlank(message = "触发项目编码不能为空")
    private String triggerItemCode;

    @Schema(description = "触发条件")
    @NotBlank(message = "触发条件不能为空")
    private String triggerCondition;

    @Schema(description = "处置步骤")
    @NotBlank(message = "处置步骤不能为空")
    private String actionSteps;

    @Schema(description = "状态")
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "主键ID")
    private Long id;

}