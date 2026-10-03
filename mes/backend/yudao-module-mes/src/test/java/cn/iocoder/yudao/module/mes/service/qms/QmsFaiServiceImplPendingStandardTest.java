package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiBindStandardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemGroupAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.service.hc.productmodel.HcProductModelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsFaiServiceImplPendingStandardTest {

    @InjectMocks
    private QmsFaiServiceImpl service;

    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Mock
    private QmsFaiItemMapper qmsFaiItemMapper;
    @Mock
    private QmsFaiSampleMapper qmsFaiSampleMapper;
    @Mock
    private QmsFaiAbnormalMapper qmsFaiAbnormalMapper;
    @Mock
    private QmsFaiItemGroupAuditMapper qmsFaiItemGroupAuditMapper;
    @Mock
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Mock
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Mock
    private QmsNoGeneratorService qmsNoGeneratorService;
    @Mock
    private QmsQualityStandardContentHashService qmsQualityStandardContentHashService;
    @Mock
    private HcProductModelService hcProductModelService;

    @Test
    void shouldCreatePendingInspectionTaskWhenPushCannotMatchStandard() {
        when(qmsNoGeneratorService.generateNo("FAI")).thenReturn("FAI-PENDING-001");
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(qmsFaiOrderMapper.insert(any(QmsFaiOrderDO.class))).thenAnswer(invocation -> {
            QmsFaiOrderDO entity = invocation.getArgument(0);
            entity.setId(101L);
            return 1;
        });

        Long id = service.createFaiForInspectionPush(buildCreateReq());

        assertEquals(101L, id);
        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).insert(captor.capture());
        QmsFaiOrderDO inserted = captor.getValue();
        assertNull(inserted.getStandardId());
        assertNull(inserted.getStandardNo());
        assertEquals("PENDING", inserted.getStatus());
        assertTrue(inserted.getRemark().contains("待选择检验标准"));
    }

    @Test
    void shouldCreateEmptyManualRecordWhenStandardCannotBeMatched() {
        when(qmsNoGeneratorService.generateNo("FAI")).thenReturn("FAI-PENDING-001");
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(qmsFaiOrderMapper.insert(any(QmsFaiOrderDO.class))).thenAnswer(invocation -> {
            QmsFaiOrderDO entity = invocation.getArgument(0);
            entity.setId(102L);
            return 1;
        });

        Long id = service.createFai(buildCreateReq());

        assertEquals(102L, id);
        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).insert(captor.capture());
        assertNull(captor.getValue().getStandardId());
        assertTrue(captor.getValue().getRemark().contains("待选择检验标准"));
    }

    @Test
    void shouldMarkPackagingCoaInspectionAsRecheck() {
        QmsFaiSaveReqVO reqVO = buildCreateReq();
        reqVO.setSourceModule("PACKAGING_COA");
        when(qmsNoGeneratorService.generateNo("FAI")).thenReturn("FAI-COA-RECHECK-001");
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(qmsFaiOrderMapper.insert(any(QmsFaiOrderDO.class))).thenAnswer(invocation -> {
            QmsFaiOrderDO entity = invocation.getArgument(0);
            entity.setId(107L);
            return 1;
        });

        Long id = service.createFaiForInspectionPush(reqVO);

        assertEquals(107L, id);
        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).insert(captor.capture());
        assertTrue(captor.getValue().getRecheckFlag());
    }

    @Test
    void shouldCreateEmptyTaskWhenMultipleStandardsMatch() {
        QmsQualityStandardDO first = matchedStandard(301L, "STD-A");
        QmsQualityStandardDO second = matchedStandard(302L, "STD-B");
        QmsQualityStandardItemDO standardItem = QmsQualityStandardItemDO.builder()
                .inspectionItem("外观")
                .itemType("QUALITATIVE")
                .sampleSize(1)
                .build();
        when(qmsNoGeneratorService.generateNo("FAI")).thenReturn("FAI-PENDING-002");
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(first, second));
        when(qmsQualityStandardItemMapper.selectListByStandardId(301L)).thenReturn(List.of(standardItem));
        when(qmsQualityStandardItemMapper.selectListByStandardId(302L)).thenReturn(List.of(standardItem));
        when(qmsFaiOrderMapper.insert(any(QmsFaiOrderDO.class))).thenAnswer(invocation -> {
            QmsFaiOrderDO entity = invocation.getArgument(0);
            entity.setId(103L);
            return 1;
        });

        Long id = service.createFaiForInspectionPush(buildCreateReq());

        assertEquals(103L, id);
        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).insert(captor.capture());
        assertNull(captor.getValue().getStandardId());
        assertTrue(captor.getValue().getRemark().contains("匹配到多个已审核标准"));
    }

    @Test
    void shouldAutomaticallyBindTheOnlyMatchingStandard() {
        QmsQualityStandardDO standard = matchedStandard(303L, "STD-ONLY");
        standard.setVersion("1.0");
        QmsQualityStandardItemDO standardItem = QmsQualityStandardItemDO.builder()
                .id(403L)
                .standardId(303L)
                .inspectionItem("外观")
                .itemType("QUALITATIVE")
                .sampleSize(1)
                .build();
        when(qmsNoGeneratorService.generateNo("FAI")).thenReturn("FAI-ONLY-001");
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(standard));
        when(qmsQualityStandardItemMapper.selectListByStandardId(303L)).thenReturn(List.of(standardItem));
        when(qmsQualityStandardContentHashService.hashFaiStandardItems(any())).thenReturn("only-hash");
        when(qmsFaiOrderMapper.insert(any(QmsFaiOrderDO.class))).thenAnswer(invocation -> {
            QmsFaiOrderDO entity = invocation.getArgument(0);
            entity.setId(104L);
            return 1;
        });

        Long id = service.createFaiForInspectionPush(buildCreateReq());

        assertEquals(104L, id);
        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).insert(captor.capture());
        assertEquals(303L, captor.getValue().getStandardId());
        assertEquals("only-hash", captor.getValue().getStandardSnapshotHash());
        verify(qmsFaiItemMapper).insert(any(QmsFaiItemDO.class));
    }

    @Test
    void shouldPreferExactModelStandardOverFamilyStandard() {
        QmsFaiSaveReqVO reqVO = buildCreateReq();
        reqVO.setProductModel("W33P0200");
        QmsQualityStandardDO exact = matchedStandard(304L, "STD-W33P0200");
        exact.setProductModelId(35L);
        exact.setProductModelCode("W33P0200");
        exact.setVersion("1.0");
        QmsQualityStandardDO family = matchedStandard(305L, "STD-W33P");
        family.setProductModelId(47L);
        family.setProductModelCode("W33P");
        family.setVersion("1.0");
        QmsQualityStandardItemDO standardItem = standardItem(404L, 304L);
        when(hcProductModelService.getHcProductModelByCode("W33P0200"))
                .thenReturn(productModel(35L, "W33P0200", 47L));
        when(hcProductModelService.getHcProductModel(47L))
                .thenReturn(productModel(47L, "W33P", null));
        when(qmsNoGeneratorService.generateNo("FAI")).thenReturn("FAI-EXACT-001");
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(family, exact));
        when(qmsQualityStandardItemMapper.selectListByStandardId(304L)).thenReturn(List.of(standardItem));
        when(qmsQualityStandardItemMapper.selectListByStandardId(305L)).thenReturn(List.of(standardItem(405L, 305L)));
        when(qmsQualityStandardContentHashService.hashFaiStandardItems(any())).thenReturn("exact-hash");
        when(qmsFaiOrderMapper.insert(any(QmsFaiOrderDO.class))).thenAnswer(invocation -> {
            QmsFaiOrderDO entity = invocation.getArgument(0);
            entity.setId(105L);
            return 1;
        });

        service.createFaiForInspectionPush(reqVO);

        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).insert(captor.capture());
        assertEquals(304L, captor.getValue().getStandardId());
        assertEquals("EXACT_MODEL", captor.getValue().getStandardMatchType());
        assertEquals(35L, captor.getValue().getMatchedModelId());
    }

    @Test
    void shouldFallbackToFamilyStandardWhenExactStandardIsUnavailable() {
        QmsFaiSaveReqVO reqVO = buildCreateReq();
        reqVO.setProductModel("W33P0200");
        QmsQualityStandardDO family = matchedStandard(306L, "STD-W33P");
        family.setProductModelId(47L);
        family.setProductModelCode("W33P");
        family.setVersion("1.0");
        QmsQualityStandardItemDO standardItem = standardItem(406L, 306L);
        when(hcProductModelService.getHcProductModelByCode("W33P0200"))
                .thenReturn(productModel(35L, "W33P0200", 47L));
        when(hcProductModelService.getHcProductModel(47L))
                .thenReturn(productModel(47L, "W33P", null));
        when(qmsNoGeneratorService.generateNo("FAI")).thenReturn("FAI-FAMILY-001");
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(family));
        when(qmsQualityStandardItemMapper.selectListByStandardId(306L)).thenReturn(List.of(standardItem));
        when(qmsQualityStandardContentHashService.hashFaiStandardItems(any())).thenReturn("family-hash");
        when(qmsFaiOrderMapper.insert(any(QmsFaiOrderDO.class))).thenAnswer(invocation -> {
            QmsFaiOrderDO entity = invocation.getArgument(0);
            entity.setId(106L);
            return 1;
        });

        service.createFaiForInspectionPush(reqVO);

        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).insert(captor.capture());
        assertEquals(306L, captor.getValue().getStandardId());
        assertEquals("FAMILY_MODEL", captor.getValue().getStandardMatchType());
        assertEquals(47L, captor.getValue().getMatchedModelId());
        assertEquals("W33P", captor.getValue().getMatchedModelCode());
    }

    @Test
    void shouldRequireReasonWhenManuallySelectedStandardDoesNotMatchTaskScope() {
        QmsFaiOrderDO order = QmsFaiOrderDO.builder()
                .id(201L)
                .faiNo("FAI-PENDING-002")
                .status("PENDING")
                .sourceModule("WET_REPORT")
                .materialCode("MAT-A")
                .productModel("MODEL-A")
                .operationCode("OP-A")
                .operationName("工段A")
                .build();
        QmsQualityStandardDO standard = QmsQualityStandardDO.builder()
                .id(301L)
                .standardNo("STD-B")
                .version("1.0")
                .applyType("FAI")
                .status(1)
                .auditStatus(20)
                .materialCode("MAT-B")
                .processCode("OP-B")
                .processName("工段B")
                .build();
        QmsQualityStandardItemDO standardItem = QmsQualityStandardItemDO.builder()
                .id(401L)
                .standardId(301L)
                .inspectionItem("外观")
                .itemType("QUALITATIVE")
                .build();
        when(qmsFaiOrderMapper.selectById(201L)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(201L)).thenReturn(Collections.emptyList());
        when(qmsQualityStandardMapper.selectById(301L)).thenReturn(standard);
        when(qmsQualityStandardItemMapper.selectListByStandardId(301L)).thenReturn(List.of(standardItem));
        QmsFaiBindStandardReqVO reqVO = new QmsFaiBindStandardReqVO();
        reqVO.setId(201L);
        reqVO.setStandardId(301L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.bindStandard(reqVO));

        assertEquals(1008100130, exception.getCode());
    }

    @Test
    void shouldBindMatchedStandardAndCreateSnapshotItems() {
        QmsFaiOrderDO order = QmsFaiOrderDO.builder()
                .id(202L)
                .faiNo("FAI-PENDING-003")
                .status("PENDING")
                .materialCode("MAT-A")
                .productModel("MODEL-A")
                .operationCode("OP-A")
                .operationName("工段A")
                .build();
        QmsQualityStandardDO standard = QmsQualityStandardDO.builder()
                .id(302L)
                .standardNo("STD-A")
                .version("2.0")
                .applyType("FAI")
                .status(1)
                .auditStatus(20)
                .productModelCode("MODEL-A")
                .processCode("OP-A")
                .processName("工段A")
                .build();
        QmsQualityStandardItemDO standardItem = QmsQualityStandardItemDO.builder()
                .id(402L)
                .standardId(302L)
                .inspectionItem("外观")
                .itemType("QUALITATIVE")
                .sampleSize(1)
                .build();
        List<QmsFaiItemDO> snapshotItems = new ArrayList<>();
        when(qmsFaiOrderMapper.selectById(202L)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(202L)).thenAnswer(invocation -> snapshotItems);
        when(qmsQualityStandardMapper.selectById(302L)).thenReturn(standard);
        when(qmsQualityStandardItemMapper.selectListByStandardId(302L)).thenReturn(List.of(standardItem));
        when(qmsQualityStandardContentHashService.hashFaiStandardItems(any())).thenReturn("new-hash");
        when(qmsFaiOrderMapper.update(any(QmsFaiOrderDO.class), any())).thenReturn(1);
        when(qmsFaiItemMapper.insert(any(QmsFaiItemDO.class))).thenAnswer(invocation -> {
            QmsFaiItemDO item = invocation.getArgument(0);
            item.setId(502L);
            snapshotItems.add(item);
            return 1;
        });
        when(qmsFaiSampleMapper.selectListByFaiId(202L)).thenReturn(Collections.emptyList());
        when(qmsFaiAbnormalMapper.selectListByFaiId(202L)).thenReturn(Collections.emptyList());
        when(qmsFaiItemGroupAuditMapper.selectListByFaiId(202L)).thenReturn(Collections.emptyList());
        QmsFaiBindStandardReqVO reqVO = new QmsFaiBindStandardReqVO();
        reqVO.setId(202L);
        reqVO.setStandardId(302L);

        QmsFaiRespVO response = service.bindStandard(reqVO);

        assertEquals(302L, response.getStandardId());
        assertEquals("STD-A", response.getStandardNo());
        assertEquals(1, response.getItems().size());
        assertEquals(402L, response.getItems().get(0).getStandardItemId());
    }

    private QmsFaiSaveReqVO buildCreateReq() {
        QmsFaiSaveReqVO reqVO = new QmsFaiSaveReqVO();
        reqVO.setWorkOrderNo("WO-001");
        reqVO.setOperationCode("OP-A");
        reqVO.setOperationName("工段A");
        reqVO.setMachineCode("MC-001");
        reqVO.setMaterialCode("MAT-A");
        reqVO.setProductModel("MODEL-A");
        reqVO.setProductBatchNo("BATCH-001");
        reqVO.setProcessCategory("WET");
        reqVO.setSubmissionType("FIRST_ARTICLE");
        reqVO.setTriggerReason("NEW_ORDER");
        reqVO.setStandardMatchMode("PRODUCT_MODEL_PROCESS");
        reqVO.setRemark("报工首检申请");
        return reqVO;
    }

    private QmsQualityStandardDO matchedStandard(Long id, String standardNo) {
        return QmsQualityStandardDO.builder()
                .id(id)
                .standardNo(standardNo)
                .applyType("FAI")
                .status(1)
                .auditStatus(20)
                .productModelCode("MODEL-A")
                .processCode("OP-A")
                .processName("工段A")
                .build();
    }

    private QmsQualityStandardItemDO standardItem(Long id, Long standardId) {
        return QmsQualityStandardItemDO.builder()
                .id(id)
                .standardId(standardId)
                .inspectionItem("外观")
                .itemType("QUALITATIVE")
                .sampleSize(1)
                .build();
    }

    private HcProductModelDO productModel(Long id, String modelCode, Long parentModelId) {
        return HcProductModelDO.builder()
                .id(id)
                .modelCode(modelCode)
                .modelName(modelCode)
                .modelLevel(parentModelId == null ? "FAMILY" : "MODEL")
                .parentModelId(parentModelId)
                .status("ENABLE")
                .build();
    }
}
