package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 分切&压槽生产记录表 Response VO")
@Data
public class HcSlittingPressProductionRecordRespVO {

    private Long id;

    @Schema(description = "日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "垫型：BLACK_PAD/WHITE_PAD；为空表示未归类")
    private String padType;

    @Schema(description = "料号")
    private String materialCode;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "分切投入(m)")
    private BigDecimal slittingInputM;

    @Schema(description = "分切确认产出(pcs)")
    private Integer slittingOutputPcs;

    @Schema(description = "分切NG(pcs)")
    private Integer slittingNgPcs;

    @Schema(description = "压槽实际报工投入(pcs)")
    private BigDecimal pressSlotActualInputPcs;

    @Schema(description = "压槽实际报工产出(pcs)")
    private BigDecimal pressSlotActualOutputPcs;

    @Schema(description = "压槽合格产出(pcs)")
    private BigDecimal pressSlotOutputPcs;

    @Schema(description = "压槽清洗累计片数(pcs)")
    private Integer rollerCleanAccumulatedPcs;

    @Schema(description = "压槽辊清洗周期累计使用天数")
    private Integer rollerCleanUseDays;

    @Schema(description = "轴承更换累计片数(pcs)")
    private Integer bearingReplaceAccumulatedPcs;

    @Schema(description = "压槽辊轴承更换周期累计使用天数")
    private Integer bearingReplaceUseDays;

    @Schema(description = "数据来源")
    private String recordSource;

    @Schema(description = "记录人")
    private String recorderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    private String confirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    private String status;
    private String sourceType;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
