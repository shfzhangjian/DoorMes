package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 内包装扫描片号 Request VO")
@Data
public class HcPackagingAddInnerItemReqVO {

    @NotNull(message = "内包装单元ID不能为空")
    private Long innerUnitId;

    @NotBlank(message = "裁切片号不能为空")
    private String sliceBatchNo;

    private String scanUserName;
}
