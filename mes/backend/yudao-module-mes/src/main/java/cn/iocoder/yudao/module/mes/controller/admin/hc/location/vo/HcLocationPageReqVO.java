package cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 库位分页 Request VO")
@Data
public class HcLocationPageReqVO extends PageParam {

    @Schema(description = "库位编码")
    private String locationCode;

    @Schema(description = "库位名称")
    private String locationName;

    @Schema(description = "仓库编码")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "库位类型")
    private String locationType;

    @Schema(description = "是否允许混批")
    private Boolean mixBatchFlag;

    @Schema(description = "是否允许混型号")
    private Boolean mixModelFlag;

    @Schema(description = "状态")
    private String status;

}