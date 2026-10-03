package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单配置包 Response VO")
@Data
public class HcStationFormConfigPackageRespVO {

    @Schema(description = "配置包类型")
    private String packageType;

    @Schema(description = "配置包版本")
    private String packageVersion;

    @Schema(description = "导出时间")
    private String exportTime;

    @Schema(description = "导出来源说明")
    private String source;

    @Schema(description = "导入/预检状态，SUCCESS/ERROR")
    private String status = "SUCCESS";

    @Schema(description = "来源文件名")
    private String sourceFileName;

    @Schema(description = "表单总数")
    private Integer totalCount = 0;

    @Schema(description = "新增数")
    private Integer newCount = 0;

    @Schema(description = "更新数")
    private Integer updateCount = 0;

    @Schema(description = "无变化数")
    private Integer sameCount = 0;

    @Schema(description = "跳过数")
    private Integer skippedCount = 0;

    @Schema(description = "失败数")
    private Integer failureCount = 0;

    @Schema(description = "提示信息")
    private List<String> messages = new ArrayList<>();

    @Schema(description = "失败明细")
    private List<String> failures = new ArrayList<>();

    @Schema(description = "表单配置快照")
    private List<HcStationFormConfigPackageFormVO> forms = new ArrayList<>();
}
