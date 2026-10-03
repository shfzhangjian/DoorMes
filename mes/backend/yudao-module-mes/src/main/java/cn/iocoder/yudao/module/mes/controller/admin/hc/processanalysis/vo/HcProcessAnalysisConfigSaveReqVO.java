package cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 计划排程统计分析方案保存 Request VO")
@Data
public class HcProcessAnalysisConfigSaveReqVO {

    @Schema(description = "方案ID，更新时必填")
    private Long id;

    @Schema(description = "方案名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "方案名称不能为空")
    @Size(max = 100, message = "方案名称不能超过100个字符")
    private String configName;

    @Schema(description = "范围：PRIVATE个人、SHARED租户共享")
    private String scopeType;

    @Schema(description = "是否默认方案")
    private Boolean defaultFlag;

    @Schema(description = "配置JSON版本")
    private Integer configVersion;

    @Schema(description = "分析配置JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分析配置不能为空")
    private String configJson;

    @Schema(description = "方案说明")
    @Size(max = 500, message = "方案说明不能超过500个字符")
    private String remark;

}
