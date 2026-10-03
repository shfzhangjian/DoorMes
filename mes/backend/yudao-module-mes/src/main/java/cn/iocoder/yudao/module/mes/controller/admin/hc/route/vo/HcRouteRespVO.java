package cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工艺路线 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcRouteRespVO {

    @Schema(description = "路线编码")
    @ExcelProperty("路线编码")
    private String routeCode;

    @Schema(description = "路线名称")
    @ExcelProperty("路线名称")
    private String routeName;

    @Schema(description = "适用范围")
    @ExcelProperty("适用范围")
    private String applicableScope;

    @Schema(description = "适用物料ID")
    @ExcelProperty("适用物料ID")
    private Long productMaterialId;

    @Schema(description = "适用物料编码")
    @ExcelProperty("适用物料编码")
    private String productMaterialCode;

    @Schema(description = "适用层级")
    @ExcelProperty("适用层级")
    private String productLevel;

    @Schema(description = "版本号")
    @ExcelProperty("版本号")
    private String versionNo;

    @Schema(description = "路线类型")
    @ExcelProperty("路线类型")
    private String routeType;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
