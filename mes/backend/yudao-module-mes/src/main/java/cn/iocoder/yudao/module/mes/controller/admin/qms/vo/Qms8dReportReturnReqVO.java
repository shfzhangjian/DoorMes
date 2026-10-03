package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 8D报告退回 Request VO")
@Data
public class Qms8dReportReturnReqVO {

    @Schema(description = "主键ID")
    @NotNull(message = "8D报告ID不能为空")
    private Long id;

    @Schema(description = "目标阶段")
    private String targetStep;

    @Schema(description = "退回意见")
    @NotBlank(message = "退回意见不能为空")
    private String opinion;
}
