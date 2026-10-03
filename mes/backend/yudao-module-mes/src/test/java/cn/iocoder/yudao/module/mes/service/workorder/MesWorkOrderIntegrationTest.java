// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/workorder/MesWorkOrderIntegrationTest.java
package cn.iocoder.yudao.module.mes.service.workorder;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoTest;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.workorder.MesWorkOrderMapper;
import cn.iocoder.yudao.module.mes.ut.MesTestConstants;
import cn.iocoder.yudao.module.mes.ut.factory.WorkOrderDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MES 工单业务集成测试 (Service Layer)
 * 核心目标：验证 Scenario 1 (混合制造), Scenario 2 (库存拉动), Scenario 3 (Q-Time)
 */
@Import(MesWorkOrderServiceImpl.class)
public class MesWorkOrderIntegrationTest extends BaseMockitoTest {

    @InjectMocks
    @Spy
    private MesWorkOrderServiceImpl workOrderService;

    @Mock
    private MesWorkOrderMapper mesWorkOrderMapper;

    // =================================================================
    // 🟢 场景 1: 全链路混合制造 (Hybrid Flow) - T01 禾臣
    // =================================================================
    @Test
    @DisplayName("Scenario 1: 混合制造 - 库存不足时，应触发上游工单自动拆解")
    public void testCreate_Scenario1_HybridSplit() {
        System.out.println("\n========== [开始测试] 场景 1: 混合制造 - 库存不足触发拆解 ==========");

        // 1. 准备数据
        MesWorkOrderDO workOrderDO = WorkOrderDataFactory.buildWorkOrderMts();
        MesWorkOrderSaveReqVO reqVO = BeanUtils.toBean(workOrderDO, MesWorkOrderSaveReqVO.class);
        System.out.println("1. 准备工单数据: " + reqVO.getWorkOrderNo());

        // 2. 模拟环境
        doReturn(false).when(workOrderService).checkWipInventory(eq("M_SLIT_ROLL")); // 模拟库存不足
        doNothing().when(workOrderService).triggerUpstreamSplit(any()); // 模拟触发拆解逻辑
        System.out.println("2. 模拟环境: 库存不足 (checkWipInventory=false)");

        // 🚨 强制转型 1: insert(T)
        when(mesWorkOrderMapper.insert((MesWorkOrderDO) any())).thenReturn(1);

        // 3. 执行
        System.out.println("3. 执行创建工单...");
        workOrderService.createWorkOrder(reqVO);

        // 4. 验证
        verify(workOrderService, times(1)).triggerUpstreamSplit(any(MesWorkOrderDO.class));
        System.out.println("4. 验证: 是否触发上游拆解 (triggerUpstreamSplit)? ✅ 是");

        verify(mesWorkOrderMapper).insert((MesWorkOrderDO) any());
        System.out.println("5. 验证: 工单是否入库? ✅ 是");
        System.out.println("✅ 场景 1 测试通过！");
    }

    // =================================================================
    // 🔵 场景 2: 线边库拉动 (WIP Pull) - T01 禾臣
    // =================================================================
    @Test
    @DisplayName("Scenario 2: 线边库拉动 - 库存充足时，跳过拆解并锁定批次")
    public void testCreate_Scenario2_WipPull() {
        System.out.println("\n========== [开始测试] 场景 2: 线边库拉动 - 库存充足锁定批次 ==========");

        // 1. 准备数据
        MesWorkOrderDO workOrderDO = WorkOrderDataFactory.buildWorkOrderMto();
        MesWorkOrderSaveReqVO reqVO = BeanUtils.toBean(workOrderDO, MesWorkOrderSaveReqVO.class);
        System.out.println("1. 准备工单数据: " + reqVO.getWorkOrderNo());

        // 2. 模拟环境
        doReturn(true).when(workOrderService).checkWipInventory(eq("M_SLIT_ROLL")); // 模拟库存充足
        System.out.println("2. 模拟环境: 库存充足 (checkWipInventory=true)");

        // 🚨 强制转型 2: insert(T)
        when(mesWorkOrderMapper.insert((MesWorkOrderDO) any())).thenReturn(1);

        // 3. 执行
        System.out.println("3. 执行创建工单...");
        workOrderService.createWorkOrder(reqVO);

        // 4. 验证
        verify(workOrderService, never()).triggerUpstreamSplit(any());
        System.out.println("4. 验证: 是否跳过上游拆解? ✅ 是 (未调用 triggerUpstreamSplit)");

        // 🚨 强制转型 3: insert(T) with matcher
        verify(mesWorkOrderMapper).insert((MesWorkOrderDO) argThat(arg -> {
            MesWorkOrderDO wo = (MesWorkOrderDO) arg;
            boolean locked = wo.getRemark() != null && wo.getRemark().contains("锁定线边库库存");
            System.out.println("5. 验证: 备注是否包含'锁定线边库库存'? " + (locked ? "✅ 是" : "❌ 否"));
            return locked;
        }));
        System.out.println("✅ 场景 2 测试通过！");
    }

