package cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HC 研发管理工艺路线工序快照 Request VO")
@Data
public class HcResearchTaskRouteOperationReqVO {

    @Schema(description = "工序顺序")
    private Integer opSeq;

    @Schema(description = "工序编码")
    private String opCode;

    @Schema(description = "工序名称")
    private String opName;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "工作中心编码")
    private String workCenterCode;

    @Schema(description = "工作中心名称")
    private String workCenterName;

    @Schema(description = "执行要求")
    private String instructionText;

}
