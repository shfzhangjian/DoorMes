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
public class HcGrindingProductionRecordSaveReqVO {
    private HcGrindingConsumptionVO consumption;

    private Long id;
    /** 仅在研发手工新增时由设备选择框提交；编辑时以后端已存快照为准。 */
    private Long equipmentId;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;
    @NotBlank(message = "型号不能为空")
    private String modelCode;
    @NotBlank(message = "类型不能为空")
    private String padType;
    @NotBlank(message = "料号不能为空")
    private String materialCode;
    @NotBlank(message = "批号不能为空")
    private String batchNo;
    private BigDecimal inputLength;
    private BigDecimal outputLength;
    @NotBlank(message = "磨皮次数不能为空")
    private String passType;
    private BigDecimal sandpaperLife;
    private Integer sandpaperLifeDays;
    private String sandpaperBatchNo;
    private Boolean sandpaperChanged;
    private String sandpaperReplaceReason;
    private Integer guideClothLife;
    private String guideClothBatchNo;
    private Boolean guideClothChanged;
    private String guideClothReplaceReason;
    private String replaceReason;
    @NotBlank(message = "记录人不能为空")
    private String recorderName;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @NotNull(message = "完工日期不能为空")
    private LocalDateTime recordTime;
    private String remark;
}
