package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单 Excel 导入确认 Request VO")
@Data
public class HcStationFormImportConfirmReqVO {

    @Schema(description = "导入解析后的动态表单")
    @NotNull(message = "导入表单不能为空")
    @Valid
    private HcStationFormSaveReqVO form;

    @Schema(description = "是否覆盖同编码已有表单")
    private Boolean overwriteExisting;
}
