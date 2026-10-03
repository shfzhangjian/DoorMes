package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 产品异常事件驳回复检 Response VO")
@Data
public class QmsProductAbnormalEventRejectRespVO {

    @Schema(description = "检验来源类型")
    private String sourceType;

    @Schema(description = "复检链路ID")
    private Long groupId;

    @Schema(description = "根检验单ID")
    private Long rootInspectionId;

    @Schema(description = "根检验单号")
    private String rootInspectionNo;

    @Schema(description = "新复检单ID")
    private Long newInspectionId;

    @Schema(description = "新复检单号")
    private String newInspectionNo;

    @Schema(description = "驳回复检轮次")
    private Integer recheckRoundNo;
}
