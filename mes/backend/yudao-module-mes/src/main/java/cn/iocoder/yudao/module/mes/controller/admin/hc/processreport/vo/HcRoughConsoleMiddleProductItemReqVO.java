package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板中间品记录单明细 Request VO")
@Data
public class HcRoughConsoleMiddleProductItemReqVO {

    private Integer seq;
    private BigDecimal lengthMeter;
    private String innerThickness;
    private String outerThickness;
    private LocalDate recordDate;
    private String modelCode;
    private String materialCode;
    private String batchNo;
    private BigDecimal inputLength;
    private BigDecimal outputLength;
    private String grindingPass;
    private BigDecimal sandpaperLife;
    private String sandpaperBatchNo;
    private Integer guideClothLife;
    private String guideClothBatchNo;
    private String replaceReason;
    private String recorderName;
    private String length;
    private String width;
    private String thickness;
    private String result;
    private String remark;
}
