package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 湿法报工异常位置明细 Response VO")
@Data
public class HcWetReportAbnormalPositionRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "湿法最终END报工记录ID")
    private Long operationReportId;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划工序ID")
    private Long planOperationId;

    @Schema(description = "计划单号")
    private String planNo;

    @Schema(description = "来源模块编码")
    private String sourceMenuCode;

    @Schema(description = "工序阶段")
    private String processStage;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "关联批次号")
    private String batchNo;

    @Schema(description = "生产批次号")
    private String productionBatchNo;

    @Schema(description = "来源计划号")
    private String sourcePlanNo;

    @Schema(description = "来源前端行UID")
    private String sourceRowUid;

    @Schema(description = "来源明细ID")
    private Long sourceDetailId;

    @Schema(description = "异常位置")
    private String positionText;

    @Schema(description = "异常米数")
    private BigDecimal abnormalLength;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
