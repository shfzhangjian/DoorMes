package cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 工位动态表单详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcStationFormDetailRespVO extends HcStationFormRespVO {

    @Schema(description = "运行时预设头数据 JSON")
    private String presetHeaderDataJson;

    @Schema(description = "运行时预设明细")
    private List<HcStationFormItemRespVO> presetItems;

    @Schema(description = "模板明细")
    private List<HcStationFormItemRespVO> items;
}
