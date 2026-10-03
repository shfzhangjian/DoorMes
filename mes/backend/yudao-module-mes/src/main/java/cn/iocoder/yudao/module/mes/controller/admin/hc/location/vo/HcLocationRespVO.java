package cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 库位 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcLocationRespVO {

    @Schema(description = "库位编码")
    @ExcelProperty("库位编码")
    private String locationCode;

    @Schema(description = "库位名称")
    @ExcelProperty("库位名称")
    private String locationName;

    @Schema(description = "仓库编码")
    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "库位类型")
    @ExcelProperty("库位类型")
    private String locationType;

    @Schema(description = "是否允许混批")
    @ExcelProperty("是否允许混批")
    private Boolean mixBatchFlag;

    @Schema(description = "是否允许混型号")
    @ExcelProperty("是否允许混型号")
    private Boolean mixModelFlag;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}