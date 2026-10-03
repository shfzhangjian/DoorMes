package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工位动态表单报工工序选项 Response VO")
@Data
public class HcStationFormProcessOptionRespVO {

    @Schema(description = "工序编码")
    private String value;

    @Schema(description = "工序名称")
    private String label;
}
