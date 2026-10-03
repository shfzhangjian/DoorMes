package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备分类 Response VO")
@Data
public class ResourceDeviceCategoryRespVO {

    private Long id;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private Integer status;
    private Integer sort;

}
