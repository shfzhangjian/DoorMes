package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 新增磨皮生产记录时的耗材寿命默认快照。
 *
 * <p>来源为同设备、目标完工时间之前最近的一条有效生产记录；待确认记录允许作为来源，前端据
 * {@link #getSourceStatus()} 提示操作人。</p>
 */
@Schema(description = "管理后台 - 磨皮生产记录耗材默认快照 Response VO")
@Data
public class HcGrindingProductionRecordConsumableDefaultRespVO {

    private Long sourceRecordId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceCompletionTime;

    private String sourceStatus;
    private String sourceType;
    private BigDecimal sandpaperLife;
    private Integer sandpaperLifeDays;
    private String sandpaperBatchNo;
    private Integer guideClothLife;
    private String guideClothBatchNo;
}
