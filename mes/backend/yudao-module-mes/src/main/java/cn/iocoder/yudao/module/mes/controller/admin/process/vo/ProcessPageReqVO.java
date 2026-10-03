package cn.iocoder.yudao.module.mes.controller.admin.process.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES标准工序分页 Request VO")
@Data
public class ProcessPageReqVO extends PageParam {

    @Schema(description = "工序编码")
    private String code;

    @Schema(description = "工序名称", example = "李四")
    private String name;

    @Schema(description = "默认车间ID", example = "10939")
    private Long workshopId;

    @Schema(description = "车间编码")
    private String workshopCode;

    @Schema(description = "车间名称", example = "李四")
    private String workshopName;

    @Schema(description = "工序类型", example = "2")
    private String processType;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "状态", example = "2")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
