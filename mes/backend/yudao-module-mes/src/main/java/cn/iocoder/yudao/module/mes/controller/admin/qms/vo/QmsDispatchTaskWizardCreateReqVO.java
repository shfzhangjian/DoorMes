package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 质量任务向导创建并下达 Request VO")
@Data
public class QmsDispatchTaskWizardCreateReqVO {

    @NotBlank(message = "建立方式不能为空")
    private String taskType;
    private String triggerSource;
    @NotBlank(message = "检验类型不能为空")
    private String checkType;
    private String sourceMode;
    private String objectMode;
    private String sampleSelectionMode;
    private Integer requiredSampleQty;
    private List<String> samplePieceNos;
    private String inspectionScene;
    private Long sourceExecutionId;
    private String sourceExecutionNo;
    private String rejectReason;
    private Long standardId;
    @NotEmpty(message = "至少选择一个检验项目")
    private List<Long> selectedItemIds;
    private List<QmsDispatchTaskItemSelectionReqVO> selectedScopes;

    private String batchNo;
    private String workOrderNo;
    private Long productModelId;
    private String productModelCode;
    private String productModelName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private Long operationId;
    private String operationCode;
    private String operationName;
    private BigDecimal checkQty;
    private String unit;
    private String receiptNo;
    private String supplierName;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate arrivalDate;

    @NotBlank(message = "任务要求不能为空")
    private String taskInstruction;

    @NotNull(message = "执行人不能为空")
    private Long assigneeUserId;
    @NotBlank(message = "执行人姓名不能为空")
    private String assigneeUserName;
    private Long assigneeDeptId;
    private String assigneeDeptName;
    private String priority;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @NotNull(message = "要求完成时间不能为空")
    private LocalDateTime requiredFinishTime;
}
