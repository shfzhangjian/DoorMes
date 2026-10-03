package cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 货主新增/修改 Request VO")
@Data
public class HcOwnerSaveReqVO {

    @Schema(description = "货主编码")
    @NotBlank(message = "货主编码不能为空")
    private String ownerCode;

    @Schema(description = "货主名称")
    @NotBlank(message = "货主名称不能为空")
    private String ownerName;

    @Schema(description = "货主类型")
    @NotBlank(message = "货主类型不能为空")
    private String ownerType;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "状态")
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "主键ID")
    private Long id;

}