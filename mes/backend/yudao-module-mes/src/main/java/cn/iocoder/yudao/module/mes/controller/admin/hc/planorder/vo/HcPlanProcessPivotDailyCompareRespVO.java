package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产进度（日结版）与旧查询比对 Response VO")
@Data
@Builder
public class HcPlanProcessPivotDailyCompareRespVO {

    @Schema(description = "旧查询来源行数")
    private Integer sourceRowCount;

    @Schema(description = "日结快照行数")
    private Integer snapshotRowCount;

    @Schema(description = "完全一致行数")
    private Integer matchedRowCount;

    @Schema(description = "日结缺失行数")
    private Integer missingSnapshotCount;

    @Schema(description = "日结内容过期行数")
    private Integer staleSnapshotCount;

    @Schema(description = "日结多余行数")
    private Integer extraSnapshotCount;

    @Schema(description = "样例差异说明")
    private List<String> sampleMessages;

    @Schema(description = "比对完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime checkedTime;
}
