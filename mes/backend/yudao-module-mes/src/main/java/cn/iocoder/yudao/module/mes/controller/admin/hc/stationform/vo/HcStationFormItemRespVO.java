package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单明细 Response VO")
@Data
public class HcStationFormItemRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "表单ID")
    private Long formId;

    @Schema(description = "序号")
    private Integer itemSeq;

    @Schema(description = "分类")
    private String itemCategory;

    @Schema(description = "步骤节点")
    private String stepNode;

    @Schema(description = "项目名称")
    private String itemName;

    @Schema(description = "标准")
    private String standardText;

    @Schema(description = "值模式")
    private String valueMode;

    @Schema(description = "双值标签1")
    private String dualLabel1;

    @Schema(description = "双值标签2")
    private String dualLabel2;

    /** 多字段定义快照，按稳定 key 对应实际值。 */
    private String fieldDefinitionsJson;

    @Schema(description = "默认结果")
    private String defaultResult;

    @Schema(description = "是否必填")
    private Boolean requiredFlag;

    @Schema(description = "备注")
    private String remark;
}
