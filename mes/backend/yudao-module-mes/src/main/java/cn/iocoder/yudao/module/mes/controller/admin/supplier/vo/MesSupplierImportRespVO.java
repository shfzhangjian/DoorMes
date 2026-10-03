package cn.iocoder.yudao.module.mes.controller.admin.supplier.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 供应商名录导入 Response VO")
@Data
public class MesSupplierImportRespVO {

    @Schema(description = "读取的非空数据行数")
    private Integer totalRows = 0;

    @Schema(description = "跳过的空行数")
    private Integer skippedRows = 0;

    @Schema(description = "成功导入行数")
    private Integer successCount = 0;

    @Schema(description = "失败数量")
    private Integer failureCount = 0;

    @Schema(description = "提示信息")
    private List<String> messages = new ArrayList<>();

    @Schema(description = "失败明细")
    private List<String> failures = new ArrayList<>();

}
