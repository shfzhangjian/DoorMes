package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 内包装单元 Response VO")
@Data
public class HcPackagingInnerUnitRespVO {

    private Long id;
    private String innerUnitNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Integer packageSpec;
    private Integer targetQty;
    private Integer currentQty;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String batchNo;
    private String productSize;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate packageDate;

    private String labelNo;
    private String unitStatus;
    private Integer printCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;

    private String reviewerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;

    private Boolean backfillFlag;
    private String backfillReason;
    private String recorderName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recorderTime;

    private String remark;
    private List<HcPackagingInnerUnitItemRespVO> items;
}
