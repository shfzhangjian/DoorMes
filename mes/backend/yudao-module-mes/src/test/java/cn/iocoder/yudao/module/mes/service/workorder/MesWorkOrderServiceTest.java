// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/workorder/MesWorkOrderServiceTest.java
package cn.iocoder.yudao.module.mes.service.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoTest;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.process.ProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workorder.MesWorkOrderMapper;
import cn.iocoder.yudao.module.mes.ut.factory.WorkOrderDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MES 主工单 CRUD 单元测试 (增强日志版)
 * 专注：基础数据的增删改查、状态校验、分页查询
 */
@Import(MesWorkOrderServiceImpl.class)
public class MesWorkOrderServiceTest extends BaseMockitoTest {

    @InjectMocks
    private MesWorkOrderServiceImpl workOrderService;

    @Mock
    private MesWorkOrderMapper mesWorkOrderMapper;
    @Mock
    private ProcessMapper processMapper;

    // ================== Update (修改) ==================

    @Test
    @DisplayName("Update: 正常修改")
    public void testUpdateWorkOrder_Success() {
        System.out.println("\n========== [开始测试] Update: 正常修改工单备注 ==========");

        // 1. 准备数据
        MesWorkOrderDO dbWorkOrder = WorkOrderDataFactory.buildWorkOrderMts();
        MesWorkOrderSaveReqVO updateReqVO = BeanUtils.toBean(dbWorkOrder, MesWorkOrderSaveReqVO.class);
        updateReqVO.setRemark("修改备注");
        System.out.println("1. 准备数据: 工单ID=" + dbWorkOrder.getId() + ", 原备注=" + dbWorkOrder.getRemark() + ", 新备注=修改备注");

        // 2. Mock
        when(mesWorkOrderMapper.selectById(eq(dbWorkOrder.getId()))).thenReturn(dbWorkOrder);
        // 🚨 强制转型 1: 消除 updateById(T) vs (Collection) 歧义
        when(mesWorkOrderMapper.updateById((MesWorkOrderDO) any())).thenReturn(1);
        System.out.println("2. Mock环境: selectById -> 返回存在对象; updateById -> 返回 1");

        // 3. 执行
        System.out.println("3. 执行 updateWorkOrder...");
        workOrderService.updateWorkOrder(updateReqVO);

        // 4. 验证
        // 🚨 强制转型 2: 消除 verify 中的歧义
        verify(mesWorkOrderMapper).updateById((MesWorkOrderDO) argThat(arg -> {
            MesWorkOrderDO wo = (MesWorkOrderDO) arg;
            boolean match = wo.getId().equals(dbWorkOrder.getId()) && "修改备注".equals(wo.getRemark());
            System.out.println("4. 验证: updateById 参数 ID匹配且备注已更新? " + (match ? "✅ 是" : "❌ 否"));
            return match;
        }));
        System.out.println("✅ Update Success 测试通过！");
    }

    @Test
    @DisplayName("Update: 尝试修改不存在的工单 -> 抛出异常")
    public void testUpdateWorkOrder_NotFound() {
        System.out.println("\n========== [开始测试] Update: 修改不存在的工单 ==========");

        MesWorkOrderSaveReqVO reqVO = new MesWorkOrderSaveReqVO();
        reqVO.setId(randomLongId());
        System.out.println("1. 准备请求: 随机ID=" + reqVO.getId());

        when(mesWorkOrderMapper.selectById(any())).thenReturn(null);
        System.out.println("2. Mock环境: selectById -> 返回 null");

        System.out.println("3. 执行 updateWorkOrder 并捕获异常...");
        assertThrows(RuntimeException.class, () -> workOrderService.updateWorkOrder(reqVO));

        System.out.println("4. 验证: 已抛出 RuntimeException");
        System.out.println("✅ Update Not Found 测试通过！");
    }

