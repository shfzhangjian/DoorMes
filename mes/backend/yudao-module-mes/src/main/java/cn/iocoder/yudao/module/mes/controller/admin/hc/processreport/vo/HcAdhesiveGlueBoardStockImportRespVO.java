package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 胶板边库库存导入 Response VO")
@Data
public class HcAdhesiveGlueBoardStockImportRespVO {

    @Schema(description = "读取数据行数")
    private Integer totalRows = 0;

    @Schema(description = "跳过空行数")
    private Integer skippedRows = 0;

    @Schema(description = "成功初始化库存数")
    private Integer successCount = 0;

    @Schema(description = "物理删除库存数")
    private Integer clearedStockCount = 0;

    @Schema(description = "失败数")
    private Integer failureCount = 0;

    @Schema(description = "提示信息")
    private List<String> messages = new ArrayList<>();

    @Schema(description = "失败明细")
    private List<String> failures = new ArrayList<>();
}
