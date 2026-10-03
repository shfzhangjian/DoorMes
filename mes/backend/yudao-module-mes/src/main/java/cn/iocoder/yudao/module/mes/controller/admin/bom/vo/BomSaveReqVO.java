package cn.iocoder.yudao.module.mes.controller.admin.bom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;
import jakarta.validation.constraints.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.bomitem.BomItemDO;

@Schema(description = "管理后台 - 工艺BOM主表新增/修改 Request VO")
@Data
public class BomSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "29476")
    private Long id;

    @Schema(description = "所属物料ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "12928")
    @NotNull(message = "所属物料不能为空")
    private Long productId;

    @Schema(description = "父件物料编码")
    private String productCode;

    @Schema(description = "父件物料名称")
    private String productName;

    @Schema(description = "父件规格型号")
    private String productSpec;

    @Schema(description = "父件单位")
    private String productUnit;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "版本号不能为空")
    private String version;

    @Schema(description = "是否当前版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否当前版本不能为空")
    private Boolean active;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "工艺BOM子项列表")
    private List<BomItemDO> bomItems;

    @Schema(description = "基准产出数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "基准产出数量不能为空")
    private BigDecimal baseQuantity;

    @Schema(description = "基准产出单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "基准产出单位不能为空")
    private String baseUnit;

}
