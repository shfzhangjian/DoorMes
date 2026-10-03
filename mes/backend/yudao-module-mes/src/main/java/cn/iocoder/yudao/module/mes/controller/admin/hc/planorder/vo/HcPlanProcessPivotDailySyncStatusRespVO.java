package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产进度（日结版）同步状态 Response VO")
@Data
public class HcPlanProcessPivotDailySyncStatusRespVO {

    @Schema(description = "日结数据中的最新统计日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate latestStatDate;

    @Schema(description = "最近同步时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime latestSyncTime;

    @Schema(description = "已物化显示行数")
    private Long settledRowCount;
}
