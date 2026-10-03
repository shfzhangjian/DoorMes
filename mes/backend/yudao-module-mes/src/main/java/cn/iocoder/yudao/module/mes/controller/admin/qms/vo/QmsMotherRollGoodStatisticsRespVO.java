package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - QMS 母卷批次良品统计 Response VO")
@Data
public class QmsMotherRollGoodStatisticsRespVO {

    @Schema(description = "计划ID")
    private Long id;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "是否后加工计划")
    private Boolean postProcessFlag;

    @Schema(description = "计划号标签文案")
    private String planNoTagText;

    @Schema(description = "计划日期")
    private LocalDate planDate;

    @Schema(description = "计划状态")
    private String planStatus;

    @Schema(description = "生产开始日期")
    private LocalDate productionStartDate;

    @Schema(description = "生产结束日期")
    private LocalDate productionEndDate;

    @Schema(description = "生产料号")
    private String materialCode;

    @Schema(description = "生产物料名称")
    private String materialName;

    @Schema(description = "母料料号")
    private String motherMaterialCode;

    @Schema(description = "母料名称")
    private String motherMaterialName;

    @Schema(description = "母料型号")
    private String motherModelCode;

    @Schema(description = "母料型号名称")
    private String motherModelName;

    @Schema(description = "成品型号")
    private String modelCode;

    @Schema(description = "成品型号名称")
    private String modelName;

    @Schema(description = "尺寸规格")
    private String sizeSpec;

    @Schema(description = "尺寸名称")
    private String sizeName;

    @Schema(description = "计划目标量")
    private BigDecimal targetQty;

    @Schema(description = "净排产量")
    private BigDecimal netPlanQty;

    @Schema(description = "计划目标单位")
    private String targetUom;

    @Schema(description = "主批号")
    private String batchNo;

    @Schema(description = "生产批号")
    private String productionBatchNo;

    @Schema(description = "父生产批号")
    private String parentProductionBatchNo;

    @Schema(description = "母卷批号")
    private String motherRollBatchNo;

    @Schema(description = "分段批次号/追踪分段批号")
    private String segmentBatchNo;

    @Schema(description = "透视显示行分组键")
    private String pivotRowKey;

    @Schema(description = "计划级合并键")
    private String planMergeKey;

    @Schema(description = "分段批号合并键")
    private String segmentMergeKey;

    @Schema(description = "型号尺寸合并键")
    private String modelSizeMergeKey;

    @Schema(description = "实际/显示型号")
    private String actualModelCode;

    @Schema(description = "目标匹配型号前三位")
    private String modelSeriesCode;

    @Schema(description = "实际/显示尺寸")
    private String actualSizeSpec;

    @Schema(description = "差异起始工序编码")
    private String variationStartStageCode;

    @Schema(description = "差异起始工序名称")
    private String variationStartStageName;

    @Schema(description = "各工序合并键，key 为阶段编码")
    private Map<String, String> stageMergeKeys;

    @Schema(description = "累计不合格/损耗数")
    private BigDecimal totalDefectQty;

    @Schema(description = "最后报工时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime latestReportTime;

    @Schema(description = "工序阶段台账，key 为阶段编码")
    private Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages;

}
