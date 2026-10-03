package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickCandidateSegmentRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HcFinishedPackagingShippingPickQuantityRuleTest {

    private final HcFinishedPackagingServiceImpl service = new HcFinishedPackagingServiceImpl();

    @Test
    void shouldAllowExactConfirmedPickAndPartialDraftPick() throws Exception {
        assertDoesNotThrow(() -> validateShippingPickQuantity(2, 2, 4, true));
        assertDoesNotThrow(() -> validateShippingPickQuantity(2, 1, 4, false));
    }

    @Test
    void shouldRejectConfirmedPickWhenQuantityIsInsufficientOrExcessive() {
        ServiceException insufficient = assertThrows(ServiceException.class,
                () -> validateShippingPickQuantity(2, 1, 4, true));
        ServiceException excessive = assertThrows(ServiceException.class,
                () -> validateShippingPickQuantity(3, 2, 4, false));

        assertTrue(insufficient.getMessage().contains("必须与客户要求发货数量一致"));
        assertTrue(excessive.getMessage().contains("不能超过客户要求发货数量"));
    }

    @Test
    void shouldRejectExplicitNoticeItemWhenItsCapacityIsAlreadyFull() {
        HcFgShippingNoticeItemDO noticeItem = HcFgShippingNoticeItemDO.builder()
                .id(101L)
                .lockedQty(2)
                .build();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> validateShippingNoticeItemPickCapacity(noticeItem, Map.of(noticeItem.getId(), 2)));

        assertTrue(exception.getMessage().contains("配货明细数量不能超过客户要求"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGroupShippingPickCandidatesByDisplayedSegmentBatchNo() throws Exception {
        FgStockLedgerRespVO warehousePiece = candidate(
                "W26E017AQ", "W26P0300", "W26E017AQ104B", "WAREHOUSE_STOCK");
        FgStockLedgerRespVO directPiece = candidate(
                "W26E017AQ", "OTHER-PRODUCTION-BATCH", "W26E017AQ105B", "PACKAGING_DIRECT");
        FgStockLedgerRespVO anotherSegmentPiece = candidate(
                "W26E018BQ", "W26P0300", "W26E018BQ101B", "WAREHOUSE_STOCK");

        List<ShippingNoticePickCandidateSegmentRespVO> segments =
                (List<ShippingNoticePickCandidateSegmentRespVO>) invoke(
                        "buildShippingNoticePickCandidateSegments",
                        new Class<?>[]{List.class},
                        List.of(warehousePiece, directPiece, anotherSegmentPiece));

        assertEquals(2, segments.size());
        assertEquals("W26E017AQ", segments.get(0).getSegmentBatchNo());
        assertEquals(2, segments.get(0).getTotalPieceCount());
        assertEquals(1, segments.get(0).getWarehousePieceCount());
        assertEquals(1, segments.get(0).getPackagingDirectPieceCount());
        assertEquals(List.of("W26E017AQ104B", "W26E017AQ105B"),
                segments.get(0).getSampleSliceBatchNos());
    }

    private FgStockLedgerRespVO candidate(String materialCode, String batchNo,
                                           String sliceBatchNo, String candidateType) {
        FgStockLedgerRespVO row = new FgStockLedgerRespVO();
        row.setMaterialCode(materialCode);
        row.setBatchNo(batchNo);
        row.setSliceBatchNo(sliceBatchNo);
        row.setCandidateType(candidateType);
        return row;
    }

    private void validateShippingPickQuantity(int pickedQty, int incomingQty, int requiredQty,
                                              boolean confirmPicked) throws Exception {
        invoke("validateShippingPickQuantity",
                new Class<?>[]{int.class, int.class, int.class, boolean.class},
                pickedQty, incomingQty, requiredQty, confirmPicked);
    }

    private void validateShippingNoticeItemPickCapacity(HcFgShippingNoticeItemDO noticeItem,
                                                        Map<Long, Integer> pickedCountMap) throws Exception {
        invoke("validateShippingNoticeItemPickCapacity",
                new Class<?>[]{HcFgShippingNoticeItemDO.class, Map.class}, noticeItem, pickedCountMap);
    }

    private Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = HcFinishedPackagingServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(service, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}
