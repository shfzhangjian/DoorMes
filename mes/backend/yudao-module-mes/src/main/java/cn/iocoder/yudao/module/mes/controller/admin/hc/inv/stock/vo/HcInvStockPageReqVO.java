package cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 实时库存余额分页 Request VO")
@Data
public class HcInvStockPageReqVO extends PageParam {

    @Schema(description = "仓库编码")
    private String warehouseCode;

    @Schema(description = "库存类型：WIP/FG")
    private String stockType;

    @Schema(description = "WIP 来源类型")
    private String sourceType;

    @Schema(description = "排除的 WIP 来源类型")
    private List<String> excludeSourceTypes;

    @Schema(description = "来源计划号")
    private String sourcePlanNo;

    @Schema(description = "来源半成品批号")
    private String sourceBatchNo;

    @Schema(description = "来源上游批号/母批号")
    private String sourceParentBatchNo;

    @Schema(description = "配方编码")
    private String recipeCode;

    @Schema(description = "尺寸规格")
    private String specSize;

    @Schema(description = "所在工序")
    private Integer opSeq;

    @Schema(description = "排除的工序编码")
    private List<String> excludeOpCodes;

    @Schema(description = "排除的工序名称关键词")
    private List<String> excludeOpNameKeywords;

    @Schema(description = "段位编码")
    private String segmentCode;

    @Schema(description = "关键词：批次/物料/型号")
    private String keyword;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "型号")
    private String modelNo;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "质量状态")
    private String qualityStatus;

    @Schema(description = "业务状态")
    private String bizStatus;

    @Schema(description = "生产日期-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDateStart;

    @Schema(description = "生产日期-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDateEnd;

    @Schema(description = "是否包含可用量为0但仍有在库量的库存")
    private Boolean includeUnavailable;

    @Schema(description = "是否仅查询可利库量大于0的库存")
    private Boolean onlyShareable;

}
