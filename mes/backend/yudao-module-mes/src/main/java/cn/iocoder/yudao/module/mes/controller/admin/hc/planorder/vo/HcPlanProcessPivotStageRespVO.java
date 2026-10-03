package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeEvaluationRespVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - HC 计划工序透视阶段 Response VO")
@Data
public class HcPlanProcessPivotStageRespVO {

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

    @Schema(description = "送检数量")
    private BigDecimal inspectionQty;

    @Schema(description = "检验NG数量")
    private BigDecimal inspectionNgQty;

    @Schema(description = "已确认数量")
    private BigDecimal confirmedQty;

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

    @Schema(description = "额定 QTIME 评估结果")
    private HcQtimeEvaluationRespVO qtime;

    @Schema(description = "阶段备注")
    private String remark;

    @Schema(description = "片级钻取明细")
    private List<HcPlanProcessPivotPieceRespVO> pieceDetails;

    @Schema(description = "送检钻取明细")
    private List<HcPlanProcessPivotInspectionRespVO> inspectionDetails;

}
