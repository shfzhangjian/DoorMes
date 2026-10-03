// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/dal/mysql/workorder/MesWorkOrderMapperTest.java
package cn.iocoder.yudao.module.mes.dal.mysql.workorder;

import cn.iocoder.yudao.framework.test.core.ut.BaseManualTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.dal.mysql.workordersub.MesWorkOrderSubMapper;
import cn.iocoder.yudao.module.mes.ut.factory.WorkOrderDataFactory;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@Import({MesWorkOrderMapper.class, MesWorkOrderSubMapper.class})
public class MesWorkOrderMapperTest extends BaseManualTest {

    @Resource
    private MesWorkOrderMapper mesWorkOrderMapper;
    @Resource
    private MesWorkOrderSubMapper mesWorkOrderSubMapper;

    @BeforeEach
    public void clean() {
        System.out.println("🧹 清理旧数据 (ID: 5001, 6001, 6007)...");
        jdbcTemplate.update("DELETE FROM mes_work_order WHERE id IN (5001)");
        jdbcTemplate.update("DELETE FROM mes_work_order_sub WHERE id IN (6001, 6007)");
    }

    @Test
    @DisplayName("Test: 验证主工单全量冗余字段 (来源/产品/资源)")
    public void testWorkOrder_FullRedundancy_Check() {
        System.out.println("\n========== [开始测试] 主工单全量冗余字段验证 ==========");

        // 1. 准备数据 (MTS工单)
        MesWorkOrderDO workOrder = WorkOrderDataFactory.buildWorkOrderMts();
        System.out.println("1. 准备数据: 工单号=" + workOrder.getWorkOrderNo());

        // 2. 插入
        mesWorkOrderMapper.insert(workOrder);
        track("mes_work_order", workOrder.getId());
        System.out.println("2. 插入成功，ID=" + workOrder.getId());

        // 3. 核心断言：验证 5 大冗余字段组
        MesWorkOrderDO dbRecord = mesWorkOrderMapper.selectById(workOrder.getId());
        System.out.println("3. 回查数据库记录...");

        // A. 来源冗余
        System.out.println("   [A] 来源冗余验证: 计划号=" + dbRecord.getPlanNo());
        assertEquals("PLn_202602_01", dbRecord.getPlanNo());

        // B. 产品冗余
        System.out.println("   [B] 产品冗余验证: Code=" + dbRecord.getProductCode() + ", Name=" + dbRecord.getProductName());
        assertEquals("M_SLIT_ROLL", dbRecord.getProductCode());
        assertEquals("分切子卷_1000m", dbRecord.getProductName());
        assertEquals("1000m*50um", dbRecord.getProductSpec());

        // C. 资源冗余
        System.out.println("   [C] 资源冗余验证: 车间=" + dbRecord.getWorkshopName() + ", 工艺=" + dbRecord.getRouteCode());
        assertEquals("一号车间", dbRecord.getWorkshopName());
        assertEquals("RT_FILM_STD", dbRecord.getRouteCode());

        System.out.println("✅ Full Redundancy Check 测试通过！");
    }

    @Test
    @DisplayName("Test: 验证派工细单工位映射与默认值")
    public void testWorkOrderSub_StationMapping_Check() {
        System.out.println("\n========== [开始测试] 派工细单工位映射验证 ==========");

        // 1. 准备数据 (模切派工)
        MesWorkOrderSubDO subOrder = WorkOrderDataFactory.buildSubTaskDieCut();
        System.out.println("1. 准备数据: 细单号=" + subOrder.getSubOrderNo());

        // 2. 插入
        mesWorkOrderSubMapper.insert(subOrder);
        track("mes_work_order_sub", subOrder.getId());
        System.out.println("2. 插入成功，ID=" + subOrder.getId());

        // 3. 核心断言
        MesWorkOrderSubDO dbRecord = mesWorkOrderSubMapper.selectById(subOrder.getId());
        System.out.println("3. 回查数据库记录...");

        // 目标: 验证你特别要求的 stationCode 和 stationName
        System.out.println("   [验证] 工位信息: Code=" + dbRecord.getStationCode() + ", Name=" + dbRecord.getStationName());
        assertEquals("EQ_DIE_01", dbRecord.getStationCode(), "StationCode 丢失");
        assertEquals("模切机M1", dbRecord.getStationName(), "StationName 丢失");

        // 目标: 验证默认数值 (BigDecimal 精度)
        System.out.println("   [验证] 实绩数量: " + dbRecord.getActualQty());
        assertEquals(0, BigDecimal.ZERO.compareTo(dbRecord.getActualQty()));

        System.out.println("✅ Station Mapping Check 测试通过！");
    }

    @Test
    @DisplayName("Test: 验证部分更新机制 (UpdateById)")
    public void testUpdate_Mechanism() {
        System.out.println("\n========== [开始测试] 部分更新机制验证 ==========");

        // 1. Insert
        MesWorkOrderDO workOrder = WorkOrderDataFactory.buildWorkOrderMts();
        mesWorkOrderMapper.insert(workOrder);
        track("mes_work_order", workOrder.getId());
        System.out.println("1. 插入初始记录: 备注=" + workOrder.getRemark());

        // 2. Update (只更新部分字段)
        MesWorkOrderDO updateReq = new MesWorkOrderDO();
        updateReq.setId(workOrder.getId());
        updateReq.setRemark("Updated Remark");
        updateReq.setRealStartTime(LocalDateTime.now());

        System.out.println("2. 执行更新: 设置备注='Updated Remark', 开工时间=NOW");
        mesWorkOrderMapper.updateById(updateReq);

        // 3. Assert
        MesWorkOrderDO dbRecord = mesWorkOrderMapper.selectById(workOrder.getId());

        System.out.println("3. 回查验证:");
        System.out.println("   新备注: " + dbRecord.getRemark());
        System.out.println("   新时间: " + dbRecord.getRealStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("   原工单号(未变): " + dbRecord.getWorkOrderNo());

        assertEquals("Updated Remark", dbRecord.getRemark());
        assertNotNull(dbRecord.getRealStartTime());
        // 验证未更新字段保持原样
        assertEquals("WO_MTS_01_A", dbRecord.getWorkOrderNo());

        System.out.println("✅ Update Mechanism Check 测试通过！");
    }
}
