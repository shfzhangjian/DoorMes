package cn.iocoder.yudao.module.mes.service.hc.inventoryanalysis;

import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewRespVO;

/**
 * 库存分析报表 Service。
 */
public interface HcInventoryAnalysisService {

    /**
     * 查询当前库存结构及出入库变化趋势。
     */
    HcInventoryAnalysisOverviewRespVO getOverview(HcInventoryAnalysisOverviewReqVO reqVO);

}
