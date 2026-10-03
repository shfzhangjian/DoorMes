package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplGlueBoardUsageRecalculateTest {

    @InjectMocks
    private HcProcessReportServiceImpl service;

    @Mock
    private HcAdhesiveGlueBoardUsageMapper hcAdhesiveGlueBoardUsageMapper;
    @Mock
    private HcAdhesiveReportMapper hcAdhesiveReportMapper;
    @Mock
    private HcAdhesive2ReportMapper hcAdhesive2ReportMapper;
    @Mock
    private HcToolingConsumableConsumeMapper hcToolingConsumableConsumeMapper;

    @Test
    void shouldKeepLedgerConsumptionWhenRecalculatingAdhesive1GlueBoardUsage() throws Exception {
        HcAdhesiveGlueBoardUsageDO usage = buildLengthUsage(137L, new BigDecimal("93.5"));
        when(hcAdhesiveGlueBoardUsageMapper.selectById(137L)).thenReturn(usage);
        when(hcAdhesiveReportMapper.selectListByGlueBoardUsageId(137L)).thenReturn(List.of(
                HcAdhesiveReportDO.builder().glueBoardUseLength(new BigDecimal("17")).build()));
        when(hcAdhesive2ReportMapper.selectListByGlueBoardUsageId(137L)).thenReturn(List.of());
        when(hcToolingConsumableConsumeMapper.sumConsumeQtyByGlueBoardUsageId(137L))
                .thenReturn(new BigDecimal("20"));

        assertEquals(0, new BigDecimal("131.5").compareTo(resolveAvailableStart(usage)));
        assertEquals(0, new BigDecimal("62").compareTo(resolveAvailableLength(usage)));
        invokeRecalculate(137L);

        assertRecalculatedBalance(new BigDecimal("131.5"));
    }

    @Test
    void shouldKeepLedgerConsumptionWhenRecalculatingAdhesive2GlueBoardUsage() throws Exception {
        HcAdhesiveGlueBoardUsageDO usage = buildLengthUsage(138L, BigDecimal.ZERO);
        when(hcAdhesiveGlueBoardUsageMapper.selectById(138L)).thenReturn(usage);
        when(hcAdhesiveReportMapper.selectListByGlueBoardUsageId(138L)).thenReturn(List.of());
        when(hcAdhesive2ReportMapper.selectListByGlueBoardUsageId(138L)).thenReturn(List.of(
                HcAdhesive2ReportDO.builder().glueBoardUseLength(new BigDecimal("17")).build()));
        when(hcToolingConsumableConsumeMapper.sumConsumeQtyByGlueBoardUsageId(138L))
                .thenReturn(new BigDecimal("20"));

        invokeRecalculate(138L);

        assertRecalculatedBalance(new BigDecimal("38"));
    }

    @Test
    void shouldDeductFixedLossWhenCalculatingAdhesive1GlueBoardUsage() throws Exception {
        assertEquals(0, new BigDecimal("95").compareTo(calculateAdhesiveGlueBoardUsage(
                new BigDecimal("100"), new BigDecimal("5"))));
        assertEquals(0, BigDecimal.ZERO.compareTo(calculateAdhesiveGlueBoardUsage(
                new BigDecimal("5"), new BigDecimal("8"))));
    }

    private HcAdhesiveGlueBoardUsageDO buildLengthUsage(Long id, BigDecimal receiveStartPosition) {
        return HcAdhesiveGlueBoardUsageDO.builder()
                .id(id)
                .stockMeasureMode("LENGTH")
                .receiveStartPosition(receiveStartPosition)
                .receiveLength(new BigDecimal("100"))
                .aqcSampleLength(BigDecimal.ONE)
                .lossLength(BigDecimal.ZERO)
                .availableStartPosition(receiveStartPosition.add(new BigDecimal("18")))
                .availableLength(new BigDecimal("82"))
                .build();
    }

    private void invokeRecalculate(Long usageId) throws Exception {
        Method method = HcProcessReportServiceImpl.class
                .getDeclaredMethod("recalculateGlueBoardUsage", Long.class);
        method.setAccessible(true);
        method.invoke(service, usageId);
    }

    private BigDecimal calculateAdhesiveGlueBoardUsage(BigDecimal inputLength, BigDecimal lossLength) throws Exception {
        Method method = HcProcessReportServiceImpl.class
                .getDeclaredMethod("calculateAdhesiveGlueBoardUseLength", BigDecimal.class, BigDecimal.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, inputLength, lossLength);
    }

    private BigDecimal resolveAvailableStart(HcAdhesiveGlueBoardUsageDO usage) throws Exception {
        Method method = HcProcessReportServiceImpl.class
                .getDeclaredMethod("resolveGlueBoardAvailableStartPosition", HcAdhesiveGlueBoardUsageDO.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, usage);
    }

    private BigDecimal resolveAvailableLength(HcAdhesiveGlueBoardUsageDO usage) throws Exception {
        Method method = HcProcessReportServiceImpl.class
                .getDeclaredMethod("resolveGlueBoardAvailableLength", HcAdhesiveGlueBoardUsageDO.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, usage);
    }

    private void assertRecalculatedBalance(BigDecimal expectedAvailableStart) {
        ArgumentCaptor<HcAdhesiveGlueBoardUsageDO> captor =
                ArgumentCaptor.forClass(HcAdhesiveGlueBoardUsageDO.class);
        verify(hcAdhesiveGlueBoardUsageMapper).updateById(captor.capture());
        HcAdhesiveGlueBoardUsageDO update = captor.getValue();
        assertEquals(0, new BigDecimal("37").compareTo(update.getConsumedLength()));
        assertEquals(0, expectedAvailableStart.compareTo(update.getAvailableStartPosition()));
        assertEquals(0, new BigDecimal("62").compareTo(update.getAvailableLength()));
        assertEquals(0, expectedAvailableStart.compareTo(update.getReturnedStartPosition()));
        assertEquals(0, new BigDecimal("62").compareTo(update.getReturnedLength()));
    }
}
