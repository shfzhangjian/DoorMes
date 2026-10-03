package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮生产记录耗材寿命修正 Request VO")
@Data
public class HcGrindingProductionRecordLifeUpdateReqVO {

    @NotNull(message = "磨皮生产记录不能为空")
    private Long id;

    @DecimalMin(value = "0", message = "砂纸累计寿命不能为负数")
    private BigDecimal sandpaperLife;

    @Min(value = 0, message = "砂纸累计天数不能为负数")
    private Integer sandpaperLifeDays;

    @Min(value = 0, message = "导布累计寿命不能为负数")
    private Integer guideClothLife;

    @Size(max = 512, message = "更换原因长度不能超过512个字符")
    private String replaceReason;
}
