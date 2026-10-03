package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 物料主数据 Simple Response VO")
@Data
public class HcMaterialSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "物料状态")
    private Integer materialStatus;

}