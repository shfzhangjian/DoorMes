package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 湿法过站工作填写/确认 Request VO")
@Data
public class HcWetPassWorkSaveReqVO {

    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @Schema(description = "表单编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "表单编码不能为空")
    private String formCode;

    @Schema(description = "执行结果")
    private String result;

    @Schema(description = "检验结果")
    private String inspectionResult;

    @Schema(description = "主表单数据JSON")
    private String headerDataJson;

    @Schema(description = "执行备注")
    private String formRemark;

    @Schema(description = "记录人")
    private String recorder;

    @Schema(description = "记录时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime recorderTime;

    @Schema(description = "确认人")
    private String confirmer;

    @Schema(description = "确认时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmerTime;

    @Schema(description = "确认备注")
    private String confirmRemark;

    @Schema(description = "记录日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    @Schema(description = "机台ID")
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;

    @Schema(description = "明细")
    @Valid
    private List<HcWetPassWorkItemReqVO> details;
}
