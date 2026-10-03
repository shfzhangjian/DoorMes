package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - NCR办理 Request VO")
@Data
public class QmsNcRecordHandleReqVO {

    @Schema(description = "NCR ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "NCR ID不能为空")
    private Long id;

    @Schema(description = "办理意见")
    private String opinion;

    @Schema(description = "再次确认节点可修改的不合格说明")
    private String ncDescription;

    @Schema(description = "下一处理人ID")
    private Long nextHandlerUserId;

    @Schema(description = "下一处理人名称")
    private String nextHandlerUserName;

    @Schema(description = "下一处理人ID列表")
    private List<Long> nextHandlerUserIds;

    @Schema(description = "下一处理人名称列表")
    private List<String> nextHandlerUserNames;

    @Schema(description = "原材料品质转办办理方式：FINAL_APPROVAL-提交终审，DISPOSITION_EXECUTION-处置执行分派，DIRECT_CLOSE-直接关闭")
    private String transferRoute;

    @Schema(description = "品质转办人填写的处置选项；直接分派执行或直接关闭时必填")
    private String finalDisposition;

    @Schema(description = "品质转办人填写的终审意见；直接分派执行或直接关闭时必填")
    private String finalOpinion;

    @Schema(description = "品质转办人选择是否新建并挂接异常事件")
    private Boolean createExceptionFlag;

    @Schema(description = "处置数量")
    private BigDecimal stockDisposeQty;

    @Schema(description = "品质转办人填写的具体措施")
    private String finalDisposeDescription;

    @Schema(description = "处置执行分派通知人ID列表")
    private List<Long> dispositionNotifyUserIds;

    @Schema(description = "处置执行分派通知人名称列表")
    private List<String> dispositionNotifyUserNames;

    @Schema(description = "MRB会签明细")
    private List<QmsNcMrbReviewReqVO> reviews;
}
