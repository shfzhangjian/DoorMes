package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 内包装单元片号明细 Response VO")
@Data
public class HcPackagingInnerUnitItemRespVO {

    private Long id;
    private Long innerUnitId;
    private String innerUnitNo;
    private Long sourceCutRoundReportId;
    private String sliceBatchNo;
    private String productionBatchNo;
    private String qualityStatus;
    private String scanUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;
}
