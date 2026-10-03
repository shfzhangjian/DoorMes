package cn.iocoder.yudao.module.mes.service.hc.stationrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordSaveReqVO;

public interface HcStationRecordQueryService {

    PageResult<HcStationRecordRespVO> getStationRecordPage(HcStationRecordPageReqVO reqVO);

    HcStationRecordDetailRespVO getStationRecordDetail(Long id);

    void updateStationRecord(HcStationRecordSaveReqVO reqVO);
}
