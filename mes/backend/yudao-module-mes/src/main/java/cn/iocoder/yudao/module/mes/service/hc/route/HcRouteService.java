package cn.iocoder.yudao.module.mes.service.hc.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRoutePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.route.vo.HcRouteSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteOperationDO;

import java.util.List;

public interface HcRouteService {
    Long createHcRoute(HcRouteSaveReqVO createReqVO);
    void updateHcRoute(HcRouteSaveReqVO updateReqVO);
    void deleteHcRoute(Long id);
    void deleteHcRouteListByIds(List<Long> ids);
    HcRouteDO getHcRoute(Long id);
    List<HcRouteDO> getHcRouteSimpleList();
    List<HcRouteDO> getHcRouteSimpleListByMaterialId(Long materialId);
    List<HcRouteDO> getHcRouteList(HcRoutePageReqVO reqVO);
    PageResult<HcRouteDO> getHcRoutePage(HcRoutePageReqVO pageReqVO);
    List<HcRouteOperationDO> getHcRouteOperationListByParentId(Long parentId);
}
