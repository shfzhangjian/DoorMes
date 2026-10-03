package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶2报工异常类别修正 Request VO")
@Data
public class HcAdhesive2AbnormalCategoryCorrectReqVO {

    @NotNull(message = "报工记录ID不能为空")
    private Long id;

    @NotBlank(message = "异常类别不能为空")
    private String category;

    @NotBlank(message = "修正原因不能为空")
    private String reason;
}
