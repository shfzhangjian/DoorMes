package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 包装来源裁切片号 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcPackagingSourceRespVO {

    private Long sourceCutRoundReportId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String sliceBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String productSize;
    private String qualityStatus;
    private String reportStatus;
    private String packageStatus;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;
}
