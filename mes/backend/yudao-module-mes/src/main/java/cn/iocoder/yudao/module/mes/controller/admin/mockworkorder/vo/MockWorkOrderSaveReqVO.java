package cn.iocoder.yudao.module.mes.controller.admin.mockworkorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import jakarta.validation.constraints.*;

@Schema(description = "管理后台 - 模拟生产工单表（用于AI大模型MCP调用测试）新增/修改 Request VO")
@Data
public class MockWorkOrderSaveReqVO {

    @Schema(description = "工单流水号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18975")
    private Long id;

    @Schema(description = "生产工单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "生产工单号不能为空")
    private String orderNo;

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @NotEmpty(message = "产品名称不能为空")
    private String productName;

    @Schema(description = "排产数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排产数量不能为空")
    private Integer quantity;

    @Schema(description = "工单状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRAFT:草稿, DOING:生产中, DONE:已完成")
    @NotEmpty(message = "工单状态不能为空")
    private String status;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

}
