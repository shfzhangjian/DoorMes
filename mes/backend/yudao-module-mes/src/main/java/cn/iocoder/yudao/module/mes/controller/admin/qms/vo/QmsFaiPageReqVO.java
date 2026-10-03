package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - FAI首件检验单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsFaiPageReqVO extends PageParam {

    @Schema(description = "FAI首检单号")
    private String faiNo;

    @Schema(description = "生产工单号")
    private String workOrderNo;

    @Schema(description = "生产机台")
    private String machineCode;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "母料料号兼容字段；当前等同物料编码")
    private String productModel;

    @Schema(description = "产品批次")
    private String productBatchNo;

    @Schema(description = "胶板型号")
    private String glueBoardModel;

    @Schema(description = "胶板料号")
    private String glueBoardMaterialCode;

    @Schema(description = "胶板批次")
    private String gluePlateBatchNo;

    @Schema(description = "工序类别")
    private String processCategory;

    @Schema(description = "来源模块")
    private String sourceModule;

    @Schema(description = "排除来源模块")
    private String excludedSourceModule;

    @Schema(description = "送检类型")
    private String submissionType;

    @Schema(description = "湿法送样类型")
    private String wetSampleType;

    @Schema(description = "送检人员")
    private String submitterName;

    @Schema(description = "单据状态")
    private String status;

    @Schema(description = "判定结果")
    private String judgment;

    @Schema(description = "是否复检单")
    private Boolean recheckFlag;

    @Schema(description = "样本组复检审核过滤：WAIT_RECHECK=待复检，WAIT_AUDIT=复检待审")
    private String itemRecheckStatusFilter;

    @Schema(description = "留样状态；字典:mes_fai_retention_status")
    private String retentionStatus;

    @Schema(description = "留样销毁状态；字典:mes_fai_retention_destroy_status")
    private String retentionDestroyStatus;

    @Schema(description = "只查询未过期留样记录")
    private Boolean retentionActiveOnly;

    @Schema(description = "只查询已过期留样记录")
    private Boolean retentionExpiredOnly;

    @Schema(description = "触发原因")
    private String triggerReason;

    @Schema(description = "品质复核时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] qaTime;

    @Schema(description = "送检时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] submissionTime;

    @Schema(description = "检验时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] inspectionTime;

    @Schema(description = "留样过期时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] retentionExpireTime;
}
