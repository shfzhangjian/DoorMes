package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 良品率分析（新版）日结同步 Response VO")
@Data
@Builder
public class QmsYieldAnalysisV2SyncRespVO {

    @Schema(description = "同步开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate startDate;

    @Schema(description = "同步结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate endDate;

    @Schema(description = "实际报工来源记录数")
    private Integer sourceRecordCount;

    @Schema(description = "内容未变化、无需写库的来源记录数")
    private Integer alreadySettledRecordCount;

    @Schema(description = "新增的来源记录数")
    private Integer insertedSourceCount;

    @Schema(description = "最新状态发生变化的来源记录数")
    private Integer updatedSourceCount;

    @Schema(description = "业务表已删除或失效的来源记录数")
    private Integer removedSourceCount;

    @Schema(description = "写入日结快照记录数")
    private Integer snapshotRecordCount;

    @Schema(description = "匹配到的母卷统计透视行数")
    private Integer pivotRowCount;

    @Schema(description = "内容变化并已刷新的母卷透视行数")
    private Integer refreshedPivotRowCount;

    @Schema(description = "同步耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "同步完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime syncTime;
}
