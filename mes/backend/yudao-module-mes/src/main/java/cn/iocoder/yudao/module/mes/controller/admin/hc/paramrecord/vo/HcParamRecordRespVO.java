package cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工艺参数记录 Response VO")
@Data
public class HcParamRecordRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "报工ID")
    private Long reportId;

    @Schema(description = "报工单号")
    private String reportNo;

    @Schema(description = "参数编码")
    private String paramCode;

    @Schema(description = "参数名称")
    private String paramName;

    @Schema(description = "参数值")
    private String paramValue;

    @Schema(description = "数值型参数值")
    private BigDecimal valueNum;

    @Schema(description = "单位")
    private String uom;

    @Schema(description = "判定结果")
    private String judgeResult;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
