package cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

public class HcTestOuterPackagePrintVO {

    @Schema(description = "合格待包装段分页请求")
    @Data
    public static class TestOuterWaitSegmentPageReqVO extends PageParam {
        @Schema(description = "关键字，支持段号/片号/型号/料号")
        private String keyword;
        @Schema(description = "段号")
        private String segmentBatchNo;
    }

    @Schema(description = "合格待包装片号列表请求")
    @Data
    public static class TestOuterWaitPieceListReqVO {
        @Schema(description = "段号")
        private String segmentBatchNo;
        @Schema(description = "关键字，支持片号/型号/料号")
        private String keyword;
    }

    @Schema(description = "客户产品分页请求")
    @Data
    public static class TestOuterCustomerProductPageReqVO extends PageParam {
        @Schema(description = "关键字，支持客户/产品/尺寸/序号")
        private String keyword;
        @Schema(description = "标签类型")
        private String labelKind;
    }

    @Schema(description = "合格待包装段响应")
    @Data
    public static class TestOuterSegmentRespVO {
        @Schema(description = "段号")
        private String segmentBatchNo;
        @Schema(description = "片数")
        private Integer pieceCount;
        @Schema(description = "物料编码")
        private String materialCode;
        @Schema(description = "物料名称")
        private String materialName;
        @Schema(description = "产品型号")
        private String modelCode;
        @Schema(description = "最早生产日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDateStart;
        @Schema(description = "最晚生产日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDateEnd;
        @Schema(description = "最晚有效期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        @Schema(description = "质量状态")
        private String packagingQualityStatus;
        @Schema(description = "样例片号")
        private String sampleSliceBatchNo;
        @Schema(description = "样例片号列表")
        private List<String> sampleSliceBatchNos;
    }

    @Schema(description = "合格待包装片号响应")
    @Data
    public static class TestOuterPieceRespVO {
        @Schema(description = "来源类型")
        private String sourceType;
        @Schema(description = "来源ID")
        private Long sourceId;
        @Schema(description = "裁切报工ID")
        private Long sourceCutRoundReportId;
        @Schema(description = "历史补录片ID")
        private Long sourceManualPieceId;
        @Schema(description = "段号")
        private String segmentBatchNo;
        @Schema(description = "片号")
        private String sliceBatchNo;
        @Schema(description = "生产批号")
        private String productionBatchNo;
        @Schema(description = "母批/父批号")
        private String parentProductionBatchNo;
        @Schema(description = "物料编码")
        private String materialCode;
        @Schema(description = "物料名称")
        private String materialName;
        @Schema(description = "产品型号")
        private String modelCode;
        @Schema(description = "生产日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDate;
        @Schema(description = "有效期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        @Schema(description = "检验任务号")
        private String inspectionTaskNo;
        @Schema(description = "检验结果")
        private String inspectionResult;
        @Schema(description = "COA检验结果")
        private String coaInspectionResult;
        @Schema(description = "质量状态")
        private String packagingQualityStatus;
        @Schema(description = "检验时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionTime;
        @Schema(description = "备注")
        private String remark;
    }

    @Schema(description = "客户产品候选")
    @Data
    public static class TestOuterCustomerProductRespVO {
        @Schema(description = "行标识")
        private String rowKey;
        @Schema(description = "客户信息ID")
        private Long customerInfoId;
        @Schema(description = "产品明细ID")
        private Long productItemId;
        @Schema(description = "序号")
        private String serialNo;
        @Schema(description = "Excel来源行号")
        private Integer sourceRow;
        @Schema(description = "客户")
        private String customer;
        @Schema(description = "产品类型")
        private String productType;
        @Schema(description = "尺寸/mm")
        private String sizeMm;
        @Schema(description = "当前标签模板ID")
        private Long designId;
        @Schema(description = "当前标签图片ID")
        private String labelImageId;
        @Schema(description = "当前标签图片文件")
        private String labelImageFile;
        private String customerSideSize;
        private String customerSideMethod;
        private String shippingMethod;
        private String needPaperCoa;
        private String needEcoa;
        private String hasMark;
        private String deliveryNote;
        private String shipmentFilePackageMethod;
        private String customerSideTemplate;
        private String specialRemark;
    }

    @Schema(description = "打印设计稿摘要")
    @Data
    public static class TestOuterPrintDesignRespVO {
        private Long id;
        private String labelKind;
        private String labelName;
        private String templateSource;
        private BigDecimal widthMm;
        private BigDecimal heightMm;
        private Integer dpi;
        private String imageId;
        private String imageFile;
        private String designJson;
    }

    @Schema(description = "打印变量预检结果")
    @Data
    public static class TestOuterVariableCheckRespVO {
        private String fieldKey;
        private String fieldLabel;
        private Object value;
        private String source;
        private Boolean required;
        private String status;
        private String message;
    }

    @Schema(description = "单片打印负载")
    @Data
    public static class TestOuterPrintPayloadItemRespVO {
        private String requestId;
        private String sliceBatchNo;
        private String segmentBatchNo;
        private String labelKind;
        private String labelName;
        private String templateName;
        private Map<String, Object> data;
        private List<TestOuterVariableCheckRespVO> variableChecks;
    }

    @Schema(description = "外包装测试打印负载")
    @Data
    public static class TestOuterPrintPayloadRespVO {
        private TestOuterCustomerProductRespVO customerProduct;
        private TestOuterPrintDesignRespVO design;
        private Map<String, Object> rendererTemplate;
        private List<TestOuterPrintPayloadItemRespVO> items;
        private Integer errorCount;
        private Integer warningCount;
    }

}
