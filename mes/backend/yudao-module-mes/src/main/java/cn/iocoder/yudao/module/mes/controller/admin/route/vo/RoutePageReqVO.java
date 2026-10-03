package cn.iocoder.yudao.module.mes.controller.admin.route.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES工艺路线主分页 Request VO")
@Data
public class RoutePageReqVO extends PageParam {

    @Schema(description = "工艺路线编号")
    private String code;

    @Schema(description = "工艺路线名称", example = "王五")
    private String name;

    @Schema(description = "产品ID", example = "3204")
    private Long productId;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称", example = "张三")
    private String productName;

    @Schema(description = "产品规格")
    private String productSpec;

    @Schema(description = "产品单位")
    private String productUnit;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "默认路线")
    private Boolean active;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

    @Schema(description = "状态", example = "2")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
