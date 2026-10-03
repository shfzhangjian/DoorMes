package cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - BOM Simple Response VO")
@Data
public class HcBomSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "BOM编码")
    private String bomCode;

    @Schema(description = "BOM名称")
    private String bomName;

    @Schema(description = "状态")
    private Integer status;

}