package cn.iocoder.yudao.module.mes.controller.admin.unit.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES计量单位分页 Request VO")
@Data
public class UnitPageReqVO extends PageParam {

    @Schema(description = "单位符号")
    private String code;

    @Schema(description = "单位名称", example = "芋艿")
    private String name;

    @Schema(description = "维度")
    private String category;

    @Schema(description = "基准单位")
    private Boolean base;

    @Schema(description = "换算率")
    private BigDecimal ratio;

    @Schema(description = "保留小数位数")
    private Integer precision;

    @Schema(description = "状态", example = "2")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
