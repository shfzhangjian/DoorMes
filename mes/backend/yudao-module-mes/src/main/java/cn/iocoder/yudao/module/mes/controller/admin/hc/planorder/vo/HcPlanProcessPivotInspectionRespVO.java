package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 计划工序透视送检明细 Response VO")
@Data
public class HcPlanProcessPivotInspectionRespVO {

    @Schema(description = "工序阶段编码")
    private String stageCode;

    @Schema(description = "检验来源类型：FAI、GLUE_BOARD_FAI、CUT_ROUND_FQC、FG_SHIPPING_FQC")
    private String sourceType;

    @Schema(description = "检验单ID")
    private Long inspectionId;

    @Schema(description = "检验单号")
    private String inspectionNo;

    @Schema(description = "检验类型")
    private String inspectionType;

    @Schema(description = "产品批号/片号")
    private String productBatchNo;

    @Schema(description = "送检数量")
    private BigDecimal inspectionQty;

    @Schema(description = "检验NG数量")
    private BigDecimal inspectionNgQty;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "检验单状态")
    private String status;

    @Schema(description = "检验时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;

    @Schema(description = "异常摘要")
    private String defectSummary;

    @Schema(description = "备注")
    private String remark;

}
