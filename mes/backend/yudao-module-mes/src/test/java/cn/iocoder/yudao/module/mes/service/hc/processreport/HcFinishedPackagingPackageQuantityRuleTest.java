package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeItemReqVO;
import java.math.BigDecimal;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HcFinishedPackagingPackageQuantityRuleTest {

    private final HcFinishedPackagingServiceImpl service = new HcFinishedPackagingServiceImpl();

    @Test
    void shouldAllowBatchPackagingForQualifiedAndUnqualifiedPieces() {
        assertDoesNotThrow(() -> validateInboundPackagePieceCount(31, "OK"));
        assertDoesNotThrow(() -> validateInboundPackagePieceCount(31, "NG"));
    }

    @Test
    void shouldRejectEmptyInboundPackageSelection() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> validateInboundPackagePieceCount(0, "OK"));
        assertTrue(exception.getMessage().contains("请选择待包装片"));
    }

    @Test
    void shouldKeepManualAuxiliaryMaterialConsumeQty() throws Exception {
        PackageAuxConsumeItemReqVO first = new PackageAuxConsumeItemReqVO();
        first.setLedgerId(11L);
        first.setConsumeQty(BigDecimal.ONE);
        PackageAuxConsumeItemReqVO second = new PackageAuxConsumeItemReqVO();
        second.setLedgerId(12L);
        second.setConsumeQty(new BigDecimal("99"));

        List<PackageAuxConsumeItemReqVO> normalizedItems = normalizeInboundPackageAuxConsumeItems(
                List.of(first, second));

        assertEquals(2, normalizedItems.size());
        assertEquals(BigDecimal.ONE, normalizedItems.get(0).getConsumeQty());
        assertEquals(new BigDecimal("99"), normalizedItems.get(1).getConsumeQty());
    }

    @Test
    void shouldRejectNonPositiveManualAuxiliaryMaterialConsumeQty() {
        PackageAuxConsumeItemReqVO item = new PackageAuxConsumeItemReqVO();
        item.setLedgerId(11L);
        item.setConsumeQty(BigDecimal.ZERO);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> normalizeInboundPackageAuxConsumeItems(List.of(item)));

        assertTrue(exception.getMessage().contains("本次领用量必须大于0"));
    }

    private void validateInboundPackagePieceCount(int pieceCount, String qualityStatus) throws Exception {
        Method method = HcFinishedPackagingServiceImpl.class.getDeclaredMethod(
                "validateInboundPackagePieceCount", int.class, String.class);
        method.setAccessible(true);
        try {
            method.invoke(service, pieceCount, qualityStatus);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    @SuppressWarnings("unchecked")
    private List<PackageAuxConsumeItemReqVO> normalizeInboundPackageAuxConsumeItems(
            List<PackageAuxConsumeItemReqVO> consumeItems) throws Exception {
        Method method = HcFinishedPackagingServiceImpl.class.getDeclaredMethod(
                "normalizeInboundPackageAuxConsumeItems", List.class);
        method.setAccessible(true);
        try {
            return (List<PackageAuxConsumeItemReqVO>) method.invoke(service, consumeItems);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}
