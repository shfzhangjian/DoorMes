package cn.iocoder.yudao.module.mes.controller.admin.mockworkorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 模拟生产工单表（用于AI大模型MCP调用测试） Response VO")
@Data
@ExcelIgnoreUnannotated
public class MockWorkOrderRespVO {

    @Schema(description = "工单流水号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18975")
    @ExcelProperty("工单流水号")
    private Long id;

    @Schema(description = "生产工单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("生产工单号")
    private String orderNo;

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "排产数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("排产数量")
    private Integer quantity;

    @Schema(description = "工单状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRAFT:草稿, DOING:生产中, DONE:已完成")
    @ExcelProperty(value = "工单状态", converter = DictConvert.class)
    @DictFormat("mes_order_status") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private String status;

    @Schema(description = "备注", example = "你说的对")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
