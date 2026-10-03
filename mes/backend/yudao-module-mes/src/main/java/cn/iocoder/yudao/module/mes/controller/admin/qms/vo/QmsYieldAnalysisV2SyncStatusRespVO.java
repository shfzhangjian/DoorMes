package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 良品率分析（新版）日结状态 Response VO")
@Data
public class QmsYieldAnalysisV2SyncStatusRespVO {

    @Schema(description = "日结数据中的最新实际报工日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate latestStatDate;

    @Schema(description = "最近日结同步时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime latestSyncTime;

    @Schema(description = "已结算实际报工来源记录数")
    private Long settledSourceCount;
}
