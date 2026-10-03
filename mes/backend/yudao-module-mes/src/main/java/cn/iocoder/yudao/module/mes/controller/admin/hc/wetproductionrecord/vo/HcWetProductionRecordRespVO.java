package cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 湿法生产记录 Response VO")
@Data
public class HcWetProductionRecordRespVO {

    private Long id;
    private String dataSource;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    private String modelCode;
    private String padType;
    private String materialCode;
    private String batchNo;
    private BigDecimal inputKg;
    private BigDecimal outputMeter;
    private String petModel;
    private String petBatchNo;
    private String guideClothBatchNo;
    private Integer guideClothUseCount;
    private String guideClothChanged;
    private String changeDesc;
    private String recorderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    private String confirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
