package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 分切切片打印标记 Request VO")
@Data
public class HcSlittingSlicePrintReqVO {

    @NotEmpty(message = "切片记录ID不能为空")
    private List<Long> ids;
}
