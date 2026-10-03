package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 8D报告关闭 Request VO")
@Data
public class Qms8dReportCloseReqVO {

    @Schema(description = "主键ID")
    @NotNull(message = "8D报告ID不能为空")
    private Long id;

    @Schema(description = "验证结果")
    private String validationResult;

    @Schema(description = "标准化说明")
    private String standardizeDesc;

    @Schema(description = "关闭意见")
    private String opinion;
}
