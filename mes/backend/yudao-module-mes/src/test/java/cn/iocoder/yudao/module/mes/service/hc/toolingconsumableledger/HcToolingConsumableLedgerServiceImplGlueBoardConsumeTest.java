package cn.iocoder.yudao.module.mes.service.hc.toolingconsumableledger;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.unit.UnitDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.unit.UnitMapper;
import cn.iocoder.yudao.module.mes.service.hc.finishedglueboardmap.HcFinishedGlueBoardMapService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcToolingConsumableLedgerServiceImplGlueBoardConsumeTest {

    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingConsumptionMapper grindingConsumptionMapper;

    @InjectMocks
    private HcToolingConsumableLedgerServiceImpl service;

    @Mock
    private HcToolingConsumableLedgerMapper hcToolingConsumableLedgerMapper;
    @Mock
    private HcToolingConsumableConsumeMapper hcToolingConsumableConsumeMapper;
    @Mock
    private HcAdhesiveGlueBoardStockMapper hcAdhesiveGlueBoardStockMapper;
    @Mock
    private HcAdhesiveGlueBoardUsageMapper hcAdhesiveGlueBoardUsageMapper;
    @Mock
    private HcFinishedGlueBoardMapService finishedGlueBoardMapService;
    @Mock
    private UnitMapper unitMapper;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void linkedGrindingConsumptionCannotBeEditedOrDeletedFromLedger() {
        when(grindingConsumptionMapper.referencesConsume(99L)).thenReturn(true);
        HcToolingConsumableConsumeSaveReqVO req = new HcToolingConsumableConsumeSaveReqVO();
        req.setId(99L);
        assertThrows(RuntimeException.class, () -> service.updateConsume(req));
        assertThrows(RuntimeException.class, () -> service.deleteConsume(99L));
        verify(hcToolingConsumableConsumeMapper, never()).deleteById(99L);
    }

    @Test
    void shouldConsumeFromGlueBoardStockWhenNoActiveUsage() {
        HcToolingConsumableLedgerDO ledger = buildLedger();
        HcAdhesiveGlueBoardStockDO stock = HcAdhesiveGlueBoardStockDO.builder()
                .id(10L)
                .toolingLedgerId(1L)
                .receiveStartPosition(BigDecimal.ZERO)
                .receiveLength(new BigDecimal("100"))
                .availableStartPosition(new BigDecimal("20"))
                .availableLength(new BigDecimal("80"))
                .usedLength(new BigDecimal("20"))
                .lossLength(BigDecimal.ZERO)
                .build();
        mockCommon(ledger, stock);
        when(hcAdhesiveGlueBoardUsageMapper.selectListByGlueBoardStockId(10L)).thenReturn(List.of());
        when(hcAdhesiveGlueBoardUsageMapper.existsActiveByGlueBoardStockId(10L)).thenReturn(false);

        service.createConsume(buildRequest(new BigDecimal("10")));

        ArgumentCaptor<HcToolingConsumableConsumeDO> consumeCaptor =
                ArgumentCaptor.forClass(HcToolingConsumableConsumeDO.class);
        verify(hcToolingConsumableConsumeMapper).insert(consumeCaptor.capture());
        assertEquals(10L, consumeCaptor.getValue().getGlueBoardStockId());
        assertEquals("LEDGER_MANUAL", consumeCaptor.getValue().getConsumeSource());
        assertEquals("NORMAL", consumeCaptor.getValue().getConsumeType());
        assertEquals("W33P0100", consumeCaptor.getValue().getProductModelCode());
        assertEquals("03.13.10053", consumeCaptor.getValue().getProductMaterialCode());
        assertEquals("RND-20260715-01", consumeCaptor.getValue().getProductBatchNo());
        assertEquals(0, new BigDecimal("10.000").compareTo(consumeCaptor.getValue().getProductInputQty()));
        assertEquals(0, new BigDecimal("9.000").compareTo(consumeCaptor.getValue().getProductOutputQty()));

        ArgumentCaptor<HcAdhesiveGlueBoardStockDO> stockCaptor =
                ArgumentCaptor.forClass(HcAdhesiveGlueBoardStockDO.class);
        verify(hcAdhesiveGlueBoardStockMapper).updateById(stockCaptor.capture());
        assertEquals(0, new BigDecimal("30").compareTo(stockCaptor.getValue().getAvailableStartPosition()));
        assertEquals(0, new BigDecimal("70").compareTo(stockCaptor.getValue().getAvailableLength()));
        assertEquals(0, new BigDecimal("30").compareTo(stockCaptor.getValue().getUsedLength()));
        verify(hcAdhesiveGlueBoardUsageMapper, never())
                .updateById(any(HcAdhesiveGlueBoardUsageDO.class));
    }

    @Test
    void shouldConsumeFromActiveUsageWithoutDeductingStockAgain() {
        HcToolingConsumableLedgerDO ledger = buildLedger();
        HcAdhesiveGlueBoardStockDO stock = HcAdhesiveGlueBoardStockDO.builder()
                .id(10L)
                .toolingLedgerId(1L)
                .availableLength(BigDecimal.ZERO)
                .build();
        HcAdhesiveGlueBoardUsageDO usage = HcAdhesiveGlueBoardUsageDO.builder()
                .id(20L)
                .planOperationId(30L)
                .planNo("PLAN-001")
                .operationCode("ADHESIVE1")
                .operationName("粘胶1")
                .glueBoardStockId(10L)
                .receiveStartPosition(BigDecimal.ZERO)
                .receiveLength(new BigDecimal("100"))
                .consumedLength(new BigDecimal("10"))
                .availableStartPosition(new BigDecimal("10"))
                .availableLength(new BigDecimal("90"))
                .usageStatus("ACTIVE")
                .build();
        mockCommon(ledger, stock);
        when(hcAdhesiveGlueBoardUsageMapper.selectListByGlueBoardStockId(10L)).thenReturn(List.of(usage));
        when(hcAdhesiveGlueBoardUsageMapper.selectByIdForUpdate(20L)).thenReturn(usage);
        when(hcAdhesiveGlueBoardUsageMapper.existsActiveByGlueBoardStockId(10L)).thenReturn(true);

        service.createConsume(buildRequest(new BigDecimal("20")));

        ArgumentCaptor<HcToolingConsumableConsumeDO> consumeCaptor =
                ArgumentCaptor.forClass(HcToolingConsumableConsumeDO.class);
        verify(hcToolingConsumableConsumeMapper).insert(consumeCaptor.capture());
        assertEquals(10L, consumeCaptor.getValue().getGlueBoardStockId());
        assertEquals(20L, consumeCaptor.getValue().getGlueBoardUsageId());
        assertEquals(30L, consumeCaptor.getValue().getPlanOperationId());
        assertEquals("PLAN-001", consumeCaptor.getValue().getPlanNo());

        ArgumentCaptor<HcAdhesiveGlueBoardUsageDO> usageCaptor =
                ArgumentCaptor.forClass(HcAdhesiveGlueBoardUsageDO.class);
        verify(hcAdhesiveGlueBoardUsageMapper).updateById(usageCaptor.capture());
        assertEquals(0, new BigDecimal("30").compareTo(usageCaptor.getValue().getConsumedLength()));
        assertEquals(0, new BigDecimal("30").compareTo(usageCaptor.getValue().getAvailableStartPosition()));
        assertEquals(0, new BigDecimal("70").compareTo(usageCaptor.getValue().getAvailableLength()));

        ArgumentCaptor<HcAdhesiveGlueBoardStockDO> stockCaptor =
                ArgumentCaptor.forClass(HcAdhesiveGlueBoardStockDO.class);
        verify(hcAdhesiveGlueBoardStockMapper).updateById(stockCaptor.capture());
        assertEquals("LOCKED", stockCaptor.getValue().getStockStatus());
    }

    @Test
    void shouldFillLedgerPageWithActiveGlueBoardUsageBalance() {
        HcToolingConsumableLedgerDO ledger = buildLedger();
        HcAdhesiveGlueBoardStockDO stock = HcAdhesiveGlueBoardStockDO.builder()
                .id(10L)
                .toolingLedgerId(1L)
                .availableLength(new BigDecimal("80"))
                .usedLength(new BigDecimal("20"))
                .lossLength(BigDecimal.ZERO)
                .build();
        HcAdhesiveGlueBoardUsageDO usage = HcAdhesiveGlueBoardUsageDO.builder()
                .id(20L)
                .glueBoardStockId(10L)
                .operationCode("ADHESIVE1")
                .usageStatus("ACTIVE")
                .aqcSampleLength(new BigDecimal("5"))
                .consumedLength(new BigDecimal("15"))
                .lossLength(new BigDecimal("2"))
                .availableLength(new BigDecimal("78"))
                .build();
        when(hcToolingConsumableLedgerMapper.selectPage(any(HcToolingConsumableLedgerPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(ledger), 1L));
        when(hcToolingConsumableConsumeMapper.selectListByLedgerIds(List.of(1L))).thenReturn(List.of());
        when(hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(1L)).thenReturn(stock);
        when(hcAdhesiveGlueBoardUsageMapper.selectListByGlueBoardStockId(10L)).thenReturn(List.of(usage));

        PageResult<HcToolingConsumableLedgerDO> page = service.getLedgerPage(
                new HcToolingConsumableLedgerPageReqVO());

        assertEquals(0, new BigDecimal("22").compareTo(page.getList().get(0).getConsumedQty()));
        assertEquals(0, new BigDecimal("78").compareTo(page.getList().get(0).getBalanceQty()));
    }

    @Test
    void shouldOverrideSubmittedGlueBoardMaterialCodeWithFinishedGlueBoardMap() {
        HcFinishedGlueBoardMapItemDO mapItem = buildMapItem("ADHESIVE1", "PT-217KV", "01.02.00002");
        when(finishedGlueBoardMapService.getGlueBoardModelItems("ADHESIVE1", null)).thenReturn(List.of(mapItem));
        mockMeterUnit();

        service.createLedger(buildGlueBoardLedgerRequest("PT-217KV", "WRONG-CODE"));

        ArgumentCaptor<HcToolingConsumableLedgerDO> ledgerCaptor =
                ArgumentCaptor.forClass(HcToolingConsumableLedgerDO.class);
        verify(hcToolingConsumableLedgerMapper).insert(ledgerCaptor.capture());
        assertEquals("01.02.00002", ledgerCaptor.getValue().getErpMaterialCode());
    }

    @Test
    void shouldRejectGlueBoardModelWithMultipleMaterialCodes() {
        when(finishedGlueBoardMapService.getGlueBoardModelItems("ADHESIVE2", null)).thenReturn(List.of(
                buildMapItem("ADHESIVE2", "W250", "01.02.00025"),
                buildMapItem("ADHESIVE2", "W250", "01.02.00045")));
        mockMeterUnit();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.createLedger(buildGlueBoardLedgerRequest("W250", "01.02.00025", "ADHESIVE2")));

        assertTrue(exception.getMessage().contains("对应多个料号"));
    }

    @Test
    void shouldRejectRAndDSampleProductInputOrOutputNotGreaterThanZero() {
        when(hcToolingConsumableLedgerMapper.selectById(1L)).thenReturn(buildLedger());
        HcToolingConsumableConsumeSaveReqVO zeroInput = buildRequest(new BigDecimal("1"));
        zeroInput.setProductInputQty(BigDecimal.ZERO);

        RuntimeException inputException = assertThrows(RuntimeException.class,
                () -> service.createConsume(zeroInput));
        assertTrue(inputException.getMessage().contains("投入米数(m)必须大于0"));

        HcToolingConsumableConsumeSaveReqVO zeroOutput = buildRequest(new BigDecimal("1"));
        zeroOutput.setProductOutputQty(BigDecimal.ZERO);
        RuntimeException outputException = assertThrows(RuntimeException.class,
                () -> service.createConsume(zeroOutput));
        assertTrue(outputException.getMessage().contains("产出米数(m)必须大于0"));
    }

    @Test
    void shouldRegisterReturnUsingServerCalculatedBalance() {
        HcToolingConsumableLedgerDO ledger = HcToolingConsumableLedgerDO.builder()
                .id(1L)
                .consumableType("SANDPAPER")
                .receiveQty(new BigDecimal("10"))
                .usageStatus("ACTIVE")
                .build();
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger);
        when(hcToolingConsumableConsumeMapper.sumConsumeQtyByLedgerId(1L, null))
                .thenReturn(new BigDecimal("3.500"));

        HcToolingConsumableLedgerReturnReqVO request = new HcToolingConsumableLedgerReturnReqVO();
        request.setId(1L);
        request.setReturnReason("工序切换，剩余耗材退回");
        request.setReturnAuthUserId(100L);
        request.setReturnAuthUserName("测试认证人");

        service.returnLedger(request);

        ArgumentCaptor<HcToolingConsumableLedgerDO> captor =
                ArgumentCaptor.forClass(HcToolingConsumableLedgerDO.class);
        verify(hcToolingConsumableLedgerMapper).updateById(captor.capture());
        assertEquals("RETURNED", captor.getValue().getUsageStatus());
        assertEquals(0, new BigDecimal("6.500").compareTo(captor.getValue().getReturnQty()));
        assertEquals("工序切换，剩余耗材退回", captor.getValue().getReturnReason());
        assertEquals("测试认证人", captor.getValue().getReturnAuthUserName());
    }

    @Test
    void shouldRejectGlueBoardReturnWhenActiveProcessUsageExists() {
        HcToolingConsumableLedgerDO ledger = buildLedger();
        HcAdhesiveGlueBoardStockDO stock = HcAdhesiveGlueBoardStockDO.builder()
                .id(10L)
                .toolingLedgerId(1L)
                .availableLength(new BigDecimal("80"))
                .build();
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger);
        when(hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(1L)).thenReturn(stock);
        when(hcAdhesiveGlueBoardStockMapper.selectByIdForUpdate(10L)).thenReturn(stock);
        when(hcAdhesiveGlueBoardUsageMapper.existsActiveByGlueBoardStockId(10L)).thenReturn(true);

        HcToolingConsumableLedgerReturnReqVO request = new HcToolingConsumableLedgerReturnReqVO();
        request.setId(1L);
        request.setReturnReason("退回剩余胶板");
        request.setReturnAuthUserName("测试认证人");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.returnLedger(request));

        assertTrue(exception.getMessage().contains("请先在报工看板退回释放"));
        verify(hcToolingConsumableLedgerMapper, never()).updateById(any(HcToolingConsumableLedgerDO.class));
    }

    private void mockCommon(HcToolingConsumableLedgerDO ledger, HcAdhesiveGlueBoardStockDO stock) {
        when(hcToolingConsumableLedgerMapper.selectById(1L)).thenReturn(ledger);
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger);
        when(hcToolingConsumableConsumeMapper.sumConsumeQtyByLedgerId(1L, null)).thenReturn(BigDecimal.ZERO);
        when(hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(1L)).thenReturn(stock);
        when(hcAdhesiveGlueBoardStockMapper.selectById(10L)).thenReturn(stock);
        when(hcAdhesiveGlueBoardStockMapper.selectByIdForUpdate(10L)).thenReturn(stock);
    }

    private HcToolingConsumableLedgerDO buildLedger() {
        return HcToolingConsumableLedgerDO.builder()
                .id(1L)
                .consumableType("GLUE_BOARD")
                .consumableTypeName("胶板")
                .processCode("ADHESIVE1")
                .processName("粘胶1")
                .model("PT-217KV")
                .batchNo("GB-001")
                .receiveQty(new BigDecimal("100"))
                .usageStatus("ACTIVE")
                .build();
    }

    private HcFinishedGlueBoardMapItemDO buildMapItem(String processCode, String model, String materialCode) {
        HcFinishedGlueBoardMapItemDO item = new HcFinishedGlueBoardMapItemDO();
        item.setGlueProcess(processCode);
        item.setGlueBoardModel(model);
        item.setGlueBoardMaterialCode(materialCode);
        return item;
    }

    private void mockMeterUnit() {
        when(unitMapper.selectById(1L)).thenReturn(UnitDO.builder()
                .id(1L)
                .code("m")
                .name("米")
                .build());
    }

    private HcToolingConsumableLedgerSaveReqVO buildGlueBoardLedgerRequest(String model, String erpMaterialCode) {
        return buildGlueBoardLedgerRequest(model, erpMaterialCode, "ADHESIVE1");
    }

    private HcToolingConsumableLedgerSaveReqVO buildGlueBoardLedgerRequest(String model, String erpMaterialCode,
                                                                             String processCode) {
        HcToolingConsumableLedgerSaveReqVO request = new HcToolingConsumableLedgerSaveReqVO();
        request.setConsumableType("GLUE_BOARD");
        request.setProcessCode(processCode);
        request.setModel(model);
        request.setBatchNo("GB-MAP-001");
        request.setErpMaterialCode(erpMaterialCode);
        request.setReceiveQty(new BigDecimal("100"));
        request.setReceiveTime(LocalDateTime.of(2026, 7, 18, 20, 0));
        request.setUomId(1L);
        request.setReceiverName("测试人员");
        return request;
    }

    private HcToolingConsumableConsumeSaveReqVO buildRequest(BigDecimal consumeQty) {
        HcToolingConsumableConsumeSaveReqVO request = new HcToolingConsumableConsumeSaveReqVO();
        request.setLedgerId(1L);
        request.setConsumableType("GLUE_BOARD");
        request.setProcessCode("ADHESIVE1");
        request.setBatchNo("GB-001");
        request.setConsumeQty(consumeQty);
        request.setConsumeTime(LocalDateTime.of(2026, 7, 15, 20, 30));
        request.setProductModelCode("W33P0100");
        request.setProductMaterialCode("03.13.10053");
        request.setProductBatchNo("RND-20260715-01");
        request.setProductInputQty(new BigDecimal("10.000"));
        request.setProductOutputQty(new BigDecimal("9.000"));
        return request;
    }
}
