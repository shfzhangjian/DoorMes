package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSliceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.*;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcFinishedPackagingCoaFreezeTest {
    @InjectMocks HcFinishedPackagingServiceImpl service;
    @Mock cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Mock cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper qmsFaiOrderMapper;
    @Mock HcFinishedStockMapper hcFinishedStockMapper;
    @Mock HcFinishedStockTxnLogMapper hcFinishedStockTxnLogMapper;

    private Object invoke(String name, Object... args) throws Exception {
        Method method = java.util.Arrays.stream(HcFinishedPackagingServiceImpl.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name) && m.getParameterCount() == args.length).findFirst().orElseThrow();
        method.setAccessible(true);
        return method.invoke(service, args);
    }
    private InspectionSliceRespVO piece(String fqc, String status, String coa) {
        InspectionSliceRespVO row = new InspectionSliceRespVO();
        row.setInspectionResult(fqc); row.setCoaInspectionStatus(status); row.setCoaInspectionResult(coa);
        return row;
    }
    private Object releasedCoa() throws Exception {
        Class<?> type = Class.forName(HcFinishedPackagingServiceImpl.class.getName() + "$CoaInspectionMeta");
        var constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
        Object meta = constructor.newInstance();
        Method accept = type.getDeclaredMethod("accept", QmsFaiOrderDO.class); accept.setAccessible(true);
        QmsFaiOrderDO fai = new QmsFaiOrderDO(); fai.setStatus("COMPLETED"); fai.setJudgment("OK"); fai.setFaiNo("TEST-COA");
        accept.invoke(meta, fai); return meta;
    }
    @Test void pendingAndNgCoaAreFrozen() throws Exception {
        for (InspectionSliceRespVO row : List.of(piece("OK", "PENDING", "PENDING"),
                piece("OK", "COMPLETED", "NG"), piece("OK", "WAITING_QA", "OK"))) {
            assertEquals("FROZEN", invoke("resolveInspectionSlicePackagingQualityStatus", row));
        }
    }
    @Test void waitingCoaUsesRecordIdMatchingSourceType() throws Exception {
        InspectionSliceRespVO row = new InspectionSliceRespVO();
        row.setSourceCutRoundReportId(101L);
        row.setSourceManualPieceId(202L);
        row.setSourceType("CUT_ROUND_REPORT");
        assertEquals(101L, invoke("resolveWaitingCoaSourceRecordId", row));
        row.setSourceType("MANUAL_HISTORY");
        assertEquals(202L, invoke("resolveWaitingCoaSourceRecordId", row));
    }
    @Test void waitingCoaRejectsMissingSourceWithoutUsingOtherSourceId() {
        for (String source : List.of("CUT_ROUND_REPORT", "MANUAL_HISTORY", "UNKNOWN")) {
            InspectionSliceRespVO row = new InspectionSliceRespVO();
            row.setSourceType(source);
            row.setSliceBatchNo("TEST-COA-001");
            if ("CUT_ROUND_REPORT".equals(source)) row.setSourceManualPieceId(202L);
            if ("MANUAL_HISTORY".equals(source)) row.setSourceCutRoundReportId(101L);
            var error = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> invoke("resolveWaitingCoaSourceRecordId", row));
            assertTrue(error.getCause().getMessage().contains("TEST-COA-001"));
            assertTrue(error.getCause().getMessage().contains(source));
        }
    }
    @Test void completedOkCoaReleasesButFqcNgKeepsNg() throws Exception {
        assertEquals("OK", invoke("resolveInspectionSlicePackagingQualityStatus", piece("OK", "COMPLETED", "OK")));
        assertEquals("NG", invoke("resolveInspectionSlicePackagingQualityStatus", piece("NG", "COMPLETED", "OK")));
    }
    @Test void frozenFilterDoesNotReturnOtherQueues() throws Exception {
        InspectionSliceRespVO frozen = piece("OK", null, null); frozen.setPackagingQualityStatus("FROZEN");
        InspectionSliceRespVO ok = piece("OK", "COMPLETED", "OK"); ok.setPackagingQualityStatus("OK");
        assertEquals(List.of(frozen), invoke("filterInspectionSlicesByPackagingQualityStatus", List.of(ok, frozen), "FROZEN"));
    }
    @Test void packageClassificationPrioritizesNgThenFrozen() throws Exception {
        var ok = HcInnerPackUnitItemDO.builder().qualityStatus("OK").build();
        var frozen = HcInnerPackUnitItemDO.builder().qualityStatus("FROZEN").build();
        var ng = HcInnerPackUnitItemDO.builder().qualityStatus("NG").build();
        assertEquals("FROZEN", invoke("resolvePackageQualityStatus", List.of(ok, frozen)));
        assertEquals("NG", invoke("resolvePackageQualityStatus", List.of(ng, frozen)));
    }
    @Test void coaUnfreezeKeepsShippingLockAndOtherNg() throws Exception {
        HcFinishedStockDO stock = HcFinishedStockDO.builder().id(1L).tenantId(1L).qty(1)
                .stockStatus("OUTBOUND_LOCKED").qualityStatus("NG").coaFrozen(true).coaFreezeReason("COA NG").build();
        invoke("applyStockCoaFreeze", stock, releasedCoa());
        assertEquals("OUTBOUND_LOCKED", stock.getStockStatus());
        assertEquals("NG", stock.getQualityStatus()); assertFalse(stock.getCoaFrozen());
        verify(hcFinishedStockTxnLogMapper).insert(any(HcFinishedStockTxnLogDO.class));
    }
    @Test void repeatedNormalWithoutCoaDoesNotDuplicateAudit() throws Exception {
        HcFinishedStockDO stock = HcFinishedStockDO.builder().id(1L).tenantId(1L).qty(1)
                .stockStatus("AVAILABLE").qualityStatus("OK").coaFrozen(false).coaFreezeReason("").build();
        invoke("applyStockCoaFreeze", stock, null);
        verifyNoInteractions(hcFinishedStockMapper, hcFinishedStockTxnLogMapper);
    }
    @Test void shippedStockIsNeverResynchronized() throws Exception {
        HcFinishedStockDO stock = HcFinishedStockDO.builder().id(1L).stockStatus("SHIPPED").coaFrozen(true).build();
        invoke("synchronizeStockCoaFreeze", stock);
        assertTrue(stock.getCoaFrozen()); verifyNoInteractions(hcFinishedStockMapper, hcFinishedStockTxnLogMapper);
    }
    @Test void missingCoaIsNormalForPackagingAndShipping() throws Exception {
        assertEquals("OK", invoke("resolveInspectionSlicePackagingQualityStatus", piece("OK", null, "UNKNOWN")));
        assertEquals("NG", invoke("resolveInspectionSlicePackagingQualityStatus", piece("NG", null, "UNKNOWN")));
        var stock = HcFinishedStockDO.builder().sliceBatchNo("W26H153AS001B").qualityStatus("OK").build();
        assertDoesNotThrow(() -> invoke("assertStockCoaReleased", stock));
    }
    @Test void deletedCoaDoesNotFreezeOrBlockShipping() throws Exception {
        QmsFaiOrderDO deleted = new QmsFaiOrderDO(); deleted.setId(9L); deleted.setDeleted(true);
        deleted.setProductBatchNo("W26H153AS"); deleted.setStatus("COMPLETED"); deleted.setJudgment("OK");
        when(qmsFaiOrderMapper.selectPackagingCoaListBySegmentBatchNos(any())).thenReturn(List.of(deleted));
        assertTrue(((java.util.Map<?, ?>) invoke("buildCoaInspectionMap", java.util.Set.of("W26H153AS"))).isEmpty());
        var stock = HcFinishedStockDO.builder().sliceBatchNo("W26H153AS001B").qualityStatus("FROZEN").build();
        assertDoesNotThrow(() -> invoke("assertStockCoaReleased", stock));
        verify(qmsFaiOrderMapper, never()).selectCoaOrderForUpdate(9L);
    }
    @Test void missingCoaUnfreezesWithoutChangingPickLock() throws Exception {
        var stock = HcFinishedStockDO.builder().id(1L).tenantId(1L).qty(1).sliceBatchNo("TEST")
                .stockStatus("OUTBOUND_LOCKED").qualityStatus("FROZEN").coaFrozen(true).coaFreezeReason("未送检").build();
        invoke("applyStockCoaFreeze", stock, null);
        assertEquals("OK", stock.getQualityStatus()); assertFalse(stock.getCoaFrozen());
        assertEquals("OUTBOUND_LOCKED", stock.getStockStatus());
        verify(hcFinishedStockTxnLogMapper).insert(any(HcFinishedStockTxnLogDO.class));
    }
    @Test void latestRecheckOkWinsOverHistoricalNg() throws Exception {
        QmsFaiOrderDO old = new QmsFaiOrderDO(); old.setId(1L); old.setProductBatchNo("W26H153AS");
        old.setRecheckGroupId(1L); old.setRecheckRoundNo(1); old.setStatus("COMPLETED"); old.setJudgment("NG");
        QmsFaiOrderDO latest = new QmsFaiOrderDO(); latest.setId(2L); latest.setProductBatchNo("W26H153AS");
        latest.setRecheckGroupId(1L); latest.setRecheckRoundNo(2); latest.setStatus("COMPLETED"); latest.setJudgment("OK");
        when(qmsFaiOrderMapper.selectPackagingCoaListBySegmentBatchNos(any())).thenReturn(List.of(old, latest));
        when(qmsFaiOrderMapper.selectCoaOrderForUpdate(1L)).thenReturn(old);
        when(qmsFaiOrderMapper.selectCoaOrderForUpdate(2L)).thenReturn(latest);
        var stock = HcFinishedStockDO.builder().sliceBatchNo("W26H153AS001B").qualityStatus("FROZEN").build();
        assertDoesNotThrow(() -> invoke("assertStockCoaReleased", stock));
    }
    @Test void freezeIsNotAnInspectionJudgment() {
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> invoke("normalizeInspectionResult", "FROZEN"));
    }
    @Test void returningFrozenStockKeepsOriginalFqcResult() throws Exception {
        var report = new cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO();
        report.setId(1L); report.setInspectionResult("OK");
        when(hcCutRoundReportMapper.selectById(1L)).thenReturn(report);
        var stock = HcFinishedStockDO.builder().sliceBatchNo("TEST-1").build();
        var notice = HcFgShippingNoticeDO.builder().id(1L).noticeNo("TEST-NOTICE").build();
        invoke("appendReturnInspectionRemark", 1L, null, stock, notice, "test", "退回", java.time.LocalDateTime.now(), "FROZEN");
        var update = org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO.class);
        verify(hcCutRoundReportMapper).updateById(update.capture());
        assertNull(update.getValue().getInspectionResult());
    }
}
