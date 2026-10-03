package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 外包装扫描内包装单元 Request VO")
@Data
public class HcPackagingAddOuterUnitReqVO {

    @NotNull(message = "外包装箱/板ID不能为空")
    private Long outerBoxId;

    @NotBlank(message = "内包装单元号不能为空")
    private String innerUnitNo;

    private String scanUserName;
}
