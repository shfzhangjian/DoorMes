package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 生产指令 Response VO")
@ExcelIgnoreUnannotated
@Data
public class HcProductionInstructionRespVO {

    @Schema(description = "主键")
    private Long id;

    @ExcelProperty("指令号")
    @Schema(description = "指令号")
    private String instructionNo;

    @Schema(description = "指令批次号")
    private String instructionBatchNo;

    @Schema(description = "父指令 ID")
    private Long parentInstructionId;

    @ExcelProperty("计划号")
    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "计划 ID")
    private Long planId;

    @Schema(description = "计划工序 ID")
    private Long planOperationId;

    @Schema(description = "标准工序 ID")
    private Long processId;

    @Schema(description = "标准工序编码")
    private String processCode;

    @Schema(description = "标准工序名称")
    private String processName;

    @ExcelProperty("工序编码")
    @Schema(description = "计划工序编码")
    private String operationCode;

    @ExcelProperty("工序")
    @Schema(description = "计划工序名称")
    private String operationName;

    @ExcelProperty("批次号")
    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "母批/生产批号")
    private String productionBatchNo;

    @Schema(description = "分段批号")
    private String segmentBatchNo;

    @Schema(description = "指令类型：DAILY/PAUSE/RESUME/CANCEL/CHANGEOVER")
    private String instructionType;

    @Schema(description = "作用范围：PLAN/OPERATION/SEGMENT")
    private String scopeType;

    @Schema(description = "接收人 ID 列表")
    private List<Long> recipientIds;

    @Schema(description = "接收人")
    private String recipientNames;

    @Schema(description = "兼容旧版接收状态，工序消息模式为空")
    private String recipientNotifyStatus;

    @Schema(description = "兼容旧版阅读/通知时间，工序消息模式为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recipientNotifyTime;

    @Schema(description = "兼容旧版未读标识，工序消息模式固定为 false")
    private Boolean unread;

    @ExcelProperty("指令内容")
    @Schema(description = "指令内容")
    private String instructionContent;

    @Schema(description = "换型前料号")
    private String beforeMaterialCode;

    @Schema(description = "换型目标料号")
    private String targetMaterialCode;

    @Schema(description = "换型前产品型号")
    private String beforeModelCode;

    @Schema(description = "换型目标产品型号")
    private String targetModelCode;

    @Schema(description = "换型目标片数")
    private Integer targetQty;

    @Schema(description = "换型已完成片数")
    private Integer completedQty;

    @Schema(description = "换型执行状态：PENDING/EXECUTING/COMPLETED")
    private String executeStatus;

    @Schema(description = "是否完成后自动恢复计划型号")
    private Boolean autoRestoreFlag;

    @Schema(description = "执行人 ID")
    private Long executeUserId;

    @Schema(description = "执行人")
    private String executeUserName;

    @Schema(description = "开始执行时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime executeStartTime;

    @Schema(description = "执行完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime executeEndTime;

    @Schema(description = "下达人 ID")
    private Long issuerId;

    @ExcelProperty("下达人")
    @Schema(description = "下达人")
    private String issuerName;

    @ExcelProperty("指令下达时间")
    @Schema(description = "指令下达时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime issuedTime;

    @Schema(description = "工序确认人 ID")
    private Long confirmerId;

    @ExcelProperty("工序确认人")
    @Schema(description = "工序确认人")
    private String confirmerName;

    @ExcelProperty("确认时间")
    @Schema(description = "确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    @Schema(description = "撤下人 ID")
    private Long revokedBy;

    @Schema(description = "撤下人")
    private String revokedByName;

    @Schema(description = "撤下时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime revokedTime;

    @Schema(description = "撤下原因")
    private String revokeReason;

    @ExcelProperty("状态")
    @Schema(description = "状态：ISSUED/CONFIRMED/REVOKED")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
