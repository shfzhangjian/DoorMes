package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 边库耗材消耗明细分页 Request VO")
@Data
public class HcToolingConsumableConsumePageReqVO extends PageParam {

    @Schema(description = "领用台账ID")
    private Long ledgerId;

    @Schema(description = "耗材种类")
    private String consumableType;

    @Schema(description = "工序")
    private String processCode;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "耗材批次号")
    private String batchNo;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "生产批次号")
    private String productionBatchNo;

    @Schema(description = "研发产品型号")
    private String productModelCode;

    @Schema(description = "研发产品料号")
    private String productMaterialCode;

    @Schema(description = "研发产品批号")
    private String productBatchNo;

    @Schema(description = "消耗开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime consumeTimeStart;

    @Schema(description = "消耗结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime consumeTimeEnd;

    @Schema(description = "记录开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeStart;

    @Schema(description = "记录结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;
}
