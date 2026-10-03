package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - HC 生产计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcPlanOrderPageReqVO extends PageParam {

    @Schema(description = "计划单号")
    private String planNo;

    @Schema(description = "关键词，匹配计划号、产品料号、产品型号、销售订单、路线或批号")
    private String keyword;

    @Schema(description = "计划状态列表")
    private List<String> planStatuses;

    @Schema(description = "排产模式")
    private String planMode;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "销售订单号")
    private String salesOrderNo;

    @Schema(description = "物料关键字，匹配编码或名称")
    private String materialKeyword;

    @Schema(description = "母料料号")
    private String motherMaterialCode;

    @Schema(description = "母料型号")
    private String motherModelCode;

    @Schema(description = "母卷批号，匹配父生产批号/生产批号/主批号")
    private String motherRollBatchNo;

    @Schema(description = "生产类型编码")
    private String prodType;

    @Schema(description = "物料类型编码")
    private String categoryCode;

    @Schema(description = "型号编码")
    private String modelCode;

    @Schema(description = "尺寸规格")
    private String sizeSpec;

    @Schema(description = "配方编码")
    private String recipeCode;

    @Schema(description = "工艺路线关键字，匹配编码或名称")
    private String routeKeyword;

    @Schema(description = "计划日期-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateStart;

    @Schema(description = "计划日期-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateEnd;

    @Schema(description = "生产开始日期-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionStartDateStart;

    @Schema(description = "生产开始日期-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionStartDateEnd;

    @Schema(description = "生产结束日期-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionEndDateStart;

    @Schema(description = "生产结束日期-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionEndDateEnd;

    @Schema(description = "创建时间-开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间-结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;

}
