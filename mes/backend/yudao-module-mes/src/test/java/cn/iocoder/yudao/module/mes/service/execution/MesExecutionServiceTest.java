// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/execution/MesExecutionServiceTest.java
package cn.iocoder.yudao.module.mes.service.execution;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoTest;
import cn.iocoder.yudao.module.mes.controller.admin.execution.vo.MesActionExecuteReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessActionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderActionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessActionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workorderaction.MesWorkOrderActionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workordersub.MesWorkOrderSubMapper;
import cn.iocoder.yudao.module.mes.service.feed.MesProdFeedService;
import cn.iocoder.yudao.module.mes.service.qms.MesQmsService;
import cn.iocoder.yudao.module.mes.service.qtime.MesQTimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Step 7.2 业务逻辑测试 (增强日志版)
 * 核心目标: 验证 T01/T02 场景下的动作执行、分流与阻断逻辑
 */
@Import(MesExecutionServiceImpl.class)
public class MesExecutionServiceTest extends BaseMockitoTest {

    @InjectMocks
    private MesExecutionServiceImpl executionService;

    @Mock private MesWorkOrderSubMapper subOrderMapper;
    @Mock private RouteProcessActionMapper routeActionMapper;
    @Mock private MesWorkOrderActionMapper workOrderActionMapper;

    @Mock private MesProdFeedService feedService;
    @Mock private MesQmsService qmsService;
    @Mock private MesQTimeService qTimeService;

    // =================================================================
    // 🟢 场景 1: T01 湿法涂布 - 正常参数报工 (Happy Path)
    // 对应 CSV: 涂台工艺-参数记录 (ID: 9121-17)
    // =================================================================
    @Test
    @DisplayName("Scenario 1: 涂布参数记录 - 正常保存快照")
    public void testExecute_T01_CoatParam() {
        System.out.println("\n========== [开始测试] 场景 1: T01 湿法涂布 - 正常参数报工 ==========");

        // 1. 准备数据
        Long subOrderId = 1001L;
        Long actionId = 9002L;

        MesWorkOrderSubDO subOrder = MesWorkOrderSubDO.builder()
                .id(subOrderId).tenantId(121L).workOrderId(5001L).build();

        RouteProcessActionDO actionDef = RouteProcessActionDO.builder()
                .id(actionId)
                .actionName("涂台工艺-参数记录")
                .triggerMoment("DURING_PROCESS")
                .dataMapping(Map.of("target", "mes_work_order_action"))
                .build();

        System.out.println("1. 模拟环境: 派工单=" + subOrderId + ", 动作=" + actionDef.getActionName());

        // 2. Mock
        when(subOrderMapper.selectById(eq(subOrderId))).thenReturn(subOrder);
        when(routeActionMapper.selectById(eq(actionId))).thenReturn(actionDef);

        // 3. 执行 Request
        Map<String, Object> formValues = Map.of(
                "f_1279726413", 80.5, // 料槽液位
                "f_472399596", 20.0   // 线速
        );
        MesActionExecuteReqVO req = new MesActionExecuteReqVO();
        req.setSubOrderId(subOrderId);
        req.setActionId(actionId);
        req.setStationId(688L);
        req.setActionValue(formValues);
        req.setIsPass(true);

        System.out.println("2. 用户输入: " + formValues);

        executionService.executeAction(req);
        System.out.println("3. 执行完成，未抛出异常");

        // 4. 验证
        verify(workOrderActionMapper).insert(argThat((MesWorkOrderActionDO record) -> {
            boolean match = record.getActionValue().get("f_1279726413").equals(80.5) &&
                    record.getStationId().equals(688L);
            System.out.println("4. 验证: 动作快照已保存? " + (match ? "✅ 是" : "❌ 否"));
            return match;
        }));

        verify(feedService, never()).createFeedRecord(any(), any());
        System.out.println("5. 验证: 未触发投料逻辑 (符合预期)");
        System.out.println("✅ 场景 1 测试通过！");
    }

    // =================================================================
    // 🔵 场景 2: T02 熔炼 - 投料防错 (Material Feed)
    // 对应 CSV: 熔炼-熔炼投料 (Scan)
    // =================================================================
    @Test
    @DisplayName("Scenario 2: 熔炼投料 - 触发库存扣减")
    public void testExecute_T02_MeltFeed() {
        System.out.println("\n========== [开始测试] 场景 2: T02 熔炼 - 投料与库存扣减 ==========");

        // 1. 准备
        MesWorkOrderSubDO subOrder = MesWorkOrderSubDO.builder().id(2001L).build();
        RouteProcessActionDO actionDef = RouteProcessActionDO.builder()
                .id(9117L)
                .actionName("熔炼-熔炼投料")
                .actionConfig(Map.of("component", "Input"))
                .build();

        System.out.println("1. 模拟环境: 动作=" + actionDef.getActionName() + " (含'投料'关键字)");

        when(subOrderMapper.selectById(any())).thenReturn(subOrder);
        when(routeActionMapper.selectById(any())).thenReturn(actionDef);

        // 2. 执行 Request
        MesActionExecuteReqVO req = new MesActionExecuteReqVO();
        req.setSubOrderId(2001L);
        req.setActionId(9117L);
        req.setStationId(999L);
        req.setActionValue(Map.of("barcode", "SN_AL_001")); // 扫码
        req.setIsPass(true);

        System.out.println("2. 用户扫码: SN_AL_001");

        executionService.executeAction(req);

        // 3. 验证
        verify(feedService, times(1)).createFeedRecord(eq(2001L), any());
        System.out.println("3. 验证: FeedService.createFeedRecord 被调用? ✅ 是");
        System.out.println("✅ 场景 2 测试通过！");
    }

