package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - HC 生产计划产品批号预览 Request VO")
@Data
public class HcPlanOrderBatchPreviewReqVO {

    @Schema(description = "计划ID；编辑时用于排除自身占号")
    private Long id;

    @Schema(description = "兼容字段；母批批号日期以计划开始日期为准")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    @Schema(description = "生产开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionStartDate;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料分类编码")
    private String categoryCode;

    @Schema(description = "生产类型")
    private String prodType;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "母料型号编码")
    private String motherModelCode;

    @Schema(description = "批次规则ID")
    private Long batchRuleId;

    @Schema(description = "批次规则编码")
    private String batchRuleCode;

    @Schema(description = "是否优先使用计划已绑定的规则版本，仅供已建计划任务展示预览使用")
    private Boolean useBoundRule;

    @Schema(description = "首道工序编码")
    private String opCode;

    @Schema(description = "首道工序名称")
    private String opName;

    @Schema(description = "首道工序工作中心ID")
    private Long workCenterId;

}
