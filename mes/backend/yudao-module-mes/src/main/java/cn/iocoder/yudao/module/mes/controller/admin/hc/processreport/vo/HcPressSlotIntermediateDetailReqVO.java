package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 压槽中间品记录明细 Request VO")
@Data
public class HcPressSlotIntermediateDetailReqVO {

    private Long id;
    private Long pressSlotReportId;
    private Long sourceSlittingSliceId;
    private String samplePosition;
    private String samplePositionName;
    private String sliceBatchNo;
    private BigDecimal widthMm;
    private BigDecimal thickness1;
    private BigDecimal thickness2;
    private BigDecimal thickness3;
    private BigDecimal thickness4;
    private BigDecimal thickness5;
    private BigDecimal thickness6;
    private BigDecimal thickness7;
    private BigDecimal thickness8;
    private BigDecimal thickness9;
    private BigDecimal thickness10;
    private String remark;
    private Integer sortNo;
}
