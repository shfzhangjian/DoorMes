package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 创建外包装箱/板 Request VO")
@Data
public class HcPackagingCreateOuterBoxReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    private String packMethod;
    private Integer standardQty;
    private Boolean tailBoxFlag;
    private String recorderName;
    private String remark;
}
