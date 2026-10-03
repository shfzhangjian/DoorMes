package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单配置包确认导入 Request VO")
@Data
public class HcStationFormConfigPackageImportReqVO {

    @Schema(description = "配置包预检数据")
    @Valid
    @NotNull(message = "配置包不能为空")
    private HcStationFormConfigPackageRespVO configPackage;

    @Schema(description = "是否覆盖同编码已有配置")
    private Boolean overwriteExisting;
}
