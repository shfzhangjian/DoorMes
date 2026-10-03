package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板中间品记录单保存 Request VO")
@Data
public class HcRoughConsoleMiddleProductRecordSaveReqVO {

    @NotNull(message = "记录单ID不能为空")
    private Long recordId;

    private String headerDataJson;

    private List<HcRoughConsoleMiddleProductItemReqVO> details;
}