    @Test
    @DisplayName("Update: 尝试修改已关闭(CLOSE)工单 -> 抛出异常")
    public void testUpdateWorkOrder_StatusClosed() {
        System.out.println("\n========== [开始测试] Update: 修改已关闭(CLOSE)工单 ==========");

        MesWorkOrderDO closedOrder = WorkOrderDataFactory.buildWorkOrderMts();
        closedOrder.setStatus("CLOSE");
        MesWorkOrderSaveReqVO reqVO = BeanUtils.toBean(closedOrder, MesWorkOrderSaveReqVO.class);
        System.out.println("1. 准备数据: 工单ID=" + closedOrder.getId() + ", 状态=CLOSE");

        when(mesWorkOrderMapper.selectById(eq(closedOrder.getId()))).thenReturn(closedOrder);
        System.out.println("2. Mock环境: selectById -> 返回已关闭工单");

        System.out.println("3. 执行 updateWorkOrder 并捕获异常...");
        assertThrows(RuntimeException.class, () -> workOrderService.updateWorkOrder(reqVO));

        System.out.println("4. 验证: 已抛出 RuntimeException (禁止修改)");
        System.out.println("✅ Update Closed Status 测试通过！");
    }

    // ================== Delete (删除) ==================

    @Test
    @DisplayName("Delete: 正常删除 PENDING 状态工单")
    public void testDeleteWorkOrder_Success() {
        System.out.println("\n========== [开始测试] Delete: 正常删除 PENDING 工单 ==========");

        MesWorkOrderDO pendingOrder = WorkOrderDataFactory.buildWorkOrderMts();
        // 确保状态是 PENDING (DataFactory 默认可能是 null 或其他，显式设置更安全)
        pendingOrder.setStatus("PENDING");
        System.out.println("1. 准备数据: 工单ID=" + pendingOrder.getId() + ", 状态=PENDING");

        when(mesWorkOrderMapper.selectById(eq(pendingOrder.getId()))).thenReturn(pendingOrder);
        System.out.println("2. Mock环境: selectById -> 返回 PENDING 工单");

        System.out.println("3. 执行 deleteWorkOrder...");
        workOrderService.deleteWorkOrder(pendingOrder.getId());

        verify(mesWorkOrderMapper).deleteById(eq(pendingOrder.getId()));
        System.out.println("4. 验证: deleteById 被调用? ✅ 是");
        System.out.println("✅ Delete Success 测试通过！");
    }

    @Test
    @DisplayName("Delete: 尝试删除 DOING 状态工单 -> 抛出异常")
    public void testDeleteWorkOrder_StatusDoing() {
        System.out.println("\n========== [开始测试] Delete: 删除执行中(DOING)工单 ==========");

        MesWorkOrderDO doingOrder = WorkOrderDataFactory.buildWorkOrderMts();
        doingOrder.setStatus("DOING");
        System.out.println("1. 准备数据: 工单ID=" + doingOrder.getId() + ", 状态=DOING");

        when(mesWorkOrderMapper.selectById(eq(doingOrder.getId()))).thenReturn(doingOrder);
        System.out.println("2. Mock环境: selectById -> 返回 DOING 工单");

        System.out.println("3. 执行 deleteWorkOrder 并捕获异常...");
        assertThrows(RuntimeException.class, () -> workOrderService.deleteWorkOrder(doingOrder.getId()));

        System.out.println("4. 验证: 已抛出 RuntimeException (禁止删除)");
        System.out.println("✅ Delete Doing Status 测试通过！");
    }

    // ================== Get (查询) ==================

    @Test
    @DisplayName("GetPage: 分页查询参数传递验证")
    public void testGetWorkOrderPage() {
        System.out.println("\n========== [开始测试] GetPage: 分页查询 ==========");

        MesWorkOrderPageReqVO reqVO = new MesWorkOrderPageReqVO();
        reqVO.setWorkOrderNo("WO_MTS");
        reqVO.setStatus("PENDING");
        System.out.println("1. 查询参数: No=WO_MTS, Status=PENDING");

        when(mesWorkOrderMapper.selectPage(eq(reqVO), any())).thenReturn(new PageResult<>());
        System.out.println("2. Mock环境: selectPage -> 返回空 PageResult");

        System.out.println("3. 执行 getWorkOrderPage...");
        workOrderService.getWorkOrderPage(reqVO);

        verify(mesWorkOrderMapper).selectPage(eq(reqVO), any());
        System.out.println("4. 验证: selectPage 被调用且参数正确? ✅ 是");
        System.out.println("✅ GetPage Success 测试通过！");
    }
}
