package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - CMP软垫翘曲片号统计同步 Response VO")
@Data
@Builder
public class QmsCmpWarpageSliceStatSyncRespVO {

    private Integer requestedCount;
    private Integer syncedCount;
    private Integer sourceMissingCount;
    private Integer customerMissingCount;
    private Integer manualProtectedCount;
    private Integer invalidValueCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedTime;
}
