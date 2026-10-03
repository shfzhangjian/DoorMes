package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS异常事件 Base VO")
@Data
public class QmsExceptionEventBaseVO {

    @Schema(description = "异常单号")
    @ExcelProperty("异常单号")
    private String exceptionNo;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源对象ID")
    private Long sourceId;

    @Schema(description = "来源对象单号")
    private String sourceNo;

    @Schema(description = "异常类别")
    @ExcelProperty("异常类别")
    private String exceptionType;

    @Schema(description = "异常等级")
    @ExcelProperty("异常等级")
    private String exceptionLevel;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "BPM流程实例编号")
    private String processInstanceId;

    @Schema(description = "当前环节编码")
    private String currentNodeCode;

    @Schema(description = "当前环节")
    @ExcelProperty("当前环节")
    private String currentNodeName;

    @Schema(description = "当前处理人ID")
    private Long currentHandlerUserId;

    @Schema(description = "当前处理人")
    @ExcelProperty("当前处理人")
    private String currentHandlerUserName;

    @Schema(description = "发现部门ID")
    private Long discoverDeptId;

    @Schema(description = "发现部门编码")
    private String discoverDeptCode;

    @Schema(description = "发现部门")
    @ExcelProperty("发现部门")
    private String discoverDeptName;

    @Schema(description = "发现人ID")
    private Long discovererId;

    @Schema(description = "发现人工号/编码")
    private String discovererCode;

    @Schema(description = "发现人")
    @ExcelProperty("发现人")
    private String discovererName;

    @Schema(description = "发现时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("发现时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime discoverTime;

    @Schema(description = "确认部门ID")
    private Long confirmDeptId;

    @Schema(description = "确认部门编码")
    private String confirmDeptCode;

    @Schema(description = "确认部门")
    private String confirmDeptName;

    @Schema(description = "确认人ID")
    private Long confirmerId;

    @Schema(description = "确认人工号/编码")
    private String confirmerCode;

    @Schema(description = "确认人")
    private String confirmerName;

    @Schema(description = "确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmTime;

    @Schema(description = "是否关联产品质量")
    @ExcelProperty("是否关联产品质量")
    private Boolean isRelatedProduct;

    @Schema(description = "主要关联NCR单号")
    @ExcelProperty("关联NCR")
    private String relatedNcrNo;

    @Schema(description = "主要关联8D单号")
    @ExcelProperty("关联8D")
    private String related8dNo;

    @Schema(description = "异常描述")
    @ExcelProperty("异常描述")
    private String description;

    @Schema(description = "初步影响范围")
    private String initialImpact;

    @Schema(description = "临时小组负责人ID")
    private Long containmentOwnerId;

    @Schema(description = "临时小组负责人")
    private String containmentOwnerName;

    @Schema(description = "围堵措施")
    private String containmentAction;

    @Schema(description = "48H围堵期限")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("围堵期限")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime containmentDeadline;

    @Schema(description = "围堵完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime containmentFinishTime;

    @Schema(description = "根因4M1E分类")
    private String rootCauseCategory;

    @Schema(description = "根因分析")
    private String rootCause;

    @Schema(description = "责任部门ID")
    private Long actionDeptId;

    @Schema(description = "责任部门")
    private String actionDeptName;

    @Schema(description = "责任人ID")
    private Long actionOwnerId;

    @Schema(description = "责任人")
    private String actionOwnerName;

    @Schema(description = "纠正预防措施")
    private String preventiveAction;

    @Schema(description = "执行结果上传人ID")
    private Long resultUploaderId;

    @Schema(description = "执行结果上传人")
    private String resultUploaderName;

    @Schema(description = "纠正预防措施执行成果")
    private String correctivePreventiveResult;

    @Schema(description = "执行结果上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime resultUploadTime;

    @Schema(description = "抄送人ID快照，逗号分隔")
    private String copyUserIds;

    @Schema(description = "抄送人名称快照，逗号分隔")
    private String copyUserNames;

    @Schema(description = "效果确认")
    private String effectConfirm;

    @Schema(description = "品质闭环确认是否有效")
    private Boolean qaConfirmValid;

    @Schema(description = "品质确认人ID")
    private Long qaConfirmerId;

    @Schema(description = "品质确认人")
    private String qaConfirmerName;

    @Schema(description = "完成/关闭时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("关闭时间")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime finishTime;

    @Schema(description = "关闭人ID")
    private Long closeUserId;

    @Schema(description = "关闭人")
    private String closeUserName;

    @Schema(description = "备注")
    private String remark;
}
