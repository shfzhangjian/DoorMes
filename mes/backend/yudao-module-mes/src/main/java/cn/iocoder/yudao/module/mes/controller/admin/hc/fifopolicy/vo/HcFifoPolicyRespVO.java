package cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 先进先出策略 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcFifoPolicyRespVO {

    @Schema(description = "策略编码")
    @ExcelProperty("策略编码")
    private String policyCode;

    @Schema(description = "策略名称")
    @ExcelProperty("策略名称")
    private String policyName;

    @Schema(description = "仓库编码")
    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "货主编码")
    @ExcelProperty("货主编码")
    private String ownerCode;

    @Schema(description = "适用范围")
    @ExcelProperty("适用范围")
    private String matchScope;

    @Schema(description = "出库规则")
    @ExcelProperty("出库规则")
    private String issueRule;

    @Schema(description = "优先字段")
    @ExcelProperty("优先字段")
    private String priorityFields;

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