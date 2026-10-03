// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/workorder/MesWorkOrderSubServiceTest.java
package cn.iocoder.yudao.module.mes.service.workorder;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoTest;
import cn.iocoder.yudao.module.mes.controller.admin.workorder.vo.MesWorkOrderSubSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.dal.mysql.workordersub.MesWorkOrderSubMapper;
import cn.iocoder.yudao.module.mes.ut.factory.WorkOrderDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MES 派工细单 CRUD 单元测试 (增强日志版)
 * 专注：细单生命周期管理与架构师红线校验
 */
@Import(MesWorkOrderSubServiceImpl.class)
public class MesWorkOrderSubServiceTest extends BaseMockitoTest {

    @InjectMocks
    private MesWorkOrderSubServiceImpl subService;

    @Mock
    private MesWorkOrderSubMapper subMapper;

    @Test
    @DisplayName("Create: 验证默认值初始化 (ActualQty=0, Status=PENDING)")
    public void testCreateWorkOrderSub() {
        System.out.println("\n========== [开始测试] Create: 派工细单默认值验证 ==========");

        // 1. 准备
        MesWorkOrderSubDO subDO = WorkOrderDataFactory.buildSubTaskDieCut();
        subDO.setStatus(null); // 强制置空，测试 Service 默认值逻辑
        MesWorkOrderSubSaveReqVO reqVO = BeanUtils.toBean(subDO, MesWorkOrderSubSaveReqVO.class);
        System.out.println("1. 准备数据: 工单号=" + reqVO.getWorkOrderNo() + ", 初始状态=NULL");

        // 2. Mock
        // 🚨 强制转型 1: insert(T)
        when(subMapper.insert((MesWorkOrderSubDO) any())).thenReturn(1);
        System.out.println("2. Mock环境: insert -> 返回 1");

        // 3. 执行
        System.out.println("3. 执行 createWorkOrderSub...");
        subService.createWorkOrderSub(reqVO);

        // 4. 验证
        // 🚨 强制转型 2: verify insert(T)
        verify(subMapper).insert((MesWorkOrderSubDO) argThat(arg -> {
            MesWorkOrderSubDO sub = (MesWorkOrderSubDO) arg;
            boolean statusOk = "PENDING".equals(sub.getStatus());
            boolean qtyOk = BigDecimal.ZERO.compareTo(sub.getActualQty()) == 0;

            System.out.println("4. 验证: 状态默认为 PENDING? " + (statusOk ? "✅ 是" : "❌ 否 (" + sub.getStatus() + ")"));
            System.out.println("   验证: 实绩数量默认为 0? " + (qtyOk ? "✅ 是" : "❌ 否 (" + sub.getActualQty() + ")"));
            return statusOk && qtyOk;
        }));
        System.out.println("✅ Create Default Values 测试通过！");
    }

    @Test
    @DisplayName("Update: 架构师红线 - DOING 状态禁止修改核心数据")
    public void testUpdate_RedLine_Check() {
        System.out.println("\n========== [开始测试] Update: 架构师红线校验 (DOING 禁止修改) ==========");

        // 1. 准备一个正在执行的任务
        MesWorkOrderSubDO doingSub = WorkOrderDataFactory.buildSubTaskDieCut();
        doingSub.setStatus("DOING");
        System.out.println("1. 准备数据: 细单ID=" + doingSub.getId() + ", 状态=DOING");

        MesWorkOrderSubSaveReqVO reqVO = BeanUtils.toBean(doingSub, MesWorkOrderSubSaveReqVO.class);
        reqVO.setPlanQty(new BigDecimal("9999"));
        System.out.println("   尝试操作: 修改计划数量为 9999");

        // 2. Mock
        when(subMapper.selectById(eq(doingSub.getId()))).thenReturn(doingSub);
        System.out.println("2. Mock环境: selectById -> 返回 DOING 状态细单");

        // 3. 断言
        System.out.println("3. 执行 updateWorkOrderSub 并捕获异常...");
        RuntimeException exception = assertThrows(RuntimeException.class, () -> subService.updateWorkOrderSub(reqVO));

        System.out.println("4. 捕获异常信息: " + exception.getMessage());
        assert(exception.getMessage().contains("严禁修改"));
        System.out.println("✅ Update Red Line Check 测试通过！");
    }

    @Test
    @DisplayName("Delete: 仅 PENDING 可删")
    public void testDelete_StatusCheck() {
        System.out.println("\n========== [开始测试] Delete: 非 PENDING 状态禁止删除 ==========");

        MesWorkOrderSubDO doneSub = WorkOrderDataFactory.buildSubTaskDieCut();
        doneSub.setStatus("DONE");
        System.out.println("1. 准备数据: 细单ID=" + doneSub.getId() + ", 状态=DONE");

        when(subMapper.selectById(eq(doneSub.getId()))).thenReturn(doneSub);
        System.out.println("2. Mock环境: selectById -> 返回 DONE 状态细单");

        System.out.println("3. 执行 deleteWorkOrderSub 并捕获异常...");
        assertThrows(RuntimeException.class, () -> subService.deleteWorkOrderSub(doneSub.getId()));

        System.out.println("4. 验证: 已抛出 RuntimeException");
        System.out.println("✅ Delete Status Check 测试通过！");
    }
}
