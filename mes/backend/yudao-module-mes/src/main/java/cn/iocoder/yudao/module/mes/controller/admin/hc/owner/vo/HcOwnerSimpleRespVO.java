package cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 货主 Simple Response VO")
@Data
public class HcOwnerSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "货主编码")
    private String ownerCode;

    @Schema(description = "货主名称")
    private String ownerName;

    @Schema(description = "状态")
    private String status;

}