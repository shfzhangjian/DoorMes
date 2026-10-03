package cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomItemDO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 工艺用料清单新增/修改 Request VO")
@Data
public class HcBomSaveReqVO {

    @Schema(description = "清单编码")
    @NotBlank(message = "清单编码不能为空")
    private String bomCode;

    @Schema(description = "清单名称")
    private String bomName;

    @Schema(description = "产出物料ID")
    @NotNull(message = "产出物料ID不能为空")
    private Long productMaterialId;

    @Schema(description = "产出物料编码")
    @NotBlank(message = "产出物料编码不能为空")
    private String productMaterialCode;

    @Schema(description = "产出物料名称")
    @NotBlank(message = "产出物料名称不能为空")
    private String productMaterialName;

    @Schema(description = "产品型号ID")
    @NotNull(message = "产品型号ID不能为空")
    private Long productModelId;

    @Schema(description = "产品型号编码")
    private String productModelCode;

    @Schema(description = "产品型号名称")
    private String productModelName;

    @Schema(description = "成品规格/尺寸")
    @NotBlank(message = "成品规格/尺寸不能为空")
    private String productSpec;

    @Schema(description = "关联配方ID")
    private Long recipeId;

    @Schema(description = "关联配方编码")
    private String recipeCode;

    @Schema(description = "关联配方名称")
    private String recipeName;

    @Schema(description = "关联工艺路线ID")
    private Long routeId;

    @Schema(description = "关联工艺路线编码")
    private String routeCode;

    @Schema(description = "版本号")
    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    @Schema(description = "清单类型")
    @NotBlank(message = "清单类型不能为空")
    private String bomType;

    @Schema(description = "标准良率")
    private BigDecimal yieldRate;

    @Schema(description = "压槽连续作业加检数")
    @Min(value = 1, message = "压槽连续作业加检数必须大于0")
    private Integer pressSlotContinuousCheckCount;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "清单明细列表")
    private List<HcBomItemDO> bomItems;

}
