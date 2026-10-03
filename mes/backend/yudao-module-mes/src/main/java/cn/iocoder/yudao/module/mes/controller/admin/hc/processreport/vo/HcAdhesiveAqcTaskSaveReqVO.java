package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 粘胶AQC首检任务保存 Request VO")
@Data
public class HcAdhesiveAqcTaskSaveReqVO {

    private Long id;

    @NotBlank(message = "AQC任务类型不能为空")
    private String taskType;

    private Long planId;

    private Long planOperationId;

    private Long adhesiveReportId;
    private Long glueBoardUsageId;
    private String glueBoardMaterialCode;
    private String glueBoardBatchNo;

    @NotNull(message = "首检送检起位置不能为空")
    @DecimalMin(value = "0", message = "首检送检起位置不能为负数")
    private BigDecimal sampleStartPosition;

    @NotNull(message = "首检送检长度不能为空")
    @DecimalMin(value = "0.001", message = "首检送检长度必须大于0")
    private BigDecimal sampleLength;

    private Long submitterId;
    private String submitterName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime submitTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;
}
