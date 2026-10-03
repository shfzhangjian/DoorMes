package cn.iocoder.yudao.module.mes.controller.admin.plan.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 生产计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PlanPageReqVO extends PageParam {

    @Schema(description = "计划单号")
    private String planNo;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "来源")
    private String fromSource;

    @Schema(description = "开始日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] startDate;
}
