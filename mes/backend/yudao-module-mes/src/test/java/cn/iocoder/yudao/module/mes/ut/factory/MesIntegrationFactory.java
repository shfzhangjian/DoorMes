// backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/ut/factory/MesIntegrationFactory.java
package cn.iocoder.yudao.module.mes.ut.factory;

import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import lombok.Builder;
import lombok.Data;

/**
 * [集成工厂] 场景编排者
 * 用途：调用各个原子工厂，组装复杂的业务场景（如：混合制造流）
 */
public class MesIntegrationFactory {

    /**
     * 场景数据包：包含运行一个测试场景所需的所有 DO 对象
     */
    @Data
    @Builder
    public static class HybridFlowScenario {
        private MesMaterialDO material;
        private ProcessDO process;
        private RouteDO route;
        private MesWorkOrderDO workOrder;
    }

    /**
     * 组装场景 2: T01 线边库拉动 (MTO)
     */
    public static HybridFlowScenario buildScenarioMtoPull() {
        return HybridFlowScenario.builder()
                .material(MaterialDataFactory.buildMaterialSlitRoll())
                .process(ProcessDataFactory.buildProcessDieCut())
                .route(ProcessDataFactory.buildRouteFilmStd())
                .workOrder(WorkOrderDataFactory.buildWorkOrderMto())
                .build();
    }
}
