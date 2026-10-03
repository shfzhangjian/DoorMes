// 完整路径: cn.iocoder.yudao.module.mes.controller.admin.bom.vo.BomPageReqVO
package cn.iocoder.yudao.module.mes.controller.admin.bom.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工艺BOM主表分页 Request VO")
@Data
public class BomPageReqVO extends PageParam {

    @Schema(description = "所属物料ID", example = "12928")
    private Long productId;

    @Schema(description = "所属物料名称(模糊查询)", example = "螺丝")
    private String productName;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "是否当前版本")
    private Boolean active;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
