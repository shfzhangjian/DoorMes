package cn.iocoder.yudao.module.mes.controller.admin.hc.processoutputbalance.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.SortingField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 工序产出物结存台账分页 Request VO")
@Data
public class HcProcessOutputBalancePageReqVO extends PageParam {

    @Schema(description = "关键词：计划号/批号/物料/型号/工序")
    private String keyword;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "工序编码")
    private String stageCode;

    @Schema(description = "结存状态：AVAILABLE/PARTIAL/CONSUMED/EMPTY")
    private String balanceStatus;

    @Schema(description = "产出批号")
    private String outputBatchNo;

    @Schema(description = "来源批号")
    private String sourceBatchNo;

    @Schema(description = "母批/上游批号")
    private String parentBatchNo;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "只显示仍有剩余的产出物")
    private Boolean onlyRemaining;

    @Schema(description = "报工确认时间-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDateStart;

    @Schema(description = "报工确认时间-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDateEnd;

    @Schema(description = "排序字段")
    private List<SortingField> sortingFields;

}
