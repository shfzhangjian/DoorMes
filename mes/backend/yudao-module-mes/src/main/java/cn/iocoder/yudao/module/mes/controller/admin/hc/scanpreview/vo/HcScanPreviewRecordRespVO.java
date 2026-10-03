package cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 扫码预览记录 Response VO")
@Data
public class HcScanPreviewRecordRespVO {

    @Schema(description = "来源表记录 ID")
    private Long id;

    @Schema(description = "统一报工流水 ID")
    private Long operationReportId;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源类型名称")
    private String sourceTypeName;

    @Schema(description = "业务单号")
    private String bizNo;

    @Schema(description = "计划 ID")
    private Long planId;

    @Schema(description = "计划单号")
    private String planNo;

    @Schema(description = "计划工序 ID")
    private Long planOperationId;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "料号")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "来源批号")
    private String sourceBatchNo;

    @Schema(description = "生产批号")
    private String productionBatchNo;

    @Schema(description = "上游生产批号")
    private String parentProductionBatchNo;

    @Schema(description = "报工日期")
    private LocalDate reportDate;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    @Schema(description = "报工数量")
    private BigDecimal reportQty;

    @Schema(description = "报工单位")
    private String reportUom;

    @Schema(description = "报工状态")
    private String reportStatus;

    @Schema(description = "记录人")
    private String recorderName;

    @Schema(description = "记录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;

    @Schema(description = "确认人")
    private String confirmerName;

    @Schema(description = "确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmerTime;

    @Schema(description = "打印状态")
    private String printStatus;

    @Schema(description = "打印次数")
    private Integer printCount;

    @Schema(description = "最近打印时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;

    @Schema(description = "扫码状态")
    private String scanStatus;

    @Schema(description = "送检单号")
    private String inspectionNo;

    @Schema(description = "送检状态")
    private String inspectionStatus;

    @Schema(description = "送检结果")
    private String inspectionResult;

    @Schema(description = "送检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionApplyTime;

    @Schema(description = "检验返回时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionReturnTime;

    @Schema(description = "送检备注")
    private String inspectionRemark;

    @Schema(description = "报工二维码内容")
    private String reportQrValue;

    @Schema(description = "报工二维码显示文本")
    private String reportQrText;

    @Schema(description = "送检二维码内容")
    private String inspectionQrValue;

    @Schema(description = "是否存在送检单")
    private Boolean hasInspection;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
