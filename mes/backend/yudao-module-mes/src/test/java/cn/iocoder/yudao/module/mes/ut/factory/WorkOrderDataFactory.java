// backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/ut/factory/WorkOrderDataFactory.java
package cn.iocoder.yudao.module.mes.ut.factory;

import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static cn.iocoder.yudao.module.mes.ut.MesTestConstants.*;

/**
 * [原子工厂] 工单与派工单数据
 * 数据源: 06_WO.csv, 07_Job_History.csv
 */
public class WorkOrderDataFactory {

    /**
     * T01 (Film): MTS 备库工单 (配料/分切)
     * 场景: 场景1 - 混合制造
     */
    public static MesWorkOrderDO buildWorkOrderMts() {
        return MesWorkOrderDO.builder()
                .id(5001L)
                .tenantId(TENANT_ID_FILM)
                .workOrderNo("WO_MTS_01_A")
                .orderType("MTS")
                .planId(8001L)
                .planNo("PLn_202602_01")
                // 产品
                .productId(1002L)
                .productCode("M_SLIT_ROLL")
                .productName("分切子卷_1000m")
                .productSpec("1000m*50um")
                // 资源
                .workshopId(101L)
                .workshopName("一号车间")
                .routeId(202L)
                .routeCode("RT_FILM_STD")
                .routeName("标准薄膜工艺")
                // 参数
                .quantity(new BigDecimal("1000.0"))
                .unit("kg")
                .requestDate(LocalDate.now().plusDays(7))
                .status("PENDING")
                .priority(10)
                .remark("MTS 自动化测试数据")
                .build();
    }

    /**
     * T01 (Film): MTO 订单工单 (模切)
     * 场景: 场景2 - 以存定产
     */
    public static MesWorkOrderDO buildWorkOrderMto() {
        return MesWorkOrderDO.builder()
                .id(5007L)
                .tenantId(TENANT_ID_FILM)
                .workOrderNo("WO_MTO_07")
                .orderType("MTO")
                .planNo("PO_SALES_007") // 来源销售订单
                // 产品
                .productId(1002L)
                .productCode("M_SLIT_ROLL")
                .productName("分切子卷_1000m")
                .productSpec("1000m*50um")
                // 资源
                .routeId(202L)
                .routeCode("RT_FILM_STD")
                .routeName("标准薄膜工艺")
                .workshopName("一号车间")
                // 参数
                .quantity(new BigDecimal("2000.0"))
                .unit("pcs") // 模切后单位变更为 pcs
                .requestDate(LocalDate.now().plusDays(5))
                .status("PENDING")
                .build();
    }

    /**
     * T01 (Film): 派工细单 (对应 WO_MTO_07)
     */
    public static MesWorkOrderSubDO buildSubTaskDieCut() {
        return MesWorkOrderSubDO.builder()
                .id(6007L)
                .tenantId(TENANT_ID_FILM)
                .workOrderId(5007L)
                .workOrderNo("WO_MTO_07")
                .subOrderNo("WO_MTO_07_001") // 补全细单号
                // 产品快照
                .productId(1002L)
                .productCode("M_SLIT_ROLL")
                .productName("分切子卷_1000m")
                .productSpec("1000m*50um")
                // 工艺快照
                .routeProcessId(901L)
                .routeId(202L)
                .routeCode("RT_FILM_STD")
                .processId(101L)
                .processCode("PROCESS_DIE_CUT")
                .processName("模切")
                .seqNo(10)
                // 资源工位
                .stationId(9001L)
                .stationCode("EQ_DIE_01")
                .stationName("模切机M1")
                // 计划与实绩
                .planDate(LocalDate.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .planQty(new BigDecimal("2000.0"))
                .actualQty(BigDecimal.ZERO)
                .goodQty(BigDecimal.ZERO)
                .scrapQty(BigDecimal.ZERO)
                .status("PENDING")
                .build();
    }
}
