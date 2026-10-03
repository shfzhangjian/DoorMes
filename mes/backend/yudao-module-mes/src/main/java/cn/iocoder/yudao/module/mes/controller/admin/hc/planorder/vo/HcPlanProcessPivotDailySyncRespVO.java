package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产进度（日结版）同步 Response VO")
@Data
@Builder
public class HcPlanProcessPivotDailySyncRespVO {

    @Schema(description = "同步开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startDate;

    @Schema(description = "同步结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endDate;

    @Schema(description = "当前来源快照行数")
    private Integer sourceRowCount;

    @Schema(description = "内容未变化、无需写库的快照行数")
    private Integer alreadySettledRowCount;

    @Schema(description = "新增快照行数")
    private Integer insertedRowCount;

    @Schema(description = "内容变化并已更新的快照行数")
    private Integer updatedRowCount;

    @Schema(description = "业务来源已删除或失效的快照行数")
    private Integer removedRowCount;

    @Schema(description = "本次写入日结快照行数")
    private Integer snapshotRowCount;

    @Schema(description = "同步耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "同步完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime syncTime;
}
