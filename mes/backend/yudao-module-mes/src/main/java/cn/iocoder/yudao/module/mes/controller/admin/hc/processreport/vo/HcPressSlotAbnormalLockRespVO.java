package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 压槽异常锁定记录 Response VO")
@Data
public class HcPressSlotAbnormalLockRespVO {

    @Schema(description = "记录 ID，明细行取锁定明细 ID，汇总行取锁定主表 ID")
    private Long id;

    @Schema(description = "异常锁定主表 ID")
    private Long lockId;

    @Schema(description = "压槽报工记录 ID")
    private Long reportId;

    @Schema(description = "计划 ID")
    private Long planId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "工序 ID")
    private Long planOperationId;

    @Schema(description = "母批批号")
    private String motherBatchNo;

    @Schema(description = "片号")
    private String productionBatchNo;

    @Schema(description = "扫码确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    @Schema(description = "扫码确认人")
    private String confirmerName;

    @Schema(description = "锁定状态；LOCKED=锁定中，RELEASED=已放行")
    private String lockStatus;

    @Schema(description = "是否锁定中")
    private Boolean locked;

    @Schema(description = "锁定起点时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lockStartTime;

    @Schema(description = "异常加检 FAI ID")
    private Long abnormalFaiId;

    @Schema(description = "异常加检单号")
    private String abnormalFaiNo;

    @Schema(description = "异常加检片号")
    private String abnormalSampleBatchNo;

    @Schema(description = "异常检验结果")
    private String abnormalResult;

    @Schema(description = "异常送检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime abnormalSubmitTime;

    @Schema(description = "异常反馈时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime abnormalFeedbackTime;

    @Schema(description = "异常检验人")
    private String abnormalInspectorName;

    @Schema(description = "最新复检放行 FAI ID")
    private Long releaseFaiId;

    @Schema(description = "最新复检放行单号")
    private String releaseFaiNo;

    @Schema(description = "最新复检放行片号")
    private String releaseSampleBatchNo;

    @Schema(description = "最新复检放行结果")
    private String releaseResult;

    @Schema(description = "最新复检送检时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseSubmitTime;

    @Schema(description = "最新复检反馈时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseFeedbackTime;

    @Schema(description = "最新复检检验人")
    private String releaseInspectorName;

    @Schema(description = "放行意见")
    private String releaseOpinion;

    @Schema(description = "放行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;
}
