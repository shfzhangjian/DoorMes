package cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 库位 Simple Response VO")
@Data
public class HcLocationSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "库位编码")
    private String locationCode;

    @Schema(description = "库位名称")
    private String locationName;

    @Schema(description = "状态")
    private String status;

}