// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/ut/MesUnitTestDataFactory.java
package cn.iocoder.yudao.module.mes.ut;

import cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.mes.ut.MesTestConstants.TENANT_ID_FILM;
import static cn.iocoder.yudao.module.mes.ut.MesTestConstants.TENANT_ID_KEJIE;

/**
 * [单元测试] 数据工厂
 * 场景：DAO 层基础 CRUD 测试
 * 特点：数据结构完整，但不一定符合复杂业务关联逻辑
 */
public class MesUnitTestDataFactory {

    public static ProcessDO buildProcessDieCut() {
        return ProcessDO.builder()
                .id(101L)
                .tenantId(TENANT_ID_FILM)
                .code("PROCESS_DIE_CUT")
                .name("模切")
                .processType("manufacture")
                // 冗余字段
                .workshopId(201L)
                .workshopCode("WS_DIE_01")
                .workshopName("模切一车间")
                .bindStation(true) // 核心测试点：Boolean 映射
                .status(0)
                .build();
    }

    // ========== Material (T02 - Kejie) ==========
    public static MesMaterialDO buildMaterialAlIngot() {
        return MesMaterialDO.builder()
                .id(2001L)
                .tenantId(TENANT_ID_KEJIE) // 统一引用常量
                .code("M_AL_INGOT")
                .name("铝合金锭_ADC12")
                .category("raw")
                .materialGrade("ADC12")
                .unit("kg")
                .drawingUrl("/docs/std/al_adc12.pdf")
                .leadTime(3)
                .supplierId(3001L)
                .supplierName("南山铝业")
                .status(0)
                .build();
    }

    // ========== Material (T01 - Film) ==========
    public static MesMaterialDO buildMaterialResinHV() {
        return MesMaterialDO.builder()
                .id(1001L)
                .tenantId(TENANT_ID_FILM) // 统一引用常量
                .code("M_RESIN_HV")
                .name("高粘度光学树脂")
                .category("raw")
                .unit("kg")
                .unitWeight(new BigDecimal("1"))
                .scrapRate(BigDecimal.ZERO)
                .remark("仿真数据: 02_Material.csv")
                .status(0)
                .build();
    }

    // ========== WorkOrder (T01 - Film) ==========
    public static MesWorkOrderDO buildWorkOrderMts() {
        return MesWorkOrderDO.builder()
                .id(5001L)
                .tenantId(TENANT_ID_FILM)
                .workOrderNo("WO_MTS_01_A")
                .orderType("MTS")
                .planId(8001L)
                .planNo("PLn_202602_01")
                // 产品信息
                .productId(1002L)
                .productCode("M_SLIT_ROLL")
                .productName("分切子卷_1000m")
                .productSpec("1000m*50um")
                // 生产车间
                .workshopId(101L)
                .workshopName("一号车间")
                // 工艺路线
                .routeId(202L)
                .routeCode("RT_FILM_STD")
                .routeName("标准薄膜工艺")
                // 其他
                .quantity(new BigDecimal("1000.0"))
                .unit("kg")
                .requestDate(LocalDate.now().plusDays(7))
                .status("PENDING")
                .priority(10)
                .remark("自动化测试数据")
                .build();
    }

    public static MesWorkOrderSubDO buildWorkOrderSubTask() {
        return MesWorkOrderSubDO.builder()
                .id(6001L)
                .tenantId(TENANT_ID_FILM)
                .subOrderNo("WO_MTS_01_A_010")
                .workOrderId(5001L)
                .workOrderNo("WO_MTS_01_A")
                // 产品冗余
                .productId(1002L)
                .productCode("M_SLIT_ROLL")
                .productName("分切子卷_1000m")
                .productSpec("1000m*50um")
                // 工艺信息
                .routeProcessId(901L)
                .routeId(202L)
                .routeCode("RT_FILM_STD")
                .processId(101L)
                .processCode("PROCESS_DIE_CUT")
                .processName("模切")
                .seqNo(10)
                // 资源信息
                .stationId(9001L)
                .stationName("模切工位A")
                .stationCode("EQ_DIE_01")
                // 计划与实绩
                .planDate(LocalDate.now())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(2))
                .planQty(new BigDecimal("1000.0"))
                .actualQty(BigDecimal.ZERO)
                .goodQty(BigDecimal.ZERO)
                .status("PENDING")
                .build();
    }
}
