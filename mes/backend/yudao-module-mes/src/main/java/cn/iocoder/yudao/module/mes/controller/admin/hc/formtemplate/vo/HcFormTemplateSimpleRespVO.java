package cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 表单模板 Simple Response VO")
@Data
public class HcFormTemplateSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "状态")
    private String status;

}