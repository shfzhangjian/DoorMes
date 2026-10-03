package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 裁切生产记录表 Response VO")
@Data
public class HcCutRoundProductionRecordRespVO {

    @Schema(description = "记录编号")
    private Long id;

    @Schema(description = "日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "垫型：BLACK_PAD/WHITE_PAD；为空表示未归类")
    private String padType;

    @Schema(description = "生产批号")
    private String productionBatchNo;

    @Schema(description = "裁切尺寸(mm)")
    private String cutSizeMm;

    @Schema(description = "投入(pcs)")
    private BigDecimal inputQty;

    @Schema(description = "产出(pcs)")
    private BigDecimal outputQty;

    @Schema(description = "刀片型号")
    private String bladeModel;

    @Schema(description = "刀片批号")
    private String bladeBatchNo;

    @Schema(description = "刀片累计裁切(pcs)")
    private Integer bladeUseCount;

    @Schema(description = "毛毡型号")
    private String feltModel;

    @Schema(description = "毛毡批号")
    private String feltBatchNo;

    @Schema(description = "裁切片数累计(<=2000pcs)(pcs)")
    private Integer feltUseCount;

    @Schema(description = "毛毡累计使用天数(<=3个月/90天)")
    private Integer feltUseDays;

    @Schema(description = "刀片更换原因")
    private String bladeReplaceReason;

    @Schema(description = "记录人")
    private String recorderName;

    @Schema(description = "记录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    @Schema(description = "确认人")
    private String confirmerName;

    @Schema(description = "确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    @Schema(description = "确认状态")
    private String status;

    @Schema(description = "数据来源，MANUAL/IMPORT/REPORT_INIT")
    private String sourceType;

    @Schema(description = "生产记录来源展示")
    private String recordSource;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
