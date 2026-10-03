// backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/ut/factory/ProcessDataFactory.java
package cn.iocoder.yudao.module.mes.ut.factory;

import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO;
import static cn.iocoder.yudao.module.mes.ut.MesTestConstants.*;

/**
 * [原子工厂] 工艺与工序数据
 * 数据源: 01_SOP_Master.csv, 15_Route.csv
 */
public class ProcessDataFactory {

    // ========== Process (工序) ==========

    /**
     * T01 (Film): 模切工序 (离散)
     */
    public static ProcessDO buildProcessDieCut() {
        return ProcessDO.builder()
                .id(101L)
                .tenantId(TENANT_ID_FILM)
                .code("PROCESS_DIE_CUT")
                .name("模切")
                .processType("manufacture")
                .workshopId(201L)
                .workshopCode("WS_DIE_01")
                .workshopName("模切一车间")
                .bindStation(true)
                .status(0)
                .build();
    }

    // ========== Route (工艺路线) ==========

    /**
     * T01 (Film): 标准薄膜工艺
     */
    public static RouteDO buildRouteFilmStd() {
        return RouteDO.builder()
                .id(202L)
                .tenantId(TENANT_ID_FILM)
                .code("RT_FILM_STD")
                .name("标准薄膜工艺")
                .status(0)
                .build();
    }
}
