// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.wms.vo.WmsInboundSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS入库单保存 Request VO")
@Data
public class WmsInboundSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "入库单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "入库单号不能为空")
    private String inboundNo;

    @Schema(description = "类型: PURCHASE(采购)/PROD(生产完工)")
    private String type;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "物料ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "物料ID不能为空")
    private Long materialId;

    @Schema(description = "入库批次号")
    private String lotNo;

    @Schema(description = "计划数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划数量不能为空")
    private BigDecimal planQty; // 🚨 架构师红线：强约束 BigDecimal

    @Schema(description = "实际数量")
    private BigDecimal actualQty; // 🚨 架构师红线：强约束 BigDecimal

    @Schema(description = "状态: CREATED/INSPECTING/DONE")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

}
