package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板分段中间品记录单初始化 Request VO")
@Data
public class HcRoughConsoleMiddleProductSegmentReqVO {

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    private String passType;

    private String segmentMark;
}
