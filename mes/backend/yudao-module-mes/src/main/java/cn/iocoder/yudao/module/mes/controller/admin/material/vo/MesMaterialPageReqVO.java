package cn.iocoder.yudao.module.mes.controller.admin.material.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES物料主数据分页 Request VO")
@Data
public class MesMaterialPageReqVO extends PageParam {

    @Schema(description = "物料编码 (ERP码)")
    private String code;

    @Schema(description = "物料名称", example = "李四")
    private String name;

    @Schema(description = "物料分类")
    private String category;

    @Schema(description = "物料来源")
    private String materialSource;

    @Schema(description = "材质牌号")
    private String materialGrade;

    @Schema(description = "规格型号")
    private String spec;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "单重 (kg) ")
    private BigDecimal unitWeight;

    @Schema(description = "理论废品率 (%)")
    private BigDecimal scrapRate;

    @Schema(description = "默认供应商关联名称", example = "xxx供应商")
    private String supplierName;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
