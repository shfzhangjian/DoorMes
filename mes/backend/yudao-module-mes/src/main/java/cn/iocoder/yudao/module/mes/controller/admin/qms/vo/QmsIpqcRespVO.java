package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - IPQC过程抽检 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsIpqcRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;
    @ExcelProperty("IPQC过程巡检单号")
    private String ipqcNo;
    @ExcelProperty("生产工单号")
    private String workOrderNo;
    private Long planOrderId;
    private String operationCode;
    private String operationName;
    private Long machineId;
    @ExcelProperty("机台编码")
    private String machineCode;
    private String machineName;
    private Long materialId;
    @ExcelProperty("物料编码")
    private String materialCode;
    @ExcelProperty("物料名称")
    private String materialName;
    @ExcelProperty("规格型号")
    private String specification;
    @ExcelProperty("巡检类型")
    private String inspectionType;
    private Long standardId;
    private String standardNo;
    private String standardVersion;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("判定")
    private String judgment;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scheduledTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("巡检时间")
    private LocalDateTime inspectionTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime nextInspectionTime;
    private Long inspectorId;
    @ExcelProperty("巡检员")
    private String inspectorName;
    private String controlAction;
    private String machineControlResult;
    private Long ncRecordId;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    private List<IpqcItem> items;
    private List<IpqcAbnormal> abnormals;

    @Data
    public static class IpqcItem {
        private Long id;
        private Long ipqcId;
        private String ipqcNo;
        private Long standardItemId;
        private String category;
        private String inspectionItem;
        private String itemType;
        private BigDecimal targetValue;
        private String standardDesc;
        private String inspectionMethod;
        private String testFrequencyJudgement;
        private String testTool;
        private Integer sampleSize;
        private BigDecimal minValueLimit;
        private BigDecimal maxValueLimit;
        private BigDecimal maxValue;
        private BigDecimal minValue;
        private BigDecimal averageValue;
        private String itemResult;
        private Boolean isSpc;
        private Integer sort;
        private List<IpqcSample> samples;
    }

    @Data
    public static class IpqcSample {
        private Long id;
        private Long ipqcId;
        private Long ipqcItemId;
        private String ipqcNo;
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
        private Long ipqcId;
        private String ipqcNo;
        private Long ipqcItemId;
        private Long sampleId;
        private String defectCode;
        private String defectName;
        private String abnormalDesc;
        private String processStatus;
        private String actionRequired;
        private Long ncRecordId;
    }
}
