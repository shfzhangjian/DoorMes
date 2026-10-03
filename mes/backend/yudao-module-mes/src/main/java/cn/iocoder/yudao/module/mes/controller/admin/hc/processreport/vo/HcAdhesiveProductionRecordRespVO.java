package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶生产记录表 Response VO")
@Data
public class HcAdhesiveProductionRecordRespVO {

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

    @Schema(description = "投入数量")
    private BigDecimal inputQty;

    @Schema(description = "产出数量")
    private BigDecimal outputQty;

    @Schema(description = "胶板料号")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板批号")
    private String glueBoardBatchNo;

    @Schema(description = "胶板消耗量（研发样品手工消耗）")
    private BigDecimal glueBoardConsumeQty;

    @Schema(description = "数据来源")
    private String recordSource;

    @Schema(description = "记录人")
    private String recorderName;

    @Schema(description = "记录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    @Schema(description = "备注")
    private String remark;

}
