package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 量检具新增申请分页 Request VO")
@Data
public class QmsMeasureToolApplyPageReqVO extends PageParam {

    @Schema(description = "申请单号")
    private String applyNo;

    @Schema(description = "量检具名称")
    private String toolName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "申请部门")
    private String applyDepartment;

    @Schema(description = "申请人")
    private String applicantName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
