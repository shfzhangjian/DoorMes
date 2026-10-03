package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 实物领用消耗，与砂纸加工寿命、导布使用次数分别记录。 */
@Data
public class HcGrindingConsumptionVO {
    private String requestKey;
    private Long sandpaperLedgerId;
    private BigDecimal sandpaperQty;
    private String sandpaperUnit;
    private Long guideClothLedgerId;
    private BigDecimal guideClothQty;
    private String guideClothUnit;
}
