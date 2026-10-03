package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 配料过站工作明细 Request VO")
@Data
public class HcFormulaPassWorkItemReqVO {

    @Schema(description = "项目顺序号")
    private Integer itemSeq;

    @Schema(description = "项目类别")
    private String category;

    @Schema(description = "步骤节点")
    private String node;

    @Schema(description = "点检项目")
    private String item;

    @Schema(description = "标准")
    private String standard;

    @Schema(description = "记录模式")
    private String valueMode;

    @Schema(description = "双值标签1")
    private String dualLabel1;

    @Schema(description = "双值标签2")
    private String dualLabel2;

    /** 多字段定义快照，按稳定 key 对应实际值。 */
    private String fieldDefinitionsJson;
    private String fieldValuesJson;

    @Schema(description = "记录值1")
    private String actualValue;

    @Schema(description = "记录值2")
    private String actualValue2;

    @Schema(description = "结果")
    private String status;

    @Schema(description = "异常说明")
    private String remark;
}
