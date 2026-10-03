package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 边库耗材领用台账导入 Response VO")
@Data
public class HcToolingConsumableLedgerImportRespVO {

    @Schema(description = "读取数据行数")
    private Integer totalRows = 0;

    @Schema(description = "跳过空行数")
    private Integer skippedRows = 0;

    @Schema(description = "新增领用台账数")
    private Integer createdLedgerCount = 0;

    @Schema(description = "更新领用台账数")
    private Integer updatedLedgerCount = 0;

    @Schema(description = "新增消耗明细数")
    private Integer createdConsumeCount = 0;

    @Schema(description = "更新消耗明细数")
    private Integer updatedConsumeCount = 0;

    @Schema(description = "失败数")
    private Integer failureCount = 0;

    @Schema(description = "提示信息")
    private List<String> messages = new ArrayList<>();

    @Schema(description = "失败明细")
    private List<String> failures = new ArrayList<>();
}
