package cn.iocoder.yudao.module.mes.controller.admin.mockworkorder.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 模拟生产工单表（用于AI大模型MCP调用测试）分页 Request VO")
@Data
public class MockWorkOrderPageReqVO extends PageParam {

    @Schema(description = "生产工单号")
    private String orderNo;

    @Schema(description = "产品名称", example = "赵六")
    private String productName;

    @Schema(description = "排产数量")
    private Integer quantity;

    @Schema(description = "工单状态", example = "DRAFT:草稿, DOING:生产中, DONE:已完成")
    private String status;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
