package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - CMP压槽工艺参数记录 Response VO")
@Data
public class HcPressSlotProcessParamRespVO {

    private Long id;
    private Long faiId;
    private Long templateId;
    private Map<String, Object> headerData;
    private Map<String, Object> runtimeSchema;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long changeoverInstructionId;
    private String formName;
    private String formType;
    private String formTypeName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    private String modelCode;
    private String materialCode;
    private String parentProductionBatchNo;
    private String motherBatchNo;
    private String productionBatchNo;
    private String firstInspectionSliceNo;
    private String firstInspectionResult;
    private String inspectionScene;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;

    private String temperature1;
    private String temperature2;
    private String temperature3;
    private String temperature4;
    private String temperature5;
    private String recorderName;
    private String remark;
    private String reportStatus;

    private String fillUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime fillTime;

    private String confirmUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    private String recordStatus;
    private String statusName;
    private String sourceExcel;
    private Map<String, Object> importAttachment;
    private List<Item> items;

    @Data
    public static class Item {

        private Long templateItemId;
        private String fieldKey;
        private String actualValue2;

        private Integer seq;
        private Integer sortNo;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate reportDate;

        private String itemCategory;
        private String itemName;
        private String standardValue;
        private String actualValue;
        private String checkResult;
        private String abnormalRemark;
        private String valueMode;
        private String dualLabel1;
        private String dualLabel2;
        private Boolean requiredFlag;
        private String modelCode;
        private String productionBatchNo;
        private String motherBatchNo;
        private BigDecimal widthM;
        private String thickness;
        private String environmentTemperature;
        private String environmentHumidity;
        private String pressSlotOrder;
        private String pressSlotSpeed;
        private String pressSlotSize;
        private String rollerGap;
        private String setTemperature;
        private String measuredTemperature1;
        private String measuredTemperature2;
        private String measuredTemperature3;
        private String measuredTemperature4;
        private String measuredTemperature5;
        private String startTime;
        private String endTime;
        private String recorderName;
        private String confirmerName;
        private String remark;
        private Long sourceReportId;
    }
}
