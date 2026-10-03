package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 裁切生产记录新增/修改 Request VO")
@Data
public class HcCutRoundProductionRecordSaveReqVO {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "日期不能为空")
    private LocalDate reportDate;

    @NotBlank(message = "型号不能为空")
    private String modelCode;

    @NotBlank(message = "生产批号不能为空")
    private String productionBatchNo;

    @NotBlank(message = "裁切尺寸不能为空")
    private String cutSizeMm;

    private BigDecimal inputQty;
    private BigDecimal outputQty;
    private String bladeModel;
    private String bladeBatchNo;
    private Integer bladeUseCount;
    private String feltModel;
    private String feltBatchNo;
    private Integer feltUseCount;
    private Integer feltUseDays;
    private String bladeReplaceReason;

    @NotBlank(message = "记录人不能为空")
    private String recorderName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    @NotNull(message = "记录时间不能为空")
    private LocalDateTime recordTime;

    private String remark;
}
