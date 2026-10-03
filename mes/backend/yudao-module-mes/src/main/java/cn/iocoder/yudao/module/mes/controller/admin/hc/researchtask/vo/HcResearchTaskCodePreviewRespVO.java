package cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - HC 研发型号编码预览 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HcResearchTaskCodePreviewRespVO {

    @Schema(description = "研发型号编码")
    private String rdModelCode;

    @Schema(description = "展示型号编码")
    private String displayModelCode;

    @Schema(description = "重复配方/组合使用序号")
    private Integer reuseSeq;

    @Schema(description = "同组合历史次数")
    private Integer combinationUsageCount;

    @Schema(description = "配方使用次数")
    private Integer formulaUsageCount;

    @Schema(description = "湿法使用次数")
    private Integer wetUsageCount;

    @Schema(description = "磨皮使用次数")
    private Integer grindingUsageCount;

    @Schema(description = "后工艺使用次数")
    private Integer postUsageCount;

}
