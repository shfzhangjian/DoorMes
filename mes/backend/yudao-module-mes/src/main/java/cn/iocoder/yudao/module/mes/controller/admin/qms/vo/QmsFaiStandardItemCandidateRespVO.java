package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - FAI复检标准检验项目候选 Response VO")
@Data
public class QmsFaiStandardItemCandidateRespVO {

    @Schema(description = "标准检验项目ID")
    private Long standardItemId;

    @Schema(description = "检验项目")
    private String inspectionItem;

    @Schema(description = "标准要求")
    private String standardDesc;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "检验方法")
    private String inspectionMethod;

    @Schema(description = "检验频次/判定规则")
    private String testFrequencyJudgement;

    @Schema(description = "样本数")
    private Integer sampleSize;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "原检对应项目是否为NG；仅提示，不代表已选择")
    private Boolean originalNg;
}
