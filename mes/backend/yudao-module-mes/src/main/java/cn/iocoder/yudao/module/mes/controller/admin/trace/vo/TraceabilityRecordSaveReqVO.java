// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.controller.admin.trace.vo.TraceabilityRecordSaveReqVO.java
package cn.iocoder.yudao.module.mes.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 全链路批次追溯谱系保存 Request VO")
@Data
public class TraceabilityRecordSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联主工单ID")
    private Long workOrderId;

    @Schema(description = "关联派工细单ID")
    private Long subOrderId;

    @Schema(description = "动作(CONSUME消耗, PRODUCE产出, SPLIT拆批, MERGE合批)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "追溯动作不能为空")
    private String actionType;

    @Schema(description = "投入/父批次号")
    private String inputLotNo;

    @Schema(description = "产出/子批次号")
    private String outputLotNo;

    @Schema(description = "投入数量")
    private BigDecimal inputQty; // 🚨 架构师红线：BigDecimal 精确核算

    @Schema(description = "产出数量")
    private BigDecimal outputQty; // 🚨 架构师红线：BigDecimal 精确核算

}
