package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - NCR 处置执行通知 Response VO")
@Data
public class QmsNcDispositionNotifyRespVO {

    @Schema(description = "通知记录 ID")
    private Long id;

    @Schema(description = "NCR ID")
    private Long ncRecordId;

    @Schema(description = "NCR 单号")
    private String ncNo;

    @Schema(description = "处置执行单 ID")
    private Long executionId;

    @Schema(description = "处置执行单号")
    private String executionNo;

    @Schema(description = "来源节点编码")
    private String sourceNodeCode;

    @Schema(description = "来源节点名称")
    private String sourceNodeName;

    @Schema(description = "处置方式")
    private String dispositionType;

    @Schema(description = "通知人 ID")
    private Long notifyUserId;

    @Schema(description = "通知人名称")
    private String notifyUserName;

    @Schema(description = "通知状态")
    private String notifyStatus;

    @Schema(description = "通知时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime notifyTime;

    @Schema(description = "回复结论")
    private String replyConclusion;

    @Schema(description = "回复人 ID")
    private Long replyUserId;

    @Schema(description = "回复人名称")
    private String replyUserName;

    @Schema(description = "回复时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源类型名称")
    private String sourceTypeName;

    @Schema(description = "来源业务类型")
    private String sourceBizType;

    @Schema(description = "来源业务类型名称")
    private String sourceBizTypeName;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "发生时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime happenTime;

    @Schema(description = "发生工序")
    private String processName;

    @Schema(description = "发生部门")
    private String happenDeptName;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "批次号")
    private String lotNo;

    @Schema(description = "不良描述")
    private String defectName;

    @Schema(description = "不良数量")
    private BigDecimal defectQty;

    @Schema(description = "不合格等级")
    private String ncLevelName;

    @Schema(description = "NCR 状态")
    private String status;

    @Schema(description = "当前节点")
    private String currentNodeName;

    @Schema(description = "当前办理人")
    private String currentHandlerUserName;

    @Schema(description = "终审处置结论")
    private String finalDisposition;

    @Schema(description = "当前登录人是否可回复")
    private Boolean canReply;

    @Schema(description = "列表操作名称")
    private String listActionName;

}
