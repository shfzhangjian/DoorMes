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

@Schema(description = "管理后台 - 磨皮设备日点检保存 Request VO")
@Data
public class HcRoughConsoleDailyCheckSaveReqVO {

    private Long recordId;

    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;

    @NotNull(message = "记录日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    @NotBlank(message = "表单编码不能为空")
    private String formCode;
    private String result;
    private String inspectionResult;
    private String headerDataJson;
    private String formRemark;
    private String confirmRemark;
    private String recorder;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime recorderTime;

    private String confirmer;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmerTime;

    @Valid
    private List<HcWetPassWorkItemReqVO> details;
}
