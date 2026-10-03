package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class SrmPerformanceQuarterEvaluationBatchCreateReqVO {

    @NotNull(message = "年度不能为空")
    private Integer evalYear;

    @NotNull(message = "季度不能为空")
    private Integer evalQuarter;

    @NotNull(message = "请选择季度评价模板版本")
    private Long templateVersionId;

    @NotEmpty(message = "请选择需要生成评价单的供应商")
    private List<Long> supplierIds;

}
