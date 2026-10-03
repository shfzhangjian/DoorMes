package cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 型号编码规则 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcModelRuleRespVO {

    @Schema(description = "规则编码")
    @ExcelProperty("规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    @ExcelProperty("规则名称")
    private String ruleName;

    @Schema(description = "规则分类")
    @ExcelProperty("规则分类")
    private String ruleCategory;

    @Schema(description = "适用产品层级")
    @ExcelProperty("适用产品层级")
    private String targetLevel;

    @Schema(description = "规则说明")
    @ExcelProperty("规则说明")
    private String ruleDesc;

    @Schema(description = "当前生效版本")
    @ExcelProperty("当前生效版本")
    private String effectiveVersion;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}