package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 配料生产记录表 Response VO")
@Data
public class HcFormulaProductionRecordRespVO {

    @Schema(description = "报工记录ID")
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

    @Schema(description = "滤网批号")
    private String filterBatchNo;

    @Schema(description = "投料重量(kg)")
    private BigDecimal inputWeight;

    @Schema(description = "产出重量(kg)")
    private BigDecimal outputWeight;

    @Schema(description = "搅拌机机台编号")
    private String mixerEquipmentCode;

    @Schema(description = "配料罐罐号")
    private String batchingTankNo;

    @Schema(description = "脱泡机机台编号")
    private String foamingEquipmentCode;

    @Schema(description = "脱泡罐罐号")
    private String defoamingTankNo;

    @Schema(description = "记录人")
    private String recorderName;

    @Schema(description = "实际完工时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    @Schema(description = "备注")
    private String remark;

}
