package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 母卷批次良品统计数据修订 Request VO")
@Data
public class HcStatisticsDataReviseReqVO {

    @Schema(description = "报工记录编号；磨皮一磨分配记录可传加工单元编号", example = "1024")
    private Long id;

    @Schema(description = "磨皮记录角色：FIRST_ALLOCATION / FIRST_ORIGINAL / SECOND")
    private String recordRole;

    @Schema(description = "磨皮一磨分配加工单元编号", example = "2048")
    private Long firstAllocationId;

    @Schema(description = "配料/湿法统计投入数")
    @DecimalMin(value = "0.000", message = "投入数不能为负数")
    private BigDecimal feedQty;

    @Schema(description = "配料/湿法统计完工数")
    @DecimalMin(value = "0.000", message = "完工数不能为负数")
    private BigDecimal goodQty;

    @Schema(description = "配料/湿法统计损耗数")
    @DecimalMin(value = "0.000", message = "损耗数不能为负数")
    private BigDecimal scrapQty;

    @Schema(description = "粘胶1统计投入米数")
    @DecimalMin(value = "0.000", message = "投入米数不能为负数")
    private BigDecimal inputLength;

    @Schema(description = "磨皮/粘胶1统计固定损耗米数")
    @DecimalMin(value = "0.000", message = "固定损耗米数不能为负数")
    private BigDecimal lossLength;

    @Schema(description = "磨皮/粘胶1统计产出米数")
    @DecimalMin(value = "0.000", message = "产出米数不能为负数")
    private BigDecimal outputLength;

    @Schema(description = "磨皮统计加工米数")
    @DecimalMin(value = "0.000", message = "加工米数不能为负数")
    private BigDecimal processLength;

    @Schema(description = "磨皮一磨分配统计确认加工米数")
    @DecimalMin(value = "0.000", message = "确认加工米数不能为负数")
    private BigDecimal confirmedLength;

    @Schema(description = "修订原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "修订原因不能为空")
    private String reason;
}
