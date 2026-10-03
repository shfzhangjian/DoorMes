package cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.SortingField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 工序中间品库台账分页 Request VO")
@Data
public class HcIntermediateStockLedgerPageReqVO extends PageParam {

    @Schema(description = "关键词：计划号/批号/物料/型号/工序/流水")
    private String keyword;

    @Schema(description = "来源计划号")
    private String sourcePlanNo;

    @Schema(description = "工序编码/来源类型")
    private String sourceType;

    @Schema(description = "库存状态：AVAILABLE/LOCKED/FROZEN/CONSUMED")
    private String stockStatus;

    @Schema(description = "中间品批号")
    private String batchNo;

    @Schema(description = "来源批号")
    private String sourceBatchNo;

    @Schema(description = "母批/上游批号")
    private String sourceParentBatchNo;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "型号")
    private String modelNo;

    @Schema(description = "质量状态")
    private String qualityStatus;

    @Schema(description = "只显示可用数量大于 0 的库存")
    private Boolean onlyAvailable;

    @Schema(description = "最近过账时间-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate txnDateStart;

    @Schema(description = "最近过账时间-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate txnDateEnd;

    @Schema(description = "排序字段")
    private List<SortingField> sortingFields;

}
