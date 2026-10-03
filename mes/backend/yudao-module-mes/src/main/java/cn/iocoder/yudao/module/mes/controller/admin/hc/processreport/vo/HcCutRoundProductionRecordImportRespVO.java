package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切生产记录导入 Response VO")
@Data
public class HcCutRoundProductionRecordImportRespVO {

    private Integer totalRows = 0;
    private Integer skippedRows = 0;
    private Integer createCount = 0;
    private Integer updateCount = 0;
    private Integer failureCount = 0;
    private List<String> messages = new ArrayList<>();
    private List<String> failures = new ArrayList<>();
}
