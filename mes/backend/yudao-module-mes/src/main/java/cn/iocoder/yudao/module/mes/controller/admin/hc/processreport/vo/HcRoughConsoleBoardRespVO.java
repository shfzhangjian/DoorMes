package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮操作看板 Response VO")
@Data
public class HcRoughConsoleBoardRespVO {

    private HcRoughReportTaskRespVO task;
    private List<HcWetPassWorkRespVO> dailyChecks;
    private List<HcRoughConsoleConsumableRespVO> consumables;
    private List<HcRoughConsoleSourceBalanceRespVO> sourceBalances;
    /** 一磨执行模式：新报工固定 FIRST_ALLOCATED；历史 ORIGINAL 仅用于查询兼容。 */
    private String allocationMode;
    /** 前端标准字段；与 allocationMode 同值，用于一磨执行模式。 */
    private String firstAllocationMode;
    /** 是否已经持久化内部一磨分段记录；首次实际分段操作时才落库。 */
    private Boolean firstAllocationModeLocked;
    /** FIRST_ALLOCATED 模式下由一磨确认的 P/Q/R/S/NONE 加工单元。 */
    private List<HcRoughConsoleFirstAllocationRespVO> firstAllocations;
    private List<HcRoughConsoleFirstReportRespVO> firstReports;
    private List<HcRoughConsoleSecondReportRespVO> secondReports;
    /** 一磨 P/Q/R/S/NONE 分段独立时间事实。 */
    private List<HcRoughConsoleSegmentTimingRespVO> firstSegmentTimings;
    /** 二磨 P/Q/R/S 独立时间事实，不关联一磨分段。 */
    private List<HcRoughConsoleSegmentTimingRespVO> secondSegmentTimings;
    private BigDecimal firstProcessLength;
    private BigDecimal firstLossLength;
    private BigDecimal firstOutputLength;
    private BigDecimal firstNapSampleLength;
    private BigDecimal secondProcessLength;
    private BigDecimal secondLossLength;
    private BigDecimal secondOutputLength;
    private BigDecimal secondNapSampleLength;
    private BigDecimal secondResearchConsumptionLength;
    private BigDecimal pendingSecondLength;
    private Boolean dailyStartupDone;
    private Boolean dailyCleaningDone;
}
