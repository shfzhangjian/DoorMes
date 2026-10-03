// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/ut/MesIntegrationTestDataFactory.java
package cn.iocoder.yudao.module.mes.ut;

import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.route.RouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.iocoder.yudao.module.mes.ut.MesTestConstants.TENANT_ID_FILM;

/**
 * [集成测试] 数据工厂
 * 场景：Service 层业务流程验证 (Step 5+)
 * 特点：严格对应 CSV 仿真数据，保证主数据(Master Data)与事务数据(Transaction Data)的一致性
 */
public class MesIntegrationTestDataFactory {

    // ================== 1. 基础主数据构建 (Master Data) ==================

    /**
     * [Material] 构建: 分切子卷
     * 对应 CSV: 02_Material.csv -> M_SLIT_ROLL
     */
    public static MesMaterialDO buildMaterialSlitRoll() {
        return MesMaterialDO.builder()
                .id(1002L)
                .tenantId(TENANT_ID_FILM)
                .code("M_SLIT_ROLL")
                .name("分切子卷_1000m")
                .category("semi")
                .unit("roll")
                .spec("1000m*50um")
                .status(0)
                .build();
    }

    /**
     * [Process] 构建: 模切工序
     * 对应 CSV: 01_SOP_Master.csv -> 模切
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

    /**
     * [Route] 构建: 标准薄膜工艺
     * 用于关联工单
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

    // ================== 2. 生产工单构建 (Transaction Data) ==================

    /**
     * [WorkOrder] 构建: 场景2 - 以存定产
     * 对应 CSV: 06_WO.csv -> WO_MTO_07
     */
    public static MesWorkOrderDO buildWorkOrderMto() {
        return MesWorkOrderDO.builder()
                .id(5007L)
                .tenantId(TENANT_ID_FILM)
                .workOrderNo("WO_MTO_07")
                .orderType("MTO")
                .planNo("PO_SALES_007")
                // 产品快照
                .productId(1002L)
                .productCode("M_SLIT_ROLL")
                .productName("分切子卷_1000m")
                .productSpec("1000m*50um")
                // 工艺快照
                .routeId(202L)
                .routeCode("RT_FILM_STD")
                .routeName("标准薄膜工艺")
                .workshopName("一号车间")
                // 生产参数
                .quantity(new BigDecimal("2000.0"))
                .unit("pcs") // 模切后单位变更为 pcs
                .requestDate(LocalDate.now().plusDays(5))
                .status("PENDING")
                .build();
    }

    /**
     * [WorkOrderSub] 构建: 模切派工
     * 对应 CSV: 07_Job_History.csv -> EQ_DIE_01
     */
    public static MesWorkOrderSubDO buildSubTaskDieCut() {
        return MesWorkOrderSubDO.builder()
                .id(6007L)
                .tenantId(TENANT_ID_FILM)
                .workOrderId(5007L)
                .workOrderNo("WO_MTO_07")
                // 产品快照
                .productId(1002L)
                .productCode("M_SLIT_ROLL")
                .productName("分切子卷_1000m")
                .productSpec("1000m*50um")
                // 工艺快照
                .processId(101L)
                .processCode("PROCESS_DIE_CUT")
                .processName("模切")
                // 资源工位
                .stationId(9001L)
                .stationCode("EQ_DIE_01")
                .stationName("模切机M1")
                // 计划
                .planQty(new BigDecimal("2000.0"))
                .status("PENDING")
                .build();
    }
}
