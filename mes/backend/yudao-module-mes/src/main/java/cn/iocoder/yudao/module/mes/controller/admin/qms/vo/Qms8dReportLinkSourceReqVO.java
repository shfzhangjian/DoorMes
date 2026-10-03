package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 8D报告关联来源 Request VO")
@Data
public class Qms8dReportLinkSourceReqVO {

    @Schema(description = "8D报告ID")
    @NotNull(message = "8D报告ID不能为空")
    private Long id;

    @Schema(description = "来源类型")
    @NotBlank(message = "来源类型不能为空")
    private String relationType;

    @Schema(description = "来源对象ID")
    private Long relatedObjectId;

    @Schema(description = "来源对象单号")
    @NotBlank(message = "来源对象单号不能为空")
    private String relatedObjectNo;

    @Schema(description = "来源对象摘要")
    private String relatedObjectName;

    @Schema(description = "来源对象状态")
    private String relationStatus;

    @Schema(description = "备注")
    private String remark;
}
