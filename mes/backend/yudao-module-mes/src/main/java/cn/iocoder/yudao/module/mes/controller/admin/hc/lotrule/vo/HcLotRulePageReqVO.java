package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则分页 Request VO")
@Data
public class HcLotRulePageReqVO extends PageParam {

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "业务对象类型")
    private String bizType;

    @Schema(description = "产品类别编码")
    private String productCategoryCode;

    @Schema(description = "生产类型")
    private String prodType;

    @Schema(description = "型号匹配方式")
    private String modelMatchMode;

    @Schema(description = "默认生成触发点")
    private String generationTrigger;

    @Schema(description = "默认生成粒度")
    private String generationScope;

    @Schema(description = "批次数量模型")
    private String batchCardinality;

    @Schema(description = "规则模式")
    private String ruleMode;

    @Schema(description = "前缀")
    private String prefix;

    @Schema(description = "日期格式")
    private String dateFormat;

    @Schema(description = "年份编码模式")
    private String yearCodeMode;

    @Schema(description = "月份编码模式")
    private String monthCodeMode;

    @Schema(description = "流水长度")
    private Integer seqLength;

    @Schema(description = "流水起始值")
    private Integer seqStart;

    @Schema(description = "流水步长")
    private Integer seqStep;

    @Schema(description = "重置周期")
    private String resetCycle;

    @Schema(description = "抽检段规则")
    private String sampleSegmentRule;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否允许预览")
    private Boolean allowPreview;

    @Schema(description = "是否允许解析")
    private Boolean allowParse;

}
