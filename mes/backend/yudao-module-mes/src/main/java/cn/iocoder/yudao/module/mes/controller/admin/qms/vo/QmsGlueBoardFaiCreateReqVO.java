package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 胶板检验新增 Request VO")
@Data
public class QmsGlueBoardFaiCreateReqVO {

    @Schema(description = "生产工单号")
    private String workOrderNo;

    @Schema(description = "生产计划/工序任务ID")
    private Long planOrderId;

    @Schema(description = "来源分段号")
    private String sourceReportNo;

    @Schema(description = "来源分段号；粘胶看板兼容字段")
    private String sourceProductionBatchNo;

    @Schema(description = "产品型号")
    private String productModel;

    @Schema(description = "胶板边库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "胶板库存内码不能为空")
    private Long glueBoardStockId;

    @Schema(description = "粘胶2胶板领用记录ID")
    private Long glueBoardUsageId;

    @Schema(description = "胶板型号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "胶板型号不能为空")
    private String glueBoardModel;

    @Schema(description = "胶板批次", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "胶板批次不能为空")
    private String gluePlateBatchNo;

    @Schema(description = "胶板料号")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板名称")
    private String glueBoardMaterialName;

    @Schema(description = "送检米数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "送检米数不能为空")
    @Positive(message = "送检米数必须大于0")
    private BigDecimal sampleLength;

    @Schema(description = "送检取样起点")
    private BigDecimal sampleStartPosition;

    @Schema(description = "工序类别", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序类别不能为空")
    private String processCategory;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "机台编码")
    private String machineCode;

    @Schema(description = "采用的检验标准ID；为空时按胶板型号自动匹配 GLUE_BOARD_FAI 标准")
    private Long standardId;

    @Schema(description = "送检时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime submissionTime;

    @Schema(description = "送检人员")
    private String submitterName;

    @Schema(description = "备注")
    private String remark;
}
