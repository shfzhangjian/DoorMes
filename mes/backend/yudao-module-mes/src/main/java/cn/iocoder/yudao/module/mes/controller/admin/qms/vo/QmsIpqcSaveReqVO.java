package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - IPQC过程抽检新增/修改 Request VO")
@Data
public class QmsIpqcSaveReqVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "IPQC过程巡检单号，不传则系统生成")
    private String ipqcNo;

    @Schema(description = "生产工单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "生产工单号不能为空")
    private String workOrderNo;

    @Schema(description = "生产计划/工序任务ID")
    private Long planOrderId;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "机台ID")
    private Long machineId;

    @Schema(description = "机台编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "机台编码不能为空")
    private String machineCode;

    @Schema(description = "机台名称")
    private String machineName;

    @Schema(description = "产品/物料ID")
    private Long materialId;

    @Schema(description = "产品/物料编码")
    private String materialCode;

    @Schema(description = "产品/物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "巡检类型")
    private String inspectionType;

    @Schema(description = "采用的检验标准ID")
    private Long standardId;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "应巡检时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime scheduledTime;

    @Schema(description = "下一次应巡检时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime nextInspectionTime;

    @Schema(description = "过程异常控制动作")
    private String controlAction;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "检验项明细；创建时由后端按IPQC检验标准生成，提交时承载样本结果")
    private List<@Valid IpqcItem> items;

    @Schema(description = "异常记录")
    private List<@Valid IpqcAbnormal> abnormals;

    @Data
    public static class IpqcItem {
        private Long id;
        private Long standardItemId;
        private String category;
        @NotBlank(message = "检验项目不能为空")
        private String inspectionItem;
        @NotBlank(message = "项目类型不能为空")
        private String itemType;
        private BigDecimal targetValue;
        private String standardDesc;
        private String inspectionMethod;
        private String testFrequencyJudgement;
        private String testTool;
        @NotNull(message = "取样数不能为空")
        @Positive(message = "取样数必须大于 0")
        private Integer sampleSize;
        private BigDecimal minValueLimit;
        private BigDecimal maxValueLimit;
        private BigDecimal maxValue;
        private BigDecimal minValue;
        private BigDecimal averageValue;
        private String itemResult;
        private Boolean isSpc;
        private Integer sort;
        private List<@Valid IpqcSample> samples;
    }

    @Data
    public static class IpqcSample {
        private Long id;
        @NotNull(message = "样本序号不能为空")
        private Integer sampleSeq;
        private String samplePosition;
        private BigDecimal measuredValue;
        private String qualitativeValue;
        private String sampleResult;
        private String defectCode;
        private String defectName;
        private String remark;
    }

    @Data
    public static class IpqcAbnormal {
        private Long id;
        private Long ipqcItemId;
        private Long sampleId;
        private String defectCode;
        private String defectName;
        @NotBlank(message = "异常描述不能为空")
        private String abnormalDesc;
        private String processStatus;
        private String actionRequired;
        private Long ncRecordId;
    }
}
