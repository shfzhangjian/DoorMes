package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备分类列表 Request VO")
@Data
public class ResourceDeviceCategoryListReqVO {

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "状态")
    private Integer status;

}
