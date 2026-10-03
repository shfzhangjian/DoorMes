package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 计划换型日志保存 Request VO")
@Data
public class HcPlanChangeoverLogSaveReqVO {

    @NotNull(message = "计划编号不能为空")
    private Long planId;

    @NotNull(message = "计划工序编号不能为空")
    private Long planOperationId;

    private String beforeMaterialCode;
    private String afterMaterialCode;

    @NotBlank(message = "换型前型号不能为空")
    private String beforeModelCode;

    @NotBlank(message = "换型后型号不能为空")
    private String afterModelCode;

    private String beforeGlueBoardModel;
    private String afterGlueBoardModel;
    private String remark;
    private String extraJson;
}
