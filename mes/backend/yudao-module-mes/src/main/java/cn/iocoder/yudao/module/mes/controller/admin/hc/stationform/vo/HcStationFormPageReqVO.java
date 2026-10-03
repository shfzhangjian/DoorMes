package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单分页 Request VO")
@Data
public class HcStationFormPageReqVO extends PageParam {

    @Schema(description = "表单编码")
    private String formCode;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

    @Schema(description = "触发时机编码")
    private String triggerTimingCode;

    @Schema(description = "是否需要确认")
    private Boolean needConfirm;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
