package cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 湿法生产记录确认 Request VO")
@Data
public class HcWetProductionRecordConfirmReqVO {

    @Schema(description = "记录 ID 列表")
    @NotEmpty(message = "确认记录不能为空")
    private List<Long> ids;

    @Schema(description = "认证人员名称")
    private String confirmerName;
}
