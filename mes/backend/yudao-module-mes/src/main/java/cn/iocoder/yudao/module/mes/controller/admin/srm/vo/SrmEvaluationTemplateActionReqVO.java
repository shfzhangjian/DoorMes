package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrmEvaluationTemplateActionReqVO {

    @NotNull(message = "模板版本ID不能为空")
    private Long versionId;
    private Boolean approved;
    private String opinion;
    private String changeSummary;

}
