package cn.iocoder.yudao.module.mes.service.hc.productioninstruction;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionChangeoverStartReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionChangeoverPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProductionInstructionStartConcurrencyTest {

    private static final Long OPERATION_ID = 894L;
    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 9, 5, 10, 16, 43);

    @InjectMocks
    private HcProductionInstructionServiceImpl service;
    @Mock
    private HcProductionInstructionMapper instructionMapper;
    @Mock
    private HcPlanOrderOperationMapper operationMapper;
    @Mock
    private HcProductionInstructionChangeoverPieceMapper pieceMapper;

    @Test
    void shouldUseLockedExecutingStateWithoutResettingStartTimeOrCompletedQuantity() {
        HcProductionInstructionDO locked = instruction("EXECUTING", 3);
        givenInitialAndLocked(instruction("PENDING", 0), locked);

        assertSame(locked, service.startChangeoverInstruction(request()));

        assertEquals(START_TIME, locked.getExecuteStartTime());
        assertEquals(3, locked.getCompletedQty());
        verifyLockOrder();
        verify(instructionMapper, never()).updateById(any(HcProductionInstructionDO.class));
        verify(instructionMapper, never()).selectOne(any(Wrapper.class));
        verifyNoInteractions(pieceMapper);
    }

    @Test
    void shouldUseLockedCompletedStateWithoutRestartingCompletedInstruction() {
        HcProductionInstructionDO locked = instruction("COMPLETED", 7);
        givenInitialAndLocked(instruction("EXECUTING", 6), locked);

        assertSame(locked, service.startChangeoverInstruction(request()));

        assertEquals(START_TIME, locked.getExecuteStartTime());
        assertEquals(7, locked.getCompletedQty());
        verifyLockOrder();
        verify(instructionMapper, never()).updateById(any(HcProductionInstructionDO.class));
        verifyNoInteractions(pieceMapper);
    }

    @Test
    void shouldRejectAnotherExecutingInstructionUsingCurrentReadAfterOperationLock() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "start-concurrency-test"),
                HcProductionInstructionDO.class);
        givenInitialAndLocked(instruction("PENDING", 0), instruction("PENDING", 0));
        HcProductionInstructionDO other = instruction("EXECUTING", 2);
        other.setId(33L);
        when(instructionMapper.selectOne(any(Wrapper.class))).thenReturn(other);

        ServiceException error = assertThrows(ServiceException.class, () -> service.startChangeoverInstruction(request()));

        assertTrue(error.getMessage().contains("已有执行中的换型指令"));
        InOrder order = inOrder(instructionMapper, operationMapper);
        order.verify(instructionMapper).selectById(34L);
        order.verify(operationMapper).selectByIdForUpdate(OPERATION_ID);
        order.verify(instructionMapper).selectByIdForUpdate(34L);
        ArgumentCaptor<Wrapper<HcProductionInstructionDO>> query = ArgumentCaptor.forClass(Wrapper.class);
        order.verify(instructionMapper).selectOne(query.capture());
        assertTrue(query.getValue().getSqlSegment().contains("FOR UPDATE"));
        verify(instructionMapper, never()).updateById(any(HcProductionInstructionDO.class));
        verifyNoInteractions(pieceMapper);
    }

    private void givenInitialAndLocked(HcProductionInstructionDO initial, HcProductionInstructionDO locked) {
        when(instructionMapper.selectById(34L)).thenReturn(initial);
        when(operationMapper.selectByIdForUpdate(OPERATION_ID))
                .thenReturn(HcPlanOrderOperationDO.builder().id(OPERATION_ID).build());
        when(instructionMapper.selectByIdForUpdate(34L)).thenReturn(locked);
    }

    private void verifyLockOrder() {
        InOrder order = inOrder(instructionMapper, operationMapper);
        order.verify(instructionMapper).selectById(34L);
        order.verify(operationMapper).selectByIdForUpdate(OPERATION_ID);
        order.verify(instructionMapper).selectByIdForUpdate(34L);
    }

    private HcProductionInstructionDO instruction(String status, int completedQty) {
        return HcProductionInstructionDO.builder().id(34L).planId(1L).planOperationId(OPERATION_ID)
                .instructionType("CHANGEOVER").status("CONFIRMED").segmentBatchNo("W26H163AP")
                .targetModelCode("W26P0300").targetQty(7).executeStatus(status).completedQty(completedQty)
                .executeStartTime(START_TIME).build();
    }

    private HcProductionInstructionChangeoverStartReqVO request() {
        HcProductionInstructionChangeoverStartReqVO request = new HcProductionInstructionChangeoverStartReqVO();
        request.setId(34L);
        return request;
    }
}
