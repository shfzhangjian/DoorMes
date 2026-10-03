package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

public final class SrmTrialValidationActionReqVO {

    private SrmTrialValidationActionReqVO() {
    }

    @Data
    public static class TrialExecute {
        @NotNull(message = "试产验证单ID不能为空")
        private Long id;
        @NotEmpty(message = "物料批号不能为空")
        private String materialBatchNo;
        @NotNull(message = "数量不能为空")
        @DecimalMin(value = "0.000001", message = "数量必须大于0")
        private BigDecimal quantity;
        private String opinion;
    }

    @Data
    public static class ProductionComplete {
        @NotNull(message = "试产验证单ID不能为空")
        private Long id;
        @NotEmpty(message = "物料批号不能为空")
        private String materialBatchNo;
        @NotNull(message = "数量不能为空")
        @DecimalMin(value = "0.000001", message = "数量必须大于0")
        private BigDecimal quantity;
        private String opinion;
    }

    @Data
    public static class ArchiveConfirm {
        @NotNull(message = "试产验证单ID不能为空")
        private Long id;
        private String opinion;
    }

}
