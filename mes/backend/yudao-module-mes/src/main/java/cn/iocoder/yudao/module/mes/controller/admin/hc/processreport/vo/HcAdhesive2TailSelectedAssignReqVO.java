package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/** 粘胶2按来源片选择尾号。 */
@Data
public class HcAdhesive2TailSelectedAssignReqVO {
    @NotNull private Long planId;
    @NotNull private Long planOperationId;
    @Valid @NotEmpty private List<@NotNull Item> items;

    @Data
    public static class Item {
        @NotBlank private String sourceProductionBatchNo;
        @NotBlank private String actualSizeRule;
    }
}
