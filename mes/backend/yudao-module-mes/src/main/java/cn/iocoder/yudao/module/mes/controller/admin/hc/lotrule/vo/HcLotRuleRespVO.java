package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcLotRuleRespVO {

    @Schema(description = "规则编码")
    @ExcelProperty("规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    @ExcelProperty("规则名称")
    private String ruleName;

    @Schema(description = "业务对象类型")
    @ExcelProperty("业务对象类型")
    private String bizType;

    @Schema(description = "产品类别编码")
    @ExcelProperty("产品类别编码")
    private String productCategoryCode;

    @Schema(description = "生产类型")
    @ExcelProperty("生产类型")
    private String prodType;

    @Schema(description = "型号匹配方式")
    @ExcelProperty("型号匹配方式")
    private String modelMatchMode;

    @Schema(description = "型号匹配值")
    @ExcelProperty("型号匹配值")
    private String modelMatchValue;

    @Schema(description = "规则优先级")
    @ExcelProperty("规则优先级")
    private Integer priority;

    @Schema(description = "规则版本号")
    @ExcelProperty("规则版本号")
    private Integer versionNo;

    @Schema(description = "流水组编码")
    @ExcelProperty("流水组编码")
    private String counterGroupCode;

    @Schema(description = "默认生成触发点")
    @ExcelProperty("默认生成触发点")
    private String generationTrigger;

    @Schema(description = "默认生成粒度")
    @ExcelProperty("默认生成粒度")
    private String generationScope;

    @Schema(description = "批次数量模型")
    @ExcelProperty("批次数量模型")
    private String batchCardinality;

    @Schema(description = "多批次展示策略")
    @ExcelProperty("多批次展示策略")
    private String displayPolicy;

    @Schema(description = "规则高级配置JSON")
    private String ruleConfigJson;

    @Schema(description = "规则模式")
    @ExcelProperty("规则模式")
    private String ruleMode;

    @Schema(description = "前缀")
    @ExcelProperty("前缀")
    private String prefix;

    @Schema(description = "日期格式")
    @ExcelProperty("日期格式")
    private String dateFormat;

    @Schema(description = "年份编码模式")
    @ExcelProperty("年份编码模式")
    private String yearCodeMode;

    @Schema(description = "月份编码模式")
    @ExcelProperty("月份编码模式")
    private String monthCodeMode;

    @Schema(description = "流水长度")
    @ExcelProperty("流水长度")
    private Integer seqLength;

    @Schema(description = "流水起始值")
    @ExcelProperty("流水起始值")
    private Integer seqStart;

    @Schema(description = "流水步长")
    @ExcelProperty("流水步长")
    private Integer seqStep;

    @Schema(description = "重置周期")
    @ExcelProperty("重置周期")
    private String resetCycle;

    @Schema(description = "抽检段规则")
    @ExcelProperty("抽检段规则")
    private String sampleSegmentRule;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "是否允许预览")
    @ExcelProperty("是否允许预览")
    private Boolean allowPreview;

    @Schema(description = "是否允许解析")
    @ExcelProperty("是否允许解析")
    private Boolean allowParse;

    @Schema(description = "是否允许人工修改已预览号码")
    @ExcelProperty("允许人工修改")
    private Boolean allowManualOverride;

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
