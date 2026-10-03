package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - QMS 母卷批次良品统计阶段 Response VO")
@Data
public class QmsMotherRollGoodStatisticsStageRespVO {

    @Schema(description = "工序阶段编码")
    private String stageCode;

    @Schema(description = "工序阶段名称")
    private String stageName;

    @Schema(description = "工序阶段状态")
    private String stageStatus;

    @Schema(description = "来源批号/母批号")
    private String sourceBatchNos;

    @Schema(description = "产出批号/分段批次号")
    private String outputBatchNos;

    @Schema(description = "投入量")
    private BigDecimal inputQty;

    @Schema(description = "投入量合并键")
    private String inputQtyMergeKey;

    @Schema(description = "分段投入量")
    private BigDecimal segmentInputQty;

    @Schema(description = "报工量")
    private BigDecimal reportQty;

    @Schema(description = "完工量")
    private BigDecimal doneQty;

    @Schema(description = "未加工量")
    private BigDecimal pendingQty;

    @Schema(description = "未加工量单位")
    private String pendingUnit;

    @Schema(description = "不合格/损耗量")
    private BigDecimal defectQty;

    @Schema(description = "分段自检NG数量")
    private BigDecimal segmentDefectQty;

    @Schema(description = "送检数量")
    private BigDecimal inspectionQty;

    @Schema(description = "本工序损耗")
    private BigDecimal processProductionInspectionQty;

    @Schema(description = "COA送检数量")
    private BigDecimal coaInspectionQty;

    @Schema(description = "胶板送检米数")
    private BigDecimal glueBoardInspectionQty;

    @Schema(description = "检验NG数量")
    private BigDecimal inspectionNgQty;

    @Schema(description = "COA检验NG数量")
    private BigDecimal coaInspectionNgQty;

    @Schema(description = "已确认数量")
    private BigDecimal confirmedQty;

    @Schema(description = "母卷产出值")
    private BigDecimal motherOutputQty;

    @Schema(description = "母卷产出值单位")
    private String motherOutputUnit;

    @Schema(description = "母卷产出值合并键")
    private String motherOutputMergeKey;

    @Schema(description = "终检产出")
    private BigDecimal finalInspectionOutputQty;

    @Schema(description = "终检产出单位")
    private String finalInspectionOutputUnit;

    @Schema(description = "终检产出合并键")
    private String finalInspectionOutputMergeKey;

    @Schema(description = "终检良率百分比")
    private BigDecimal finalInspectionYieldRate;

    @Schema(description = "终检良率合并键")
    private String finalInspectionYieldRateMergeKey;

    @Schema(description = "理论产量")
    private BigDecimal theoreticalOutputQty;

    @Schema(description = "理论产量单位")
    private String theoreticalOutputUnit;

    @Schema(description = "理论产量匹配型号")
    private String theoreticalOutputModelCode;

    @Schema(description = "理论产量匹配工序编码")
    private String theoreticalOutputProcessCode;

    @Schema(description = "理论产量是否已匹配配置")
    private Boolean theoreticalOutputMatched;

    @Schema(description = "理论产量合并键")
    private String theoreticalOutputMergeKey;

    @Schema(description = "良品率百分比")
    private BigDecimal goodYieldRate;

    @Schema(description = "良品率合并键")
    private String goodYieldRateMergeKey;

    @Schema(description = "良品达标率百分比")
    private BigDecimal goodTargetRate;

    @Schema(description = "良品达标率合并键")
    private String goodTargetRateMergeKey;

    @Schema(description = "折算长度")
    private BigDecimal lengthQty;

    @Schema(description = "折算长度单位")
    private String lengthUnit;

    @Schema(description = "二次磨皮起位置")
    private BigDecimal startPosition;

    @Schema(description = "二次磨皮加工长度")
    private BigDecimal processLength;

    @Schema(description = "计量单位")
    private String reportUnit;

    @Schema(description = "最后报工时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReportTime;

    @Schema(description = "阶段备注")
    private String remark;

    @Schema(description = "片级钻取明细")
    private List<QmsMotherRollGoodStatisticsPieceRespVO> pieceDetails;

    @Schema(description = "送检钻取明细")
    private List<QmsMotherRollGoodStatisticsInspectionRespVO> inspectionDetails;

}
