package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶2提交 FAI 工艺参数点检申请 Request VO")
@Data
public class HcAdhesive2FaiApplyReqVO {

    @NotNull(message = "计划ID不能为空")
    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long planOperationId;

    @Schema(description = "触发原因；NEW_ORDER=新单首检，REWORK_RECHECK=驳回/取消/NG后重提")
    private String triggerReason;

    @Schema(description = "检验标准匹配方式：MATERIAL_PROCESS=胶板取样，PRODUCT_MODEL_PROCESS=COA按产品型号+工序")
    private String standardMatchMode;

    @Schema(description = "检验场景：COA=COA送检，PROCESS_CHECK=过程加检；空值保持工艺参数点检逻辑")
    private String inspectionScene;

    @Schema(description = "COA送检母卷批号")
    private String inspectionScopeBatchNo;

    @Schema(description = "来源压槽报工记录ID")
    private Long sourceReportId;

    @Schema(description = "COA送检选中片号，写入FAI产品批次")
    private String productBatchNo;

    @Schema(description = "送检时实际产品型号，粘胶2换型后用于匹配成品COA标准")
    private String productModel;

    @Schema(description = "送检时实际产品料号")
    private String materialCode;

    @Schema(description = "送检时实际产品名称")
    private String materialName;

    @Schema(description = "送检时实际产品规格")
    private String specification;

    @Schema(description = "同母卷COA片号列表，仅用于校验和备注")
    private List<String> coaSliceNos;

    @Schema(description = "送检人")
    private String submitterName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "胶板领用记录ID")
    private Long glueBoardUsageId;

    @Schema(description = "胶板物料编码")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板批号")
    private String glueBoardBatchNo;

    @Schema(description = "首样取样起点")
    private BigDecimal sampleStartPosition;

    @Positive(message = "送检长度必须大于0")
    @Schema(description = "首样送检长度")
    private BigDecimal sampleLength;
}
