package cn.iocoder.yudao.module.mes.controller.admin.hc.lotinstance.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 批次实例台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcLotInstancePageReqVO extends PageParam {

    @Schema(description = "生产批号（支持模糊查询）")
    private String lotNo;

    @Schema(description = "规则ID")
    private Long ruleId;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "产品分类")
    private String productCategoryCode;

    @Schema(description = "生产类型")
    private String prodType;

    @Schema(description = "生产计划号")
    private String planNo;

    @Schema(description = "物料编码或名称")
    private String materialKeyword;

    @Schema(description = "产品型号")
    private String modelCode;

    @Schema(description = "批次层级")
    private String batchLevel;

    @Schema(description = "生成状态")
    private String instanceStatus;

    @Schema(description = "生成来源")
    private String generateSource;

    @Schema(description = "产线代码")
    private String lineCode;
}
