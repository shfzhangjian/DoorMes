package cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 物料编码预生成 Response VO")
@Data
public class HcMaterialCodeGenerateRespVO {

    @Schema(description = "物料编码")
    private String materialCode;
}
