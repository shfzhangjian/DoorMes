package cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 货主 Select Option Response VO")
@Data
public class HcOwnerSelectOptionRespVO {

    @Schema(description = "选项值")
    private Long value;

    @Schema(description = "选项标签")
    private String label;

    @Schema(description = "货主编码")
    private String code;

    @Schema(description = "状态")
    private String status;

}