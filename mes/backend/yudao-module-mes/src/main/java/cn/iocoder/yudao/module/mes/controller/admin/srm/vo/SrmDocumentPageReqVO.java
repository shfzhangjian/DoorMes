package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - SRM通用业务单据分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmDocumentPageReqVO extends PageParam {

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务单号")
    private String docNo;

    @Schema(description = "业务标题")
    private String title;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "物料/项目名称")
    private String materialName;

    @Schema(description = "业务状态")
    private String status;

    @Schema(description = "业务分类")
    private String bizCategory;

    @Schema(description = "业务等级")
    private String bizLevel;

    @Schema(description = "考核周期")
    private String periodType;

    @Schema(description = "考核年份")
    private Integer evalYear;

    @Schema(description = "考核季度")
    private Integer evalQuarter;

    @Schema(description = "扩展字段关键字")
    private String payloadKeyword;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] dueDate;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] applyTime;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