    // =================================================================
    // 🔴 场景 3-A: Q-Time 强制阻断 (Block) - T02 捷和
    // =================================================================
    @Test
    @DisplayName("Scenario 3-A: Q-Time 阻断 - 冷却时间不足 2h，抛出异常")
    public void testStart_Scenario3_QTime_Block() {
        System.out.println("\n========== [开始测试] 场景 3-A: Q-Time 强制阻断 (冷却不足) ==========");

        // 1. 准备数据
        Long workOrderId = 2001L;
        MesWorkOrderDO workOrder = MesWorkOrderDO.builder()
                .id(workOrderId)
                .tenantId(MesTestConstants.TENANT_ID_KEJIE)
                .routeCode("RT_CNC_01")
                .status("PENDING")
                .build();
        System.out.println("1. 准备工单: ID=" + workOrderId + ", 状态=PENDING");

        // 2. 模拟环境
        when(mesWorkOrderMapper.selectById(eq(workOrderId))).thenReturn(workOrder);
        doReturn(true).when(workOrderService).isQTimeConstraintActive(any()); // 激活 Q-Time
        doReturn(LocalDateTime.now().minusHours(1)).when(workOrderService).fetchLastProcessEndTime(any()); // 上道工序结束于 1 小时前
        System.out.println("2. 模拟环境: Q-Time 激活, 冷却时间=1小时 (要求>2小时)");

        // 3. 执行与断言
        System.out.println("3. 执行开工 (startWorkOrder)...");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            workOrderService.startWorkOrder(workOrderId);
        });

        System.out.println("4. 捕获异常: " + exception.getMessage());
        assert(exception.getMessage().contains("Q-Time"));

        // 🚨 强制转型 4: updateById(T)
        // 之前报错的就是这里: never().updateById(any()) 默认会匹配 updateById(Collection) 导致歧义
        verify(mesWorkOrderMapper, never()).updateById((MesWorkOrderDO) any());
        System.out.println("5. 验证: 工单状态未更新 (Update Never Called)? ✅ 是");
        System.out.println("✅ 场景 3-A 测试通过！");
    }

    // =================================================================
    // 🟢 场景 3-B: Q-Time 放行 (Pass) - T02 捷和
    // =================================================================
    @Test
    @DisplayName("Scenario 3-B: Q-Time 放行 - 冷却时间 > 2h，允许开工")
    public void testStart_Scenario3_QTime_Pass() {
        System.out.println("\n========== [开始测试] 场景 3-B: Q-Time 放行 (冷却充足) ==========");

        // 1. 准备数据
        Long workOrderId = 2001L;
        MesWorkOrderDO workOrder = MesWorkOrderDO.builder()
                .id(workOrderId)
                .tenantId(MesTestConstants.TENANT_ID_KEJIE)
                .routeCode("RT_CNC_01")
                .status("PENDING")
                .build();
        System.out.println("1. 准备工单: ID=" + workOrderId + ", 状态=PENDING");

        // 2. 模拟环境
        when(mesWorkOrderMapper.selectById(eq(workOrderId))).thenReturn(workOrder);
        doReturn(true).when(workOrderService).isQTimeConstraintActive(any());
        doReturn(LocalDateTime.now().minusHours(3)).when(workOrderService).fetchLastProcessEndTime(any()); // 上道工序结束于 3 小时前
        System.out.println("2. 模拟环境: Q-Time 激活, 冷却时间=3小时 (要求>2小时)");

        // 3. 执行
        System.out.println("3. 执行开工 (startWorkOrder)...");
        workOrderService.startWorkOrder(workOrderId);

        // 4. 验证
        // 🚨 强制转型 5: updateById(T)
        verify(mesWorkOrderMapper).updateById((MesWorkOrderDO) argThat(arg -> {
            MesWorkOrderDO wo = (MesWorkOrderDO) arg;
            boolean passed = "DOING".equals(wo.getStatus()) && wo.getRealStartTime() != null;
            System.out.println("4. 验证: 工单状态变更为 DOING? " + (passed ? "✅ 是" : "❌ 否"));
            return passed;
        }));
        System.out.println("✅ 场景 3-B 测试通过！");
    }
}
