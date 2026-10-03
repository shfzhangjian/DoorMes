package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 包装单元动作 Request VO")
@Data
public class HcPackagingUnitActionReqVO {

    @NotNull(message = "业务ID不能为空")
    private Long id;

    private String operatorName;
    private String reason;
    private String scanNo;
}
