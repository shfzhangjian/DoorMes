package cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 工作中心分页 Request VO")
@Data
public class HcWorkCenterPageReqVO extends PageParam {

    @Schema(description = "工作中心编码")
    private String wcCode;

    @Schema(description = "工作中心名称")
    private String wcName;

    @Schema(description = "工序名称，兼容历史字段 process_stage")
    private String processStage;

    @Schema(description = "标准工序ID")
    private Long processId;

    @Schema(description = "标准工序编码")
    private String processCode;

    @Schema(description = "标准工序名称")
    private String processName;

    @Schema(description = "产线编码")
    private String lineCode;

    @Schema(description = "产线名称")
    private String lineName;

    @Schema(description = "绑定工位 IP")
    private String terminalIps;

    @Schema(description = "产线短码")
    private String lineShortCode;

    @Schema(description = "批次号产线码")
    private String batchLineCode;

    @Schema(description = "标准小时产能")
    private BigDecimal capacityPerHour;

    @Schema(description = "产能单位")
    private String capacityUom;

    @Schema(description = "默认班制")
    private String defaultShiftMode;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
