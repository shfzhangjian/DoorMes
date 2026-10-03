package cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工艺参数记录分页 Request VO")
@Data
public class HcParamRecordPageReqVO extends PageParam {

    @Schema(description = "报工ID")
    private Long reportId;

    @Schema(description = "报工单号")
    private String reportNo;

    @Schema(description = "参数编码")
    private String paramCode;

    @Schema(description = "参数名称")
    private String paramName;

    @Schema(description = "判定结果")
    private String judgeResult;
}
