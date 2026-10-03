package cn.iocoder.yudao.module.mes.service.hc.processreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingProductionRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.equipment.HcEquipmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingProductionLedgerMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HcGrindingProductionRecordLedgerServiceImplResearchPadTypeTest {

    @Mock
    private HcGrindingConsumptionService grindingConsumptionService;

    @InjectMocks
    private HcGrindingProductionRecordLedgerServiceImpl service;

    @Mock
    private HcGrindingProductionLedgerMapper ledgerMapper;
    @Mock
    private HcEquipmentMapper equipmentMapper;
    @Mock
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"THIRD", "FOURTH"})
    void shouldCreateBlackPadResearchRecordWithoutProductModelClassification(String passType) {
        when(padTypeResolver.normalizePadType("BLACK_PAD")).thenReturn("BLACK_PAD");
        when(equipmentMapper.selectById(8L)).thenReturn(equipment(8L, "BLACK_PAD"));

        service.create(request("RND-NOT-IN-PRODUCT-MASTER", "BLACK_PAD", passType, 8L));

        ArgumentCaptor<HcGrindingProductionRecordDO> recordCaptor =
                ArgumentCaptor.forClass(HcGrindingProductionRecordDO.class);
        verify(ledgerMapper).insert(recordCaptor.capture());
        assertEquals("RND-NOT-IN-PRODUCT-MASTER", recordCaptor.getValue().getModelCode());
        assertEquals("BLACK_PAD", recordCaptor.getValue().getPadType());
        assertEquals(passType, recordCaptor.getValue().getPassType());
        verify(padTypeResolver, never()).resolveByModelCode(anyString());
    }

    @Test
    void shouldStillRejectEquipmentThatDoesNotMatchSelectedResearchPadType() {
        when(padTypeResolver.normalizePadType("BLACK_PAD")).thenReturn("BLACK_PAD");
        when(equipmentMapper.selectById(7L)).thenReturn(equipment(7L, "WHITE_PAD"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.create(request("RND-BLACK", "BLACK_PAD", "FOURTH", 7L)));

        assertEquals("所选设备不适用于当前类型", exception.getMessage());
        verify(ledgerMapper, never()).insert(
                org.mockito.ArgumentMatchers.<HcGrindingProductionRecordDO>any());
    }

    private HcGrindingProductionRecordSaveReqVO request(String modelCode, String padType,
                                                         String passType, Long equipmentId) {
        HcGrindingProductionRecordSaveReqVO reqVO = new HcGrindingProductionRecordSaveReqVO();
        reqVO.setEquipmentId(equipmentId);
        reqVO.setModelCode(modelCode);
        reqVO.setPadType(padType);
        reqVO.setMaterialCode("RND-MATERIAL");
        reqVO.setBatchNo("RND-BATCH-001");
        reqVO.setPassType(passType);
        reqVO.setRecorderName("研发员");
        reqVO.setRecordTime(LocalDateTime.of(2026, 9, 1, 15, 30));
        reqVO.setSandpaperChanged(false);
        reqVO.setGuideClothChanged(false);
        return reqVO;
    }

    private HcEquipmentDO equipment(Long id, String applicablePadType) {
        return HcEquipmentDO.builder()
                .id(id)
                .equipmentCode("EQ-" + id)
                .equipmentName("研发磨皮设备")
                .applicablePadType(applicablePadType)
                .tenantId(1L)
                .build();
    }
}
