package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 留样异常复检锁定 Response VO")
@Data
public class QmsSampleAbnormalLockRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "锁定单号")
    private String lockNo;

    @Schema(description = "锁定状态")
    private String lockStatus;

    @Schema(description = "对象类型")
    private String objectType;

    @Schema(description = "母卷/分段号")
    private String objectNo;

    @Schema(description = "异常来源工序编码")
    private String sourceProcessCode;

    @Schema(description = "异常来源工序名称")
    private String sourceProcessName;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "计划工序ID")
    private Long planOperationId;

    @Schema(description = "工序编码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "母卷号")
    private String motherBatchNo;

    @Schema(description = "分段号")
    private String segmentNo;

    @Schema(description = "生产批号")
    private String productionBatchNo;

    @Schema(description = "异常检验单ID")
    private Long abnormalInspectionId;

    @Schema(description = "异常检验单号")
    private String abnormalInspectionNo;

    @Schema(description = "异常检验单类型")
    private String abnormalInspectionType;

    @Schema(description = "异常检验工序编码")
    private String abnormalOperationCode;

    @Schema(description = "异常检验工序名称")
    private String abnormalOperationName;

    @Schema(description = "异常母卷/分段号")
    private String abnormalObjectNo;

    @Schema(description = "异常结果")
    private String abnormalResult;

    @Schema(description = "异常送检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime abnormalSubmitTime;

    @Schema(description = "异常反馈时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime abnormalFeedbackTime;

    @Schema(description = "异常来源模块")
    private String abnormalSourceModule;

    @Schema(description = "异常来源报工ID")
    private Long abnormalSourceReportId;

    @Schema(description = "异常来源报工单号")
    private String abnormalSourceReportNo;

    @Schema(description = "复检检验单ID")
    private Long recheckInspectionId;

    @Schema(description = "复检检验单号")
    private String recheckInspectionNo;

    @Schema(description = "复检检验单类型")
    private String recheckInspectionType;

    @Schema(description = "复检检验工序编码")
    private String recheckOperationCode;

    @Schema(description = "复检检验工序名称")
    private String recheckOperationName;

    @Schema(description = "复检母卷/分段号")
    private String recheckObjectNo;

    @Schema(description = "复检结果")
    private String recheckResult;

    @Schema(description = "复检送检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recheckSubmitTime;

    @Schema(description = "复检反馈时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recheckFeedbackTime;

    @Schema(description = "复检来源模块")
    private String recheckSourceModule;

    @Schema(description = "复检来源报工ID")
    private Long recheckSourceReportId;

    @Schema(description = "复检来源报工单号")
    private String recheckSourceReportNo;

    @Schema(description = "复检次数")
    private Integer recheckCount;

    @Schema(description = "最后复检申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastRecheckApplyTime;

    @Schema(description = "锁定原因")
    private String lockReason;

    @Schema(description = "解锁时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;

    @Schema(description = "解锁原因")
    private String releaseReason;
}
