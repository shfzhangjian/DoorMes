package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切生产记录确认 Request VO")
@Data
public class HcCutRoundProductionRecordConfirmReqVO {

    @NotEmpty(message = "确认记录不能为空")
    private List<Long> ids;

    private String confirmerName;
}
