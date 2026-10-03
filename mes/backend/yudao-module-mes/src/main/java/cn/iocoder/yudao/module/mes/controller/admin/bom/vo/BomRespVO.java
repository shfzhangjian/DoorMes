// 完整路径: cn.iocoder.yudao.module.mes.controller.admin.bom.vo.BomRespVO
package cn.iocoder.yudao.module.mes.controller.admin.bom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 工艺BOM主表 Response VO")
@Data
@ExcelIgnoreUnannotated
public class BomRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "29476")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "所属物料ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "12928")
    @ExcelProperty("所属物料ID")
    private Long productId;

    @Schema(description = "父件物料编码", example = "M001")
    @ExcelProperty("父件物料编码")
    private String productCode;

    @Schema(description = "父件物料名称", example = "成品A")
    @ExcelProperty("父件物料名称")
    private String productName;

    @Schema(description = "父件规格型号", example = "10*10")
    @ExcelProperty("父件规格型号")
    private String productSpec;

    @Schema(description = "父件单位", example = "个")
    @ExcelProperty("父件单位")
    private String productUnit;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("版本号")
    private String version;

    @Schema(description = "是否当前版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("是否当前版本")
    private Boolean active;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "基准产出数量")
    @ExcelProperty("基准产出数量")
    private BigDecimal baseQuantity;

    @Schema(description = "基准产出单位")
    @ExcelProperty("基准产出单位")
    private String baseUnit;

}
