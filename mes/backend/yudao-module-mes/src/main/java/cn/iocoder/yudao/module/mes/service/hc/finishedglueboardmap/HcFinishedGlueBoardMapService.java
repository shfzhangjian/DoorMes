package cn.iocoder.yudao.module.mes.service.hc.finishedglueboardmap;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.finishedglueboardmap.vo.HcFinishedGlueBoardMapSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemDO;
import java.util.List;

public interface HcFinishedGlueBoardMapService {

    Long createHcFinishedGlueBoardMap(HcFinishedGlueBoardMapSaveReqVO createReqVO);

    void updateHcFinishedGlueBoardMap(HcFinishedGlueBoardMapSaveReqVO updateReqVO);

    void deleteHcFinishedGlueBoardMap(Long id);

    void deleteHcFinishedGlueBoardMapListByIds(List<Long> ids);

    HcFinishedGlueBoardMapDO getHcFinishedGlueBoardMap(Long id);

    PageResult<HcFinishedGlueBoardMapDO> getHcFinishedGlueBoardMapPage(HcFinishedGlueBoardMapPageReqVO pageReqVO);

    List<HcFinishedGlueBoardMapDO> getHcFinishedGlueBoardMapList(HcFinishedGlueBoardMapPageReqVO reqVO);

    List<HcFinishedGlueBoardMapItemDO> getItemListByMapId(Long mapId);

    List<HcFinishedGlueBoardMapItemDO> getMatchedItems(String productModelCode, String glueProcess);

    List<HcFinishedGlueBoardMapItemDO> getGlueBoardModelItems(String glueProcess, String glueBoardMaterialCode);

}
