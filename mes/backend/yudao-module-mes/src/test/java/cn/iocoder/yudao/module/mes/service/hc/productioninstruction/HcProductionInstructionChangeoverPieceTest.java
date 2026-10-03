package cn.iocoder.yudao.module.mes.service.hc.productioninstruction;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionChangeoverPieceReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionChangeoverPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionChangeoverPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProductionInstructionChangeoverPieceTest {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 9, 5, 10, 16, 43);
    private static final String PIECE_NO = "W26H163AP013B";

    @InjectMocks
    private HcProductionInstructionServiceImpl service;
    @Mock
    private HcProductionInstructionMapper instructionMapper;
    @Mock
    private HcProductionInstructionChangeoverPieceMapper pieceMapper;
    @Mock
    private HcAdhesive2ReportMapper reportMapper;

    @Test
    void shouldReturnCompletedInstructionForExactRetryWithoutCountingAgain() {
        HcProductionInstructionDO instruction = instruction("COMPLETED");
        givenInstructionAndReport(instruction, report());
        when(pieceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(donePiece(1321L)));

        assertSame(instruction, service.recordChangeoverPiece(request()));

        verify(pieceMapper, never()).insert(any(HcProductionInstructionChangeoverPieceDO.class));
        verify(pieceMapper, never()).selectCountByInstructionId(any());
        verify(instructionMapper, never()).updateById(any(HcProductionInstructionDO.class));
    }

    @Test
    void shouldReturnExecutingInstructionForExactRetryWithoutCountingAgain() {
        HcProductionInstructionDO instruction = instruction("EXECUTING");
        givenInstructionAndReport(instruction, report());
        when(pieceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(donePiece(1321L)));

        assertSame(instruction, service.recordChangeoverPiece(request()));

        verify(pieceMapper, never()).insert(any(HcProductionInstructionChangeoverPieceDO.class));
        verify(pieceMapper, never()).selectCountByInstructionId(any());
    }

    @Test
    void shouldRejectRetryWithAnotherReportForTheSamePiece() {
        givenInstructionAndReport(instruction("COMPLETED"), report());
        when(pieceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(donePiece(1300L)));

        ServiceException error = assertThrows(ServiceException.class, () -> service.recordChangeoverPiece(request()));

        assertTrue(error.getMessage().contains("已关联其他报工记录"));
        verify(pieceMapper, never()).insert(any(HcProductionInstructionChangeoverPieceDO.class));
        verify(pieceMapper, never()).selectCountByInstructionId(any());
    }

    @Test
    void shouldRejectNewPieceAfterInstructionCompleted() {
        givenInstructionAndReport(instruction("COMPLETED"), report());

        ServiceException error = assertThrows(ServiceException.class, () -> service.recordChangeoverPiece(request()));

        assertTrue(error.getMessage().contains("未开始执行或已完成"));
        verify(pieceMapper, never()).insert(any(HcProductionInstructionChangeoverPieceDO.class));
    }

    @Test
    void shouldRejectAnotherPieceNumberForAnAlreadyCountedReport() {
        givenInstructionAndReport(instruction("EXECUTING"), report());
        HcProductionInstructionChangeoverPieceDO previous = donePiece(1321L);
        previous.setPieceNo("W26H163AP013A");
        when(pieceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(previous));

        ServiceException error = assertThrows(ServiceException.class, () -> service.recordChangeoverPiece(request()));

        assertTrue(error.getMessage().contains("不能重复计数"));
        verify(pieceMapper, never()).insert(any(HcProductionInstructionChangeoverPieceDO.class));
    }

    @Test
    void shouldRejectPreviousChangeoverModelBeforeRecordingOrCounting() {
        HcAdhesive2ReportDO report = report();
        report.setModelCode("W26P0200");
        givenInstructionAndReport(instruction("EXECUTING"), report);

        ServiceException error = assertThrows(ServiceException.class, () -> service.recordChangeoverPiece(request()));

        assertTrue(error.getMessage().contains("报工型号与换型指令目标型号不一致"));
        verifyNoInteractions(pieceMapper);
        verify(instructionMapper, never()).updateById(any(HcProductionInstructionDO.class));
    }

    @Test
    void shouldRejectPieceConfirmedBeforeThisChangeoverStarted() {
        HcAdhesive2ReportDO report = report();
        report.setConfirmerTime(START_TIME.minusSeconds(1));
        givenInstructionAndReport(instruction("EXECUTING"), report);

        ServiceException error = assertThrows(ServiceException.class, () -> service.recordChangeoverPiece(request()));

        assertTrue(error.getMessage().contains("历史片不能计入"));
        verifyNoInteractions(pieceMapper);
    }

    @Test
    void shouldRejectReportFromAnotherPlan() {
        HcAdhesive2ReportDO report = report();
        report.setPlanId(999L);
        givenInstructionAndReport(instruction("EXECUTING"), report);

        ServiceException error = assertThrows(ServiceException.class, () -> service.recordChangeoverPiece(request()));

        assertTrue(error.getMessage().contains("不属于当前换型指令计划"));
        verifyNoInteractions(pieceMapper);
    }

    @Test
    void shouldCompleteInstructionOnlyAfterInsertingTargetPiece() {
        HcProductionInstructionDO instruction = instruction("EXECUTING");
        givenInstructionAndReport(instruction, report());
        List<HcProductionInstructionChangeoverPieceDO> previousPieces = LongStream.rangeClosed(1, 6)
                .mapToObj(index -> HcProductionInstructionChangeoverPieceDO.builder()
                        .id(index).instructionId(34L).adhesive2ReportId(index)
                        .pieceNo("PREVIOUS-" + index).status("DONE").build())
                .toList();
        when(pieceMapper.selectList(any(Wrapper.class))).thenReturn(previousPieces);
        when(instructionMapper.selectById(34L)).thenReturn(instruction("COMPLETED"));

        HcProductionInstructionDO result = service.recordChangeoverPiece(request());

        ArgumentCaptor<HcProductionInstructionChangeoverPieceDO> pieceCaptor =
                ArgumentCaptor.forClass(HcProductionInstructionChangeoverPieceDO.class);
        verify(pieceMapper).insert(pieceCaptor.capture());
        assertEquals(34L, pieceCaptor.getValue().getInstructionId());
        assertEquals(1321L, pieceCaptor.getValue().getAdhesive2ReportId());
        assertEquals(PIECE_NO, pieceCaptor.getValue().getPieceNo());
        assertEquals("W26P0300", pieceCaptor.getValue().getActualModelCode());
        ArgumentCaptor<HcProductionInstructionDO> updateCaptor = ArgumentCaptor.forClass(HcProductionInstructionDO.class);
        verify(instructionMapper).updateById(updateCaptor.capture());
        assertEquals(7, updateCaptor.getValue().getCompletedQty());
        assertEquals("COMPLETED", updateCaptor.getValue().getExecuteStatus());
        assertNotNull(updateCaptor.getValue().getExecuteEndTime());
        assertEquals("COMPLETED", result.getExecuteStatus());
        verify(instructionMapper).selectByIdForUpdate(34L);
    }

    private void givenInstructionAndReport(HcProductionInstructionDO instruction, HcAdhesive2ReportDO report) {
        when(instructionMapper.selectByIdForUpdate(34L)).thenReturn(instruction);
        when(reportMapper.selectById(1321L)).thenReturn(report);
    }

    private HcProductionInstructionDO instruction(String executeStatus) {
        return HcProductionInstructionDO.builder()
                .id(34L).planId(134L).planOperationId(894L)
                .instructionType("CHANGEOVER").instructionNo("CHANGEOVER-34")
                .segmentBatchNo("W26H163AP").targetModelCode("W26P0300")
                .targetQty(7).completedQty("COMPLETED".equals(executeStatus) ? 7 : 6)
                .executeStatus(executeStatus).status("CONFIRMED")
                .executeStartTime(START_TIME)
                .executeEndTime("COMPLETED".equals(executeStatus) ? START_TIME.plusMinutes(10) : null)
                .build();
    }

    private HcAdhesive2ReportDO report() {
        return HcAdhesive2ReportDO.builder()
                .id(1321L).planId(134L).planOperationId(894L)
                .productionBatchNo(PIECE_NO).modelCode("W26P0300")
                .reportStatus("CONFIRMED").confirmerTime(START_TIME.plusMinutes(1))
                .build();
    }

    private HcProductionInstructionChangeoverPieceDO donePiece(Long reportId) {
        return HcProductionInstructionChangeoverPieceDO.builder()
                .id(50L).instructionId(34L).adhesive2ReportId(reportId)
                .pieceNo(PIECE_NO).status("DONE").build();
    }

    private HcProductionInstructionChangeoverPieceReqVO request() {
        HcProductionInstructionChangeoverPieceReqVO req = new HcProductionInstructionChangeoverPieceReqVO();
        req.setId(34L);
        req.setAdhesive2ReportId(1321L);
        req.setPieceNo(PIECE_NO);
        return req;
    }
}
