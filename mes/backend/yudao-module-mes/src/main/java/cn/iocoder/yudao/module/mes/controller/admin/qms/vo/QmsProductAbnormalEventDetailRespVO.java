package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 产品异常事件统一详情 Response VO")
@Data
public class QmsProductAbnormalEventDetailRespVO {

    @Schema(description = "前端行唯一键")
    private String eventKey;

    @Schema(description = "检验来源类型")
    private String sourceType;

    @Schema(description = "检验类型")
    private String inspectionType;

    @Schema(description = "检验单ID")
    private Long inspectionId;

    @Schema(description = "检验标准ID")
    private Long standardId;

    @Schema(description = "检验单号")
    private String inspectionNo;

    @Schema(description = "来源单号")
    private String sourceReportNo;

    @Schema(description = "生产工单")
    private String workOrderNo;

    @Schema(description = "工序")
    private String operationName;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "产品型号")
    private String productModel;

    @Schema(description = "产品批次")
    private String productBatchNo;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "检验数量")
    private BigDecimal inspectionQty;

    @Schema(description = "不合格数量")
    private BigDecimal unqualifiedQty;

    @Schema(description = "抽样数量")
    private Integer sampleQty;

    @Schema(description = "检验时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;

    @Schema(description = "送检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submissionTime;

    @Schema(description = "检验员")
    private String inspectorName;

    @Schema(description = "送检人")
    private String submitterName;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "检验不合格项总结")
    private String abnormalSummary;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "复检链路ID")
    private Long recheckGroupId;

    @Schema(description = "复检状态：NONE、RECHECKING、RECHECK_OK、RECHECK_NG")
    private String recheckStatus;

    @Schema(description = "复检状态名称")
    private String recheckStatusName;

    @Schema(description = "驳回复检次数")
    private Integer recheckCount;

    @Schema(description = "当前检验单轮次")
    private Integer recheckRoundNo;

    @Schema(description = "根检验单ID")
    private Long recheckRootInspectionId;

    @Schema(description = "根检验单号")
    private String recheckRootInspectionNo;

    @Schema(description = "上一轮检验单ID")
    private Long recheckPrevInspectionId;

    @Schema(description = "上一轮检验单号")
    private String recheckPrevInspectionNo;

    @Schema(description = "最新复检单ID")
    private Long recheckLatestInspectionId;

    @Schema(description = "最新复检单号")
    private String recheckLatestInspectionNo;

    @Schema(description = "最新复检结果")
    private String recheckResult;

    @Schema(description = "最近一次驳回说明")
    private String rejectReason;

    @Schema(description = "最近一次驳回人")
    private String rejectUserName;

    @Schema(description = "最近一次驳回时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime rejectTime;

    @Schema(description = "是否允许驳回复检")
    private Boolean canRejectRecheck;

    @Schema(description = "是否允许生成NCR")
    private Boolean canGenerateNcr;

    @Schema(description = "NCR生成状态：PENDING 待生成，GENERATED 已生成")
    private String ncrStatus;

    @Schema(description = "是否已生成NCR")
    private Boolean ncrGenerated;

    @Schema(description = "关联NCR ID")
    private Long ncrId;

    @Schema(description = "关联NCR单号")
    private String ncrNo;

    @Schema(description = "统一检验详情列表")
    private List<DetailItem> details;

    @Schema(description = "结构化不合格项摘要")
    private List<AbnormalItem> abnormalItems;

    @Schema(description = "管理后台 - 产品异常事件结构化不合格项 Response VO")
    @Data
    public static class AbnormalItem {

        @Schema(description = "异常对象编号，例如送检片号")
        private String targetNo;

        @Schema(description = "不合格检验项目")
        private String inspectionItem;
    }

    @Schema(description = "管理后台 - 产品异常事件统一详情行 Response VO")
    @Data
    public static class DetailItem {

        @Schema(description = "行号")
        private Integer rowNo;

        @Schema(description = "明细来源/分组")
        private String sectionName;

        @Schema(description = "检验项目")
        private String inspectionItem;

        @Schema(description = "是否复检片/明细")
        private Boolean recheckDetailFlag;

        @Schema(description = "是否复检项目/样本")
        private Boolean recheckItemFlag;

        @Schema(description = "项目类型")
        private String itemType;

        @Schema(description = "标准/要求")
        private String standardDesc;

        @Schema(description = "样本数")
        private Integer sampleSize;

        @Schema(description = "实测/统计值")
        private String measuredValue;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "判定结果")
        private String result;

        @Schema(description = "缺陷代码ID")
        private Long defectCodeId;

        @Schema(description = "缺陷代码")
        private String defectCode;

        @Schema(description = "缺陷名称")
        private String defectName;

        @Schema(description = "异常描述")
        private String abnormalDesc;

        @Schema(description = "检验员")
        private String inspectorName;

        @Schema(description = "检验时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionTime;

        @Schema(description = "备注")
        private String remark;
    }
}
