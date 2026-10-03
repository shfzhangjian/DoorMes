package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 湿法报工提交首检申请 Request VO")
@Data
public class HcWetFaiApplyReqVO {

    @NotNull(message = "计划ID不能为空")
    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planOperationId;

    @Schema(description = "触发原因；默认 NEW_ORDER，重提默认 REWORK_RECHECK")
    private String triggerReason;

    @Schema(description = "检验标准匹配方式：PRODUCT_MODEL_PROCESS=产品型号+工序，MATERIAL_PROCESS=物料编码+工序；湿法首检固定使用 PRODUCT_MODEL_PROCESS")
    private String standardMatchMode;

    @Schema(description = "检验场景；PROCESS_CHECK=过程加检")
    private String inspectionScene;

    @Schema(description = "检验范围批号；压槽过程加检传入母卷批号")
    private String inspectionScopeBatchNo;

    @Schema(description = "来源报工ID；过程加检关联已保存并确认的报工记录")
    private Long sourceReportId;

    @Schema(description = "来源单号；过程加检可按来源单号辅助追溯")
    private String sourceReportNo;

    @Schema(description = "兼容字段：湿法首检自动取任务母料ID，不使用前端覆盖值")
    private Long materialId;

    @Schema(description = "兼容字段：湿法首检自动取任务母料料号，仅用于展示追溯，不作为标准匹配硬条件")
    private String materialCode;

    @Schema(description = "兼容字段：湿法首检自动取任务母料名称，不使用前端覆盖值")
    private String materialName;

    @Schema(description = "兼容字段：湿法首检自动取任务规格，不使用前端覆盖值")
    private String specification;

    @Schema(description = "首检扫码批号；压槽首检必须扫码获得，写入 FAI 产品批次号")
    private String productBatchNo;

    @Schema(description = "湿法 NAP层送检米数")
    @DecimalMin(value = "0", inclusive = false, message = "NAP层送检(米)必须大于0")
    private BigDecimal napSampleLength;

    @Schema(description = "送检人")
    private String submitterName;

    @Schema(description = "备注")
    private String remark;
}
