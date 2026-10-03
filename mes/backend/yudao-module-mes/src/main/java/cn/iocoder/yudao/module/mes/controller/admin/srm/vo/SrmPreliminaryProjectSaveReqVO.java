package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SrmPreliminaryProjectSaveReqVO {

    private Long id;
    @NotEmpty(message = "项目编码不能为空")
    private String projectCode;
    @NotEmpty(message = "项目名称不能为空")
    private String projectName;
    private Long currentTemplateVersionId;
    private String status;
    private String remark;
    private Integer version;

}
