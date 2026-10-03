package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单 Excel 导入 Response VO")
@Data
public class HcStationFormImportRespVO {

    @Schema(description = "导入状态，SUCCESS/ERROR")
    private String status = "SUCCESS";

    @Schema(description = "来源文件名")
    private String sourceFileName;

    @Schema(description = "来源 Sheet 名")
    private String sourceSheetName;

    @Schema(description = "读取数据行数")
    private Integer totalRows = 0;

    @Schema(description = "成功解析明细数")
    private Integer successCount = 0;

    @Schema(description = "失败数")
    private Integer failureCount = 0;

    @Schema(description = "警告数")
    private Integer warningCount = 0;

    @Schema(description = "是否存在同编码表单")
    private Boolean existing = false;

    @Schema(description = "同编码已有表单 ID")
    private Long existingId;

    @Schema(description = "解析后的动态表单")
    private HcStationFormSaveReqVO form;

    @Schema(description = "提示信息")
    private List<String> messages = new ArrayList<>();

    @Schema(description = "失败明细")
    private List<String> failures = new ArrayList<>();
}