    // =================================================================
    // 🔴 场景 3: QMS 阻断 (Block)
    // 对应 CSV: 单片压槽-首件检验 (NG)
    // =================================================================
    @Test
    @DisplayName("Scenario 3: 首件检验 NG - 触发 NCR 并锁定工单")
    public void testExecute_QmsBlock() {
        System.out.println("\n========== [开始测试] 场景 3: QMS 首件检验 NG 阻断 ==========");

        MesWorkOrderSubDO subOrder = MesWorkOrderSubDO.builder().id(3001L).status("DOING").build();
        RouteProcessActionDO actionDef = RouteProcessActionDO.builder()
                .id(9126L)
                .actionName("首件检验")
                .errorStrategy("BLOCK") // 🚨 策略: 阻断
                .build();

        System.out.println("1. 模拟环境: 异常策略=BLOCK, 当前状态=DOING");

        when(subOrderMapper.selectById(any())).thenReturn(subOrder);
        when(routeActionMapper.selectById(any())).thenReturn(actionDef);

        MesActionExecuteReqVO req = new MesActionExecuteReqVO();
        req.setSubOrderId(3001L);
        req.setActionId(9126L);
        req.setStationId(1L);
        req.setIsPass(false); // ❌ NG
        req.setFailureReason("厚度超标");

        System.out.println("2. 用户提交: 检验不合格 (厚度超标)");

        // 断言: 抛出异常
        assertThrows(RuntimeException.class, () -> executionService.executeAction(req));
        System.out.println("3. 验证: 系统抛出 RuntimeException? ✅ 是");

        // 验证 NCR
        verify(qmsService).createNcRecord(eq(3001L), eq(9126L), eq("厚度超标"));
        System.out.println("4. 验证: NCR 不合格记录已创建? ✅ 是");

        // 验证 锁单
        verify(subOrderMapper).updateById(argThat((MesWorkOrderSubDO so) -> {
            boolean locked = "BLOCKED".equals(so.getStatus());
            System.out.println("5. 验证: 工单状态变更为 BLOCKED? " + (locked ? "✅ 是" : "❌ 否"));
            return locked;
        }));
        System.out.println("✅ 场景 3 测试通过！");
    }

    // =================================================================
    // 🟠 场景 4: Q-Time 校验 (Rule Check)
    // =================================================================
    @Test
    @DisplayName("Scenario 4: Q-Time 校验不通过 - 禁止作业")
    public void testExecute_QTimeFail() {
        System.out.println("\n========== [开始测试] 场景 4: Q-Time (静置时间) 校验失败 ==========");

        MesWorkOrderSubDO subOrder = MesWorkOrderSubDO.builder().id(4001L).build();
        RouteProcessActionDO actionDef = RouteProcessActionDO.builder()
                .triggerMoment("PRE_CHECK") // 🚨 只有 PRE_CHECK 才验 Q-Time
                .processId(101L)
                .build();

        System.out.println("1. 模拟环境: 触发时机=PRE_CHECK, 工序ID=101");

        when(subOrderMapper.selectById(any())).thenReturn(subOrder);
        when(routeActionMapper.selectById(any())).thenReturn(actionDef);

        // Mock QTime Service 返回 false (时间未到)
        doReturn(false).when(qTimeService).checkQTime(any(), any());
        System.out.println("2. 模拟 QTimeService: 返回 false (时间未到)");

        MesActionExecuteReqVO req = new MesActionExecuteReqVO();
        req.setSubOrderId(4001L);
        req.setActionId(999L);
        req.setStationId(1L);

        // 断言异常
        RuntimeException ex = assertThrows(RuntimeException.class, () -> executionService.executeAction(req));
        System.out.println("3. 捕获异常信息: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Q-Time"));

        // 验证: 动作并未保存
        verify(workOrderActionMapper, never()).insert(any(MesWorkOrderActionDO.class));
        System.out.println("4. 验证: 动作记录未保存 (事务回滚/未执行)? ✅ 是");
        System.out.println("✅ 场景 4 测试通过！");
    }
}
