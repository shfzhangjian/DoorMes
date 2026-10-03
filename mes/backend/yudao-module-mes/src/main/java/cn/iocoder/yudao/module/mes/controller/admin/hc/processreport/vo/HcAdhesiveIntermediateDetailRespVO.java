package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 粘双面胶中间品记录明细 Response VO")
@Data
public class HcAdhesiveIntermediateDetailRespVO {

    private Long id;
    private BigDecimal lengthMark;
    private BigDecimal leftThickness;
    private BigDecimal rightThickness;
    private String remark;
    private Integer sortNo;
}
