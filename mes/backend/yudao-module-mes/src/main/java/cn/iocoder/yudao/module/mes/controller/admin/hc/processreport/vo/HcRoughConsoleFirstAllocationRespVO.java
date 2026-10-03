package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮一磨前置分配 Response VO")
@Data
public class HcRoughConsoleFirstAllocationRespVO {
    private HcGrindingConsumptionVO consumption;


    private Long id;
    private Long firstDetailId;
    private String segmentMark;
    private String productionBatchNo;
    private BigDecimal startPosition;
    private BigDecimal confirmedLength;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private Long sandpaperStateId;
    private BigDecimal sandpaperLife;
    private String sandpaperBatchNo;
    private Long guideClothStateId;
    private Integer guideClothLife;
    private String guideClothBatchNo;
    private Long checkRecordId;
    private Long operatorId;
    private String operatorName;
    private String detailStatus;
    private String remark;
}
