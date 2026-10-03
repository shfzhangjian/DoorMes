package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 不合格品处理单 Base VO")
@Data
public class QmsNcRecordBaseVO {

    @Schema(description = "NCR单号")
    @ExcelProperty("NCR单号")
    private String ncNo;

    @Schema(description = "不合格类型：半成品、成品、客退品")
    @ExcelProperty("不合格类型")
    private String sourceType;

    @Schema(description = "不合格类型名称快照")
    @ExcelProperty("不合格类型名称")
    private String sourceTypeName;

    @Schema(description = "关联来源单据类型")
    private String sourceBizType;

    @Schema(description = "关联来源单据类型名称快照")
    private String sourceBizTypeName;

    @Schema(description = "来源对象ID")
    private Long sourceId;

    @Schema(description = "来源对象单号")
    @ExcelProperty("来源单号")
    private String sourceNo;

    @Schema(description = "重新发起来源 NCR 主键")
    private Long sourceNcRecordId;

    @Schema(description = "发生时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("发生时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime happenTime;

    @Schema(description = "关联派工细单ID")
    private Long subOrderId;

    @Schema(description = "工序ID")
    private Long processId;

    @Schema(description = "发生工序")
    @ExcelProperty("发生工序")
    private String processName;

    @Schema(description = "发生部门ID")
    private Long happenDeptId;

    @Schema(description = "发生部门")
    @ExcelProperty("发生部门")
    private String happenDeptName;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码")
    @ExcelProperty("物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    @ExcelProperty("物料名称")
    private String materialName;

    @Schema(description = "规格型号")
    @ExcelProperty("规格型号")
    private String specification;

    @Schema(description = "单位")
    private String unitCode;

    @Schema(description = "批次号")
    @ExcelProperty("批次号")
    private String lotNo;

    @Schema(description = "缺陷代码")
    @ExcelProperty("缺陷代码")
    private String defectCode;

    @Schema(description = "缺陷名称")
    @ExcelProperty("缺陷名称")
    private String defectName;

    @Schema(description = "不合格数量")
    @ExcelProperty("不合格数量")
    private BigDecimal defectQty;

    @Schema(description = "不合格等级")
    @ExcelProperty("不合格等级")
    private String ncLevel;

    @Schema(description = "不合格等级名称快照")
    @ExcelProperty("不合格等级名称")
    private String ncLevelName;

    @Schema(description = "责任部门编码，多个以逗号分隔")
    private String responsibilityDeptCodes;

    @Schema(description = "责任部门名称快照，多个以顿号分隔")
    @ExcelProperty("责任部门")
    private String responsibilityDeptNames;

    @Schema(description = "不合格说明")
    @ExcelProperty("不合格说明")
    private String ncDescription;

    @Schema(description = "原物料异常类别")
    private String rawMaterialAbnormalCategory;

    @Schema(description = "原物料异常类别名称快照")
    @ExcelProperty("原物料异常类别")
    private String rawMaterialAbnormalCategoryName;

    @Schema(description = "是否隔离")
    @ExcelProperty("是否隔离")
    private Boolean isolatedFlag;

    @Schema(description = "NCR状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "BPM流程实例编号")
    private String processInstanceId;

    @Schema(description = "当前节点编码")
    private String currentNodeCode;

    @Schema(description = "当前节点")
    @ExcelProperty("当前节点")
    private String currentNodeName;

    @Schema(description = "当前处理人ID")
    private Long currentHandlerUserId;

    @Schema(description = "当前处理人")
    @ExcelProperty("当前处理人")
    private String currentHandlerUserName;

    @Schema(description = "发起人ID")
    private Long applicantUserId;

    @Schema(description = "发起人")
    @ExcelProperty("发起人")
    private String applicantUserName;

    @Schema(description = "发起部门ID")
    private Long applicantDeptId;

    @Schema(description = "发起部门")
    private String applicantDeptName;

    @Schema(description = "再次确认人ID")
    private Long contentConfirmUserId;

    @Schema(description = "再次确认人")
    @ExcelProperty("再次确认人")
    private String contentConfirmUserName;

    @Schema(description = "再次确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @ExcelProperty("再次确认时间")
    private LocalDateTime contentConfirmTime;

    @Schema(description = "品质确认人ID")
    private Long qualityConfirmUserId;

    @Schema(description = "品质确认人")
    @ExcelProperty("品质确认人")
    private String qualityConfirmUserName;

    @Schema(description = "品质确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @ExcelProperty("品质确认时间")
    private LocalDateTime qualityConfirmTime;

    @Schema(description = "兼容旧MRB决策")
    private String mrbDecision;

    @Schema(description = "终审处置结论")
    @ExcelProperty("处置结论")
    private String finalDisposition;

    @Schema(description = "终审意见")
    private String finalOpinion;

    @Schema(description = "最终处置说明")
    private String finalDisposeDescription;

    @Schema(description = "终审人ID")
    private Long finalApproverId;

    @Schema(description = "终审人")
    private String finalApproverName;

    @Schema(description = "终审时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime finalApproveTime;

    @Schema(description = "库存处置状态")
    @ExcelProperty("库存处置状态")
    private String stockDisposeStatus;

    @Schema(description = "处置数量")
    @ExcelProperty("处置数量")
    private BigDecimal stockDisposeQty;

    @Schema(description = "库存处置人ID")
    private Long stockDisposeUserId;

    @Schema(description = "库存处置人")
    private String stockDisposeUserName;

    @Schema(description = "库存处置结果/意见")
    private String stockDisposeResult;

    @Schema(description = "库存处置时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime stockDisposeTime;

    @Schema(description = "效果确认")
    private String effectConfirmResult;

    @Schema(description = "效果确认人ID")
    private Long effectConfirmUserId;

    @Schema(description = "效果确认人")
    private String effectConfirmUserName;

    @Schema(description = "效果确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime effectConfirmTime;

    @Schema(description = "主关联异常单号")
    @ExcelProperty("关联异常单")
    private String relatedExceptionNo;

    @Schema(description = "主关联异常事件ID")
    private Long relatedExceptionId;

    @Schema(description = "终审是否新建异常事件")
    private Boolean createExceptionFlag;

    @Schema(description = "主关联8D单号")
    @ExcelProperty("关联8D")
    private String related8dNo;

    @Schema(description = "关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("关闭时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime closeTime;

    @Schema(description = "关闭人ID")
    private Long closeUserId;

    @Schema(description = "关闭人")
    private String closeUserName;

    @Schema(description = "备注")
    private String remark;
}
