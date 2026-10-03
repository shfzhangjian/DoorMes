package cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - HC 批次追溯 Response VO")
@Data
public class HcBatchTraceRespVO {

    @Schema(description = "追溯总览")
    private OverviewRespVO overview;

    @Schema(description = "批次树")
    private List<TreeNodeRespVO> treeNodes = new ArrayList<>();

    @Schema(description = "工序时间轴事实节点")
    private List<TimelineNodeRespVO> timelineNodes = new ArrayList<>();

    @Schema(description = "质量事件")
    private List<QualityItemRespVO> qualityEvents = new ArrayList<>();

    @Schema(description = "辅料/工装事件")
    private List<AuxiliaryItemRespVO> auxiliaryEvents = new ArrayList<>();

    @Schema(description = "工艺参数事件")
    private List<ProcessParamItemRespVO> processParamEvents = new ArrayList<>();

    @Schema(description = "批次追溯总览")
    @Data
    public static class OverviewRespVO {

        @Schema(description = "输入批次号")
        private String inputBatchNo;

        @Schema(description = "标准化批次号")
        private String normalizedBatchNo;

        @Schema(description = "识别批次类型")
        private String batchType;

        @Schema(description = "识别批次类型名称")
        private String batchTypeName;

        @Schema(description = "母批号")
        private String rootBatchNo;

        @Schema(description = "分段批次")
        private String segmentBatchNo;

        @Schema(description = "单片片号")
        private String sliceBatchNo;

        @Schema(description = "裁切片号")
        private String finalBatchNo;

        @Schema(description = "当前工序编码")
        private String currentProcessCode;

        @Schema(description = "当前工序名称")
        private String currentProcessName;

        @Schema(description = "当前状态")
        private String currentStatus;

        @Schema(description = "当前状态名称")
        private String currentStatusText;

        @Schema(description = "分支数量")
        private Integer branchCount;

        @Schema(description = "命中报工事实数量")
        private Integer factCount;
    }

    @Schema(description = "批次树节点")
    @Data
    public static class TreeNodeRespVO {

        @Schema(description = "节点 Key")
        private String key;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "批次号")
        private String batchNo;

        @Schema(description = "节点类型 ROOT/SEGMENT/SLICE/FINAL")
        private String nodeType;

        @Schema(description = "节点状态")
        private String status;

        @Schema(description = "节点状态名称")
        private String statusText;

        @Schema(description = "子节点")
        private List<TreeNodeRespVO> children = new ArrayList<>();
    }

    @Schema(description = "时间轴节点")
    @Data
    public static class TimelineNodeRespVO {

        @Schema(description = "节点 Key")
        private String key;

        @Schema(description = "来源类型")
        private String sourceType;

        @Schema(description = "来源记录 ID")
        private Long sourceId;

        @Schema(description = "工序编码")
        private String processCode;

        @Schema(description = "工序名称")
        private String processName;

        @Schema(description = "工序顺序")
        private Integer processOrder;

        @Schema(description = "批次号")
        private String batchNo;

        @Schema(description = "来源批次")
        private String sourceBatchNo;

        @Schema(description = "上游批次")
        private String parentBatchNo;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "状态名称")
        private String statusText;

        @Schema(description = "时间轴时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime eventTime;

        @Schema(description = "摘要")
        private String summary;

        @Schema(description = "报工单号")
        private String reportNo;

        @Schema(description = "报工状态")
        private String reportStatus;

        @Schema(description = "计划号")
        private String planNo;

        @Schema(description = "工位/工作中心")
        private String workCenterName;

        @Schema(description = "设备名称")
        private String equipmentName;

        @Schema(description = "记录人")
        private String recorderName;

        @Schema(description = "确认人")
        private String confirmerName;

        @Schema(description = "明细字段")
        private Map<String, String> details;

        @Schema(description = "质量事件")
        private List<QualityItemRespVO> qualityItems = new ArrayList<>();

        @Schema(description = "辅料/工装")
        private List<AuxiliaryItemRespVO> auxiliaryItems = new ArrayList<>();

        @Schema(description = "工艺参数")
        private List<ProcessParamItemRespVO> processParams = new ArrayList<>();
    }

    @Schema(description = "质量事件")
    @Data
    public static class QualityItemRespVO {

        @Schema(description = "来源时间轴 Key")
        private String timelineKey;

        @Schema(description = "检验单号/事件号")
        private String inspectionNo;

        @Schema(description = "检验类型")
        private String inspectionType;

        @Schema(description = "状态")
        private String status;

        @Schema(description = "判定结果")
        private String result;

        @Schema(description = "事件时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime eventTime;

        @Schema(description = "备注")
        private String remark;
    }

    @Schema(description = "辅料/工装")
    @Data
    public static class AuxiliaryItemRespVO {

        @Schema(description = "来源时间轴 Key")
        private String timelineKey;

        @Schema(description = "辅料类型")
        private String materialType;

        @Schema(description = "辅料类型名称")
        private String materialTypeName;

        @Schema(description = "物料编码")
        private String materialCode;

        @Schema(description = "物料名称/型号")
        private String materialName;

        @Schema(description = "批号")
        private String batchNo;

        @Schema(description = "使用信息")
        private String usageInfo;

        @Schema(description = "来源表")
        private String sourceTable;
    }

    @Schema(description = "工艺参数")
    @Data
    public static class ProcessParamItemRespVO {

        @Schema(description = "来源时间轴 Key")
        private String timelineKey;

        @Schema(description = "参数编码")
        private String paramCode;

        @Schema(description = "参数名称")
        private String paramName;

        @Schema(description = "参数值")
        private String paramValue;

        @Schema(description = "参数数值")
        private BigDecimal paramValueNum;

        @Schema(description = "单位")
        private String uom;

        @Schema(description = "记录时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime recordTime;

        @Schema(description = "记录人")
        private String recorderName;

        @Schema(description = "来源表单")
        private String sourceFormName;

        @Schema(description = "备注")
        private String remark;
    }

}
