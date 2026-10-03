package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - OQC出货检验新增/修改 Request VO")
@Data
public class QmsOqcSaveReqVO {

    private Long id;
    private String oqcNo;
    private Long shippingNoticeId;
    private Long shippingNoticeItemId;
    @NotBlank(message = "发货通知单号不能为空")
    private String shippingNo;
    private String noticeNo;
    private Long customerId;
    private String customerCode;
    @NotBlank(message = "客户名称不能为空")
    private String customerName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private String modelCode;
    private String productSize;
    @NotBlank(message = "出货批次不能为空")
    private String batchNo;
    private String customerBatchNo;
    private BigDecimal shippingQty;
    private BigDecimal shippingPieceQty;
    private String unitCode;
    private String unitName;
    private String aqlStandard;
    private Integer sampleQty;
    private Long standardId;
    private String status;
    private String judgment;
    private Long inspectorId;
    private String inspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime inspectionTime;
    private String releaseResult;
    private String relatedNcrNo;
    private String ncrStatus;
    private String entryMode;
    private String entryLayout;
    private String remark;
    private List<@Valid OqcItem> items;
    private List<@Valid OqcAbnormal> abnormals;

    @Data
    public static class OqcItem {
        private Long id;
        private Long standardItemId;
        private String category;
        @NotBlank(message = "检验项目不能为空")
        private String inspectionItem;
        @NotBlank(message = "项目类型不能为空")
        private String itemType;
        private BigDecimal targetValue;
        private String standardDesc;
        private String unit;
        private String ruleDescription;
        private String inspectionMethod;
        private String testFrequencyJudgement;
        private String valueTemplate;
        private String valueTemplateName;
        private String judgmentMetric;
        private String templateParams;
        private BigDecimal avgMinLimit;
        private BigDecimal avgMaxLimit;
        private BigDecimal stdMinLimit;
        private BigDecimal stdMaxLimit;
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
        private BigDecimal operatorMax;
        private BigDecimal operatorMin;
        private BigDecimal operatorAvg;
        private String operatorResult;
        private BigDecimal qaMax;
        private BigDecimal qaMin;
        private BigDecimal qaAvg;
        private String qaResult;
        private BigDecimal calculatedAvg;
        private BigDecimal calculatedStd;
        private BigDecimal calculatedMin;
        private BigDecimal calculatedMax;
        private Integer requiredSampleCount;
        private Integer completedSampleCount;
        private Integer abnormalSampleCount;
        private String inputStatus;
        private Boolean isSpc;
        private Integer sort;
        private List<@Valid OqcSample> samples;
    }

    @Data
    public static class OqcSample {
        private Long id;
        @NotNull(message = "样本序号不能为空")
        private Integer sampleSeq;
        private String samplePosition;
        private String sampleRole;
        private String rawValuesJson;
        private BigDecimal resultValue;
        private BigDecimal measuredValue;
        private String qualitativeValue;
        private String sampleResult;
        private Integer sampleGroupNo;
        private String valueSource;
        @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
        private LocalDateTime inputTime;
        private String remark;
    }

    @Data
    public static class OqcAbnormal {
        private Long id;
        private Long oqcItemId;
        private Long sampleId;
        private String abnormalRole;
        @NotBlank(message = "异常描述不能为空")
        private String abnormalDesc;
        private String processStatus;
        private String actionRequired;
        private Long ncRecordId;
        private String relatedNcrNo;
        private String dispositionStatus;
    }
}
