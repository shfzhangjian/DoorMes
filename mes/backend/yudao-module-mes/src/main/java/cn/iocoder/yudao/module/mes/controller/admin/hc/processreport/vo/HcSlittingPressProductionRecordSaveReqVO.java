package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class HcSlittingPressProductionRecordSaveReqVO {
    private Long id;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "日期不能为空")
    private LocalDate reportDate;
    @NotBlank(message = "型号不能为空") private String modelCode;
    @NotBlank(message = "料号不能为空") private String materialCode;
    @NotBlank(message = "批号不能为空") private String batchNo;
    private BigDecimal slittingInputM;
    private Integer slittingOutputPcs;
    private BigDecimal pressSlotInputPcs;
    private BigDecimal pressSlotOutputPcs;
    private Integer rollerCleanAccumulatedPcs;
    private Integer rollerCleanUseDays;
    private Integer bearingReplaceAccumulatedPcs;
    private Integer bearingReplaceUseDays;
    @NotBlank(message = "记录人不能为空") private String recorderName;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @NotNull(message = "记录时间不能为空")
    private LocalDateTime recordTime;
    private String remark;
}
