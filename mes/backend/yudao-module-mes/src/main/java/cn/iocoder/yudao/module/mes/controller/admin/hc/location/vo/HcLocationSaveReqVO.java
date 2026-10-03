package cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 库位新增/修改 Request VO")
@Data
public class HcLocationSaveReqVO {

    @Schema(description = "库位编码")
    @NotBlank(message = "库位编码不能为空")
    private String locationCode;

    @Schema(description = "库位名称")
    @NotBlank(message = "库位名称不能为空")
    private String locationName;

    @Schema(description = "仓库编码")
    @NotBlank(message = "仓库编码不能为空")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;

    @Schema(description = "库位类型")
    @NotBlank(message = "库位类型不能为空")
    private String locationType;

    @Schema(description = "是否允许混批")
    @NotNull(message = "是否允许混批不能为空")
    private Boolean mixBatchFlag;

    @Schema(description = "是否允许混型号")
    @NotNull(message = "是否允许混型号不能为空")
    private Boolean mixModelFlag;

    @Schema(description = "状态")
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "主键ID")
    private Long id;

}