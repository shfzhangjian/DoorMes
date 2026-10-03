package cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 工艺参数采集上下文 Response VO")
@Data
public class HcParamRecordContextRespVO {

    @Schema(description = "报工ID")
    private Long reportId;

    @Schema(description = "报工单号")
    private String reportNo;

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "工单号")
    private String workOrderNo;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "工艺路线ID")
    private Long routeId;

    @Schema(description = "工艺路线编码")
    private String routeCode;

    @Schema(description = "工艺路线名称")
    private String routeName;

    @Schema(description = "物料ID")
    private Long productMaterialId;

    @Schema(description = "物料编码")
    private String productMaterialCode;

    @Schema(description = "物料名称")
    private String productMaterialName;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "参数模板JSON")
    private String paramTemplateJson;

    @Schema(description = "已采集参数记录")
    private List<HcParamRecordRespVO> records;
}
