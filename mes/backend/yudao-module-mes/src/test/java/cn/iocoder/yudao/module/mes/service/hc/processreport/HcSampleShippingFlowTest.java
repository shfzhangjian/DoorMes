package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeSaveLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeChangeReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.*;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.*;
import jakarta.validation.Validation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcSampleShippingFlowTest {
    @InjectMocks private HcFinishedPackagingServiceImpl service;
    @Mock private HcFgShippingNoticeItemMapper hcFgShippingNoticeItemMapper;
    @Mock private HcFgShippingNoticeMapper hcFgShippingNoticeMapper;
    @Mock private HcFgShippingNoticePickItemMapper hcFgShippingNoticePickItemMapper;
    @Mock private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Mock private QmsFqcShippingDetailMapper qmsFqcShippingDetailMapper;

    @Test
    void sampleCustomerIsOptionalButMassAndRndStillRequireCustomer() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            ShippingNoticeSaveLockReqVO req = new ShippingNoticeSaveLockReqVO();
            req.setProductType("SAMPLE"); req.setNoticeQty(2);
            assertTrue(validator.validate(req).isEmpty());
            req.setProductType("MASS");
            assertTrue(validator.validate(req).stream().anyMatch(v -> v.getMessage().equals("发货客户不能为空")));
            req.setProductType("RND");
            assertFalse(validator.validate(req).isEmpty());
            req.setCustomerName("客户");
            assertTrue(validator.validate(req).isEmpty());
            ShippingNoticeChangeReqVO change = new ShippingNoticeChangeReqVO();
            change.setProductType("SAMPLE"); change.setNoticeQty(2);
            change.setChangeReason("样品调整"); change.setExpectedChangeVersion(1);
            assertTrue(validator.validate(change).isEmpty());
        }
    }

    @Test
    void quantityOnlySampleGetsOneExecutionRowPerPieceAndCanBePreparedTwice() throws Exception {
        AtomicLong ids = new AtomicLong(100);
        when(hcFgShippingNoticeItemMapper.insert(any(HcFgShippingNoticeItemDO.class))).thenAnswer(call -> {
            call.<HcFgShippingNoticeItemDO>getArgument(0).setId(ids.incrementAndGet()); return 1;
        });
        var notice = notice("SAMPLE", 3);
        List<HcFgShippingNoticeItemDO> rows = prepare(notice, List.of());
        assertEquals(3, rows.size());
        assertEquals(3, rows.stream().map(HcFgShippingNoticeItemDO::getId).distinct().count());
        assertTrue(rows.stream().allMatch(row -> row.getLockedQty() == 1 && row.getCustomerProductBatchNo() == null
                && row.getPackageSliceNo() == null && row.getInternalModelCode() == null));
        assertEquals(rows, prepare(notice, rows));
        verify(hcFgShippingNoticeItemMapper, times(3)).insert(any(HcFgShippingNoticeItemDO.class));
    }

    @Test
    void multiPieceRequirementIsSplitWithoutLosingInternalConstraints() throws Exception {
        var row = HcFgShippingNoticeItemDO.builder().id(9L).lockedQty(3)
                .internalModelCode("W33P").internalItemCode("W33B").remark("备注").build();
        var rows = prepare(notice("SAMPLE", 3), List.of(row));
        assertEquals(3, rows.size());
        assertTrue(rows.stream().allMatch(item -> item.getLockedQty() == 1
                && "W33P".equals(item.getInternalModelCode()) && "W33B".equals(item.getInternalItemCode())));
        assertEquals(9L, rows.get(0).getId());
        verify(hcFgShippingNoticeItemMapper, times(2)).insert(any(HcFgShippingNoticeItemDO.class));
    }

    @Test
    void sampleRejectsQuantityMismatchAndDoesNotSplitAlreadyBoundLegacyRow() {
        assertThrows(RuntimeException.class, () -> prepare(notice("SAMPLE", 3),
                List.of(HcFgShippingNoticeItemDO.builder().lockedQty(2).build())));
        assertThrows(RuntimeException.class, () -> prepare(notice("SAMPLE", 2),
                List.of(HcFgShippingNoticeItemDO.builder().lockedQty(2).actualSliceBatchNo("ACTUAL-1").build())));
        verifyNoInteractions(hcFgShippingNoticeItemMapper);
    }

    @Test
    void massRequirementsAreNotSplit() throws Exception {
        var rows = List.of(HcFgShippingNoticeItemDO.builder().lockedQty(3).build());
        assertSame(rows, prepare(notice("MASS", 3), rows));
        verifyNoInteractions(hcFgShippingNoticeItemMapper);
    }

    @Test
    void sampleNeedsAuditedOkAndActiveMatchingPickButDoesNotNeedAlignment() throws Exception {
        var detail = detail("OK", "NOT_REQUIRED");
        when(qmsFqcOrderMapper.selectById(8L)).thenReturn(QmsFqcOrderDO.builder().id(8L).status("COMPLETED").build());
        when(hcFgShippingNoticeMapper.selectById(1L)).thenReturn(notice("SAMPLE", 1));
        var pick = HcFgShippingNoticePickItemDO.builder().id(6L).noticeId(1L).sourceNoticeItemId(7L)
                .lockStatus("INSPECTED").actualSliceBatchNo("ACTUAL-1").build();
        when(hcFgShippingNoticePickItemMapper.selectById(6L)).thenReturn(pick);
        assertTrue(qualified(detail));
        pick.setActualSliceBatchNo("WRONG"); assertFalse(qualified(detail));
        pick.setActualSliceBatchNo("ACTUAL-1"); pick.setLockStatus("CANCELLED"); assertFalse(qualified(detail));
        pick.setLockStatus("INSPECTED"); pick.setSourceNoticeItemId(99L); assertFalse(qualified(detail));
    }

    @Test
    void massCannotSkipAlignmentAndSampleCannotSkipAuditOrNg() throws Exception {
        var order = QmsFqcOrderDO.builder().id(8L).status("WAITING_QA").build();
        when(qmsFqcOrderMapper.selectById(8L)).thenReturn(order);
        assertFalse(qualified(detail("OK", "NOT_REQUIRED")));
        assertFalse(qualified(detail("NG", "NOT_REQUIRED")));
        order.setStatus("COMPLETED");
        when(hcFgShippingNoticeMapper.selectById(1L)).thenReturn(notice("MASS", 1));
        assertFalse(qualified(detail("OK", "PENDING")));
        assertTrue(qualified(detail("OK", "ALIGNED")));
        verifyNoInteractions(hcFgShippingNoticePickItemMapper);
    }

    private HcFgShippingNoticeDO notice(String type, int qty) {
        return HcFgShippingNoticeDO.builder().id(1L).noticeNo("S-001").tenantId(1L)
                .productType(type).noticeQty(qty).requiredShipQty(qty).noticeStatus("DRAFT").build();
    }
    private QmsFqcShippingDetailDO detail(String judgment, String alignment) {
        return QmsFqcShippingDetailDO.builder().id(5L).fqcId(8L).shippingNoticeId(1L)
                .shippingNoticeItemId(7L).shippingPickItemId(6L).actualSliceBatchNo("ACTUAL-1")
                .rowJudgment(judgment).alignmentStatus(alignment).build();
    }
    @SuppressWarnings("unchecked")
    private List<HcFgShippingNoticeItemDO> prepare(HcFgShippingNoticeDO notice, List<HcFgShippingNoticeItemDO> rows) throws Exception {
        return (List<HcFgShippingNoticeItemDO>) invoke("prepareSampleExecutionItems",
                new Class<?>[]{HcFgShippingNoticeDO.class, List.class}, notice, rows);
    }
    private boolean qualified(QmsFqcShippingDetailDO detail) throws Exception {
        return (boolean) invoke("isShippingFqcAlignedAndAudited", new Class<?>[]{QmsFqcShippingDetailDO.class}, detail);
    }
    private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = HcFinishedPackagingServiceImpl.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        try { return method.invoke(service, args); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }
}
