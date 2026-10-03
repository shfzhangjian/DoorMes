package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单配置包表单项 VO")
@Data
public class HcStationFormConfigPackageFormVO {

    @Schema(description = "配置校验哈希")
    private String checksum;

    @Schema(description = "导入动作，NEW/UPDATE/SAME/INVALID")
    private String action;

    @Schema(description = "同编码已有表单 ID")
    private Long existingId;

    @Schema(description = "同编码已有表单名称")
    private String existingFormName;

    @Schema(description = "同编码已有表单校验哈希")
    private String existingChecksum;

    @Schema(description = "差异字段")
    private List<String> diffFields = new ArrayList<>();

    @Schema(description = "动态表单配置快照")
    private HcStationFormSaveReqVO form;
}
