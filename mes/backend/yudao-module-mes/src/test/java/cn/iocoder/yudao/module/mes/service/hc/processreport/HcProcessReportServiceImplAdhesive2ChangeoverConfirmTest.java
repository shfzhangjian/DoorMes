package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportConfirmReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionMapper;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.hc.productioninstruction.HcProductionInstructionService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HcProcessReportServiceImplAdhesive2ChangeoverConfirmTest {

    private final HcProcessReportServiceImpl target = new HcProcessReportServiceImpl();
    private final HcAdhesive2ReportMapper reports = mock(HcAdhesive2ReportMapper.class);
    private final HcProductionInstructionMapper instructions = mock(HcProductionInstructionMapper.class);
    private final HcProductionInstructionService instructionService = mock(HcProductionInstructionService.class);
    private final HcProcessFormRecordMapper inspections = mock(HcProcessFormRecordMapper.class);
    private final HcInvStockService stocks = mock(HcInvStockService.class);
    private HcProcessReportService service;
    private JdbcTemplate jdbc;
    private HcAdhesive2ReportDO report;
    private HcProductionInstructionDO instruction;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE report_state(id BIGINT PRIMARY KEY, status VARCHAR(32))");
        jdbc.execute("CREATE TABLE output_stock(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE changeover_piece(id BIGINT PRIMARY KEY)");
        jdbc.update("INSERT INTO report_state VALUES(1321, 'DRAFT')");
        HcPlanOrderMapper plans = mock(HcPlanOrderMapper.class);
        HcPlanOrderOperationMapper operations = mock(HcPlanOrderOperationMapper.class);
        ReflectionTestUtils.setField(target, "hcAdhesive2ReportMapper", reports);
        ReflectionTestUtils.setField(target, "hcProductionInstructionMapper", instructions);
        ReflectionTestUtils.setField(target, "hcProductionInstructionService", instructionService);
        ReflectionTestUtils.setField(target, "hcProcessFormRecordMapper", inspections);
        ReflectionTestUtils.setField(target, "hcInvStockService", stocks);
        ReflectionTestUtils.setField(target, "hcPlanOrderMapper", plans);
        ReflectionTestUtils.setField(target, "hcPlanOrderOperationMapper", operations);
        when(plans.selectById(111L)).thenReturn(HcPlanOrderDO.builder().id(111L).planNo("20260821-002").build());
        when(operations.selectById(894L)).thenReturn(HcPlanOrderOperationDO.builder()
                .id(894L).planId(111L).opCode("OP-ADHESIVE2").build());
        when(operations.selectByIdForUpdate(894L)).thenReturn(HcPlanOrderOperationDO.builder()
                .id(894L).planId(111L).opCode("OP-ADHESIVE2").build());
        report = HcAdhesive2ReportDO.builder().id(1321L).planId(111L).planOperationId(894L)
                .sourceBatchNo("W26H163AP").productionBatchNo("W26H163AP013B")
                .actualSizeRule("18").actualSizeSuffix("B").reportStatus("DRAFT")
                .modelCode("W26P0300").materialCode("03.13.10051")
                .extraJson("{\"changeoverInstructionId\":34}").build();
        instruction = HcProductionInstructionDO.builder().id(34L).planId(111L).planOperationId(894L)
                .segmentBatchNo("W26H163AP").instructionNo("PI-34").instructionType("CHANGEOVER")
                .targetModelCode("W26P0300").targetMaterialCode("03.13.10051")
                .executeStatus("EXECUTING").status("CONFIRMED").targetQty(7).build();
        when(reports.selectById(1321L)).thenReturn(report);
        when(reports.selectByIdForUpdate(1321L)).thenReturn(report);
        when(instructions.selectLatestEffectiveChangeoverForUpdate(111L, 894L, "W26H163AP")).thenReturn(instruction);
        HcProcessFormRecordDO inspection = new HcProcessFormRecordDO();
        inspection.setId(879L);
        inspection.setPlanOperationId(894L);
        inspection.setBatchNo("W26H163AP");
        inspection.setModelCode("W26P0300");
        inspection.setProcessCode("ADHESIVE2");
        inspection.setFormType("PRODUCTION_CHECK");
        inspection.setRecordStatus("CONFIRMED");
        inspection.setHeaderDataJson("{\"changeoverInstructionId\":34,\"modelCode\":\"W26P0300\"}");
        when(inspections.selectList(any(Wrapper.class))).thenReturn(List.of(inspection));
        when(reports.updateById(any(HcAdhesive2ReportDO.class))).thenAnswer(invocation -> {
            HcAdhesive2ReportDO update = invocation.getArgument(0);
            if (update.getReportStatus() != null) {
                jdbc.update("UPDATE report_state SET status=? WHERE id=?", update.getReportStatus(), update.getId());
            }
            return 1;
        });
        when(stocks.postProcessOutputWip(any())).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            jdbc.update("INSERT INTO output_stock VALUES(6298)");
            return HcInvStockDO.builder().id(6298L).lastTxnTime(LocalDateTime.now()).build();
        });
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        service = (HcProcessReportService) proxy.getProxy();
    }

    @Test
    void rejectsFirstModelBeforeConfirmingSecondChangeover() {
        report.setModelCode("W26P0200");
        report.setMaterialCode("03.13.10057");
        ServiceException error = assertThrows(ServiceException.class, () -> service.confirmAdhesive2Report(request()));
        assertTrue(error.getMessage().contains("当前换型目标"));
        assertEquals("DRAFT", status());
        verify(reports, never()).updateById(any(HcAdhesive2ReportDO.class));
        verifyNoInteractions(stocks, instructionService);
    }

    @Test
    void rejectsPreviousInstructionEvenWhenTargetModelIsSame() {
        report.setExtraJson("{\"changeoverInstructionId\":33}");
        assertThrows(ServiceException.class, () -> service.confirmAdhesive2Report(request()));
        assertEquals("DRAFT", status());
        verifyNoInteractions(stocks, instructionService);
    }

    @Test
    void rereadsReportAfterOperationLockInsteadOfConfirmingStaleDraft() {
        HcAdhesive2ReportDO submitted = HcAdhesive2ReportDO.builder()
                .id(1321L).planId(111L).planOperationId(894L).reportStatus("SUBMITTED").build();
        when(reports.selectByIdForUpdate(1321L)).thenReturn(submitted);
        ServiceException error = assertThrows(ServiceException.class, () -> service.confirmAdhesive2Report(request()));
        assertTrue(error.getMessage().contains("已过站提交"));
        verify(reports, never()).updateById(any(HcAdhesive2ReportDO.class));
        verifyNoInteractions(stocks, instructionService);
    }

    @Test
    void rollsBackConfirmationAndInventoryWhenPieceRecordingFails() {
        when(instructionService.recordChangeoverPiece(any())).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals("CONFIRMED", status());
            assertEquals(1, count("output_stock"));
            jdbc.update("INSERT INTO changeover_piece VALUES(1)");
            throw new IllegalStateException("模拟换型记片失败");
        });
        assertThrows(IllegalStateException.class, () -> service.confirmAdhesive2Report(request()));
        assertEquals("DRAFT", status());
        assertEquals(0, count("output_stock"));
        assertEquals(0, count("changeover_piece"));
    }

    @Test
    void commitsConfirmationInventoryAndPieceTogether() {
        when(instructionService.recordChangeoverPiece(any())).thenAnswer(invocation -> {
            assertEquals("CONFIRMED", status());
            jdbc.update("INSERT INTO changeover_piece VALUES(1)");
            return instruction;
        });
        assertEquals(1321L, service.confirmAdhesive2Report(request()));
        assertEquals("CONFIRMED", status());
        assertEquals(1, count("output_stock"));
        assertEquals(1, count("changeover_piece"));
    }

    @Test
    void keepsCompletedTargetForLaterProductionWithoutCountingExtraPieces() {
        instruction.setExecuteStatus("COMPLETED");
        assertEquals(1321L, service.confirmAdhesive2Report(request()));
        assertEquals("CONFIRMED", status());
        verifyNoInteractions(instructionService);
    }

    @Test
    void leavesUpstreamNgAttributionOutsideChangeover() {
        report.setSelfCheck("NG");
        report.setExtraJson("{\"preProcessSelfCheckAbnormal\":true,\"ngAttributionType\":\"PRE_PROCESS_SELF_CHECK\",\"ngAttributionProcessCode\":\"PRESS_SLOT\"}");
        assertNull(target.validateAdhesive2ConfirmChangeover(report));
        verify(instructions, never()).selectLatestEffectiveChangeoverForUpdate(any(), any(), any());
    }

    private HcAdhesiveReportConfirmReqVO request() {
        HcAdhesiveReportConfirmReqVO request = new HcAdhesiveReportConfirmReqVO();
        request.setId(1321L);
        request.setScannedBatchNo("W26H163AP013B");
        request.setConfirmerName("测试执行人");
        request.setConfirmerTime(LocalDateTime.of(2026, 9, 5, 10, 17));
        return request;
    }

    private String status() {
        return jdbc.queryForObject("SELECT status FROM report_state WHERE id=1321", String.class);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
