package cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - OCAP模板 Simple Response VO")
@Data
public class HcOcapTemplateSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "OCAP编码")
    private String ocapCode;

    @Schema(description = "OCAP名称")
    private String ocapName;

    @Schema(description = "状态")
    private String status;

}