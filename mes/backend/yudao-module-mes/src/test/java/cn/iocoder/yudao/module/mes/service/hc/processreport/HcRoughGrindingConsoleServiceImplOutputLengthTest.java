package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportAbnormalPositionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcWetReportAbnormalPositionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingAllocationModeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSecondDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcWetReportAbnormalPositionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSecondDetailMapper;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HcRoughGrindingConsoleServiceImplOutputLengthTest {

    private final HcRoughGrindingConsoleServiceImpl service = new HcRoughGrindingConsoleServiceImpl();

    @Test
    void shouldDeductAbnormalLengthFromSecondGrindingOutput() throws Exception {
        BigDecimal result = calculateSecondOutputLength(
                new BigDecimal("20"), new BigDecimal("2"), new BigDecimal("1"), null, new BigDecimal("4"));

        assertEquals(0, new BigDecimal("13").compareTo(result));
    }

    @Test
    void shouldRejectDeductionTotalGreaterThanProcessLength() throws Exception {
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> calculateSecondOutputLength(
                        new BigDecimal("20"), new BigDecimal("2"), new BigDecimal("1"), BigDecimal.ZERO, new BigDecimal("22")));

        assertTrue(exception.getCause() instanceof ServiceException);
        assertEquals("固定损耗、NAP留样、研发消耗和异常米数合计不能超过投入米数", exception.getCause().getMessage());
    }

    @Test
    void shouldRequirePositiveLengthForEachAbnormalPosition() throws Exception {
        HcWetReportAbnormalPositionSaveReqVO position = new HcWetReportAbnormalPositionSaveReqVO();
        position.setPositionText("10-11m");

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> validateAndSumSecondAbnormalLength(List.of(position)));

        assertTrue(exception.getCause() instanceof ServiceException);
        assertEquals("异常位置米数必须大于0", exception.getCause().getMessage());
    }

    @Test
    void shouldDeductResearchConsumptionAndAbnormalTogether() throws Exception {
        assertEquals(0, new BigDecimal("90").compareTo(calculateSecondOutputLength(
                new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("1"),
                new BigDecimal("3"), new BigDecimal("4"))));
    }

    @Test
    void shouldAllowExactlyZeroOutput() throws Exception {
        assertEquals(0, BigDecimal.ZERO.compareTo(calculateSecondOutputLength(
                new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("1"),
                new BigDecimal("3"), new BigDecimal("4"))));
    }

    @Test
    void shouldPreserveThreeDecimalPrecision() throws Exception {
        assertEquals(0, new BigDecimal("0.994").compareTo(calculateSecondOutputLength(
                BigDecimal.ONE, new BigDecimal("0.001"), new BigDecimal("0.002"),
                new BigDecimal("0.003"), BigDecimal.ZERO)));
    }

    @Test
    void shouldRejectResearchConsumptionOverBalance() {
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> calculateSecondOutputLength(new BigDecimal("10"), new BigDecimal("2"),
                        BigDecimal.ONE, new BigDecimal("8"), BigDecimal.ZERO));
        assertTrue(exception.getCause() instanceof ServiceException);
    }

    @Test
    void shouldRejectNegativeResearchConsumption() {
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> calculateSecondOutputLength(BigDecimal.TEN, BigDecimal.ZERO,
                        BigDecimal.ZERO, new BigDecimal("-1"), BigDecimal.ZERO));
        assertTrue(exception.getCause() instanceof ServiceException);
    }

    @Test
    void shouldBalanceAllocatedInputAndIgnoreStaleClientOutput() {
        HcGrindingFirstAllocationMapper allocations = mock(HcGrindingFirstAllocationMapper.class);
        HcGrindingFirstDetailMapper firstDetails = mock(HcGrindingFirstDetailMapper.class);
        HcGrindingSecondDetailMapper secondDetails = mock(HcGrindingSecondDetailMapper.class);
        ReflectionTestUtils.setField(service, "hcGrindingFirstAllocationMapper", allocations);
        ReflectionTestUtils.setField(service, "hcGrindingFirstDetailMapper", firstDetails);
        ReflectionTestUtils.setField(service, "hcGrindingSecondDetailMapper", secondDetails);
        HcGrindingFirstAllocationDO allocation = HcGrindingFirstAllocationDO.builder()
                .id(1L).allocationModeId(2L).planId(3L).planOperationId(4L).firstDetailId(5L)
                .motherBatchNo("TEST-MOTHER").segmentMark("P").productionBatchNo("TEST-MOTHER-P")
                .confirmedLength(new BigDecimal("100")).startPosition(BigDecimal.ZERO).build();
        when(allocations.selectByIdForUpdate(1L)).thenReturn(allocation);
        when(firstDetails.selectById(5L)).thenReturn(new HcGrindingFirstDetailDO());
        HcPlanOrderDO plan = new HcPlanOrderDO();
        plan.setId(3L);
        HcPlanOrderOperationDO operation = new HcPlanOrderOperationDO();
        operation.setId(4L);
        HcGrindingAllocationModeDO mode = new HcGrindingAllocationModeDO();
        mode.setId(2L);
        HcRoughConsoleSecondReportSaveReqVO request = new HcRoughConsoleSecondReportSaveReqVO();
        request.setFirstAllocationId(1L);
        request.setMotherBatchNo("TEST-MOTHER");
        request.setProcessLength(new BigDecimal("100"));
        request.setLossLength(new BigDecimal("2"));
        request.setNapSampleLength(BigDecimal.ONE);
        request.setResearchConsumptionLength(new BigDecimal("3"));
        request.setOutputLength(new BigDecimal("97")); // 旧客户端未扣研发/异常。
        HcWetReportAbnormalPositionSaveReqVO abnormal = new HcWetReportAbnormalPositionSaveReqVO();
        abnormal.setPositionText("10-14m");
        abnormal.setAbnormalLength(new BigDecimal("4"));
        request.setAbnormalPositions(List.of(abnormal));

        ReflectionTestUtils.invokeMethod(service, "inheritSecondReportFromFirstAllocation", request, plan, operation, mode);

        assertEquals(0, new BigDecimal("90").compareTo(request.getOutputLength()));
        assertEquals(0, new BigDecimal("100").compareTo(request.getProcessLength()));
        assertEquals("TEST-MOTHER-P", request.getProductionBatchNo());
    }

    @Test
    void shouldKeepResearchAndAbnormalDeductionsWhenSampleChanges() {
        HcGrindingSecondDetailMapper details = mock(HcGrindingSecondDetailMapper.class);
        HcWetReportAbnormalPositionMapper abnormalities = mock(HcWetReportAbnormalPositionMapper.class);
        ReflectionTestUtils.setField(service, "hcGrindingSecondDetailMapper", details);
        ReflectionTestUtils.setField(service, "hcWetReportAbnormalPositionMapper", abnormalities);
        HcWetReportAbnormalPositionDO abnormal = new HcWetReportAbnormalPositionDO();
        abnormal.setAbnormalLength(new BigDecimal("4"));
        when(abnormalities.selectListBySource(anyString(), eq("SECOND_GRINDING"), eq(1L)))
                .thenReturn(List.of(abnormal));
        HcGrindingSecondDetailDO detail = HcGrindingSecondDetailDO.builder().id(1L)
                .processLength(new BigDecimal("100")).lossLength(new BigDecimal("2"))
                .napSampleLength(BigDecimal.ONE).researchConsumptionLength(new BigDecimal("3"))
                .outputLength(new BigDecimal("90")).build();

        ReflectionTestUtils.invokeMethod(service, "syncSecondSegmentSampleLength", detail, new BigDecimal("2"));

        assertEquals(0, new BigDecimal("89").compareTo(detail.getOutputLength()));
        assertEquals(0, new BigDecimal("3").compareTo(detail.getResearchConsumptionLength()));
        verify(details).updateById(argThat((HcGrindingSecondDetailDO update) ->
                new BigDecimal("89").compareTo(update.getOutputLength()) == 0));
    }

    @Test
    void shouldValidateResearchPrecisionAndNegativeRequestValues() {
        try (jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            HcRoughConsoleSecondReportSaveReqVO request = new HcRoughConsoleSecondReportSaveReqVO();
            request.setResearchConsumptionLength(new BigDecimal("0.0001"));
            assertTrue(factory.getValidator().validateProperty(request, "researchConsumptionLength").size() > 0);
            request.setResearchConsumptionLength(new BigDecimal("-1"));
            assertTrue(factory.getValidator().validateProperty(request, "researchConsumptionLength").size() > 0);
            request.setResearchConsumptionLength(new BigDecimal("0.001"));
            assertTrue(factory.getValidator().validateProperty(request, "researchConsumptionLength").isEmpty());
            request.setResearchConsumptionLength(null);
            assertTrue(factory.getValidator().validateProperty(request, "researchConsumptionLength").isEmpty());
        }
    }

    private BigDecimal calculateSecondOutputLength(BigDecimal processLength, BigDecimal lossLength,
                                                   BigDecimal napSampleLength, BigDecimal researchConsumptionLength, BigDecimal abnormalLength) throws Exception {
        Method method = HcRoughGrindingConsoleServiceImpl.class.getDeclaredMethod(
                "calculateSecondOutputLength", BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class, BigDecimal.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, processLength, lossLength, napSampleLength, researchConsumptionLength, abnormalLength);
    }

    @SuppressWarnings("unchecked")
    private BigDecimal validateAndSumSecondAbnormalLength(
            List<HcWetReportAbnormalPositionSaveReqVO> positions) throws Exception {
        Method method = HcRoughGrindingConsoleServiceImpl.class.getDeclaredMethod(
                "validateAndSumSecondAbnormalLength", List.class);
        method.setAccessible(true);
        return (BigDecimal) method.invoke(service, positions);
    }
}
