package cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 工位记录详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcStationRecordDetailRespVO extends HcStationRecordRespVO {

    private List<HcStationRecordItemRespVO> items;
}
