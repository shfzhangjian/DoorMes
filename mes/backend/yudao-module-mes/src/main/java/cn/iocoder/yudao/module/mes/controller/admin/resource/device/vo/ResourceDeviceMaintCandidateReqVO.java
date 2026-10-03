package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备本月保养追加候选 Request VO")
@Data
public class ResourceDeviceMaintCandidateReqVO {

    @Schema(description = "设备分类ID")
    private Long categoryId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "是否包含未到期设备")
    private Boolean includeNormal;

}
