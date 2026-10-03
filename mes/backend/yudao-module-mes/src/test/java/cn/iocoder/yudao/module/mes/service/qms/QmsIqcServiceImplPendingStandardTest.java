package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsInspectionStandardCandidateRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsIqcServiceImplPendingStandardTest {

    @InjectMocks
    private QmsIqcServiceImpl service;

    @Mock
    private QmsIqcOrderMapper qmsIqcOrderMapper;
    @Mock
    private QmsIqcItemMapper qmsIqcItemMapper;
    @Mock
    private QmsQualityStandardMapper qmsQualityStandardMapper;
    @Mock
    private QmsQualityStandardItemMapper qmsQualityStandardItemMapper;
    @Mock
    private MesSupplierMapper mesSupplierMapper;
    @Mock
    private QmsNoGeneratorService qmsNoGeneratorService;
    @Mock
    private QmsQualityStandardContentHashService qmsQualityStandardContentHashService;

    @Test
    void shouldCreateEmptyIqcRecordWhenNoStandardMatches() {
        prepareCommonCreateMocks(601L);
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(Collections.emptyList());

        Long id = service.createIqc(buildCreateReq());

        assertEquals(601L, id);
        QmsIqcOrderDO inserted = captureInsertedOrder();
        assertNull(inserted.getStandardId());
        assertTrue(inserted.getRemark().contains("未找到匹配"));
    }

    @Test
    void shouldCreateEmptyIqcRecordWhenMultipleStandardsMatch() {
        prepareCommonCreateMocks(602L);
        QmsQualityStandardDO first = matchedStandard(701L, "IQC-A");
        QmsQualityStandardDO second = matchedStandard(702L, "IQC-B");
        QmsQualityStandardItemDO item = QmsQualityStandardItemDO.builder()
                .inspectionItem("外观")
                .itemType("QUALITATIVE")
                .sampleSize(1)
                .build();
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(first, second));
        when(qmsQualityStandardItemMapper.selectListByStandardId(701L)).thenReturn(List.of(item));
        when(qmsQualityStandardItemMapper.selectListByStandardId(702L)).thenReturn(List.of(item));

        Long id = service.createIqc(buildCreateReq());

        assertEquals(602L, id);
        QmsIqcOrderDO inserted = captureInsertedOrder();
        assertNull(inserted.getStandardId());
        assertTrue(inserted.getRemark().contains("匹配到多个同优先级已审核标准"));
    }

    @Test
    void shouldAutomaticallyBindTheOnlyMatchingIqcStandard() {
        prepareCommonCreateMocks(603L);
        QmsQualityStandardDO standard = matchedStandard(703L, "IQC-ONLY");
        standard.setVersion("2.0");
        QmsQualityStandardItemDO item = QmsQualityStandardItemDO.builder()
                .id(803L)
                .standardId(703L)
                .inspectionItem("外观")
                .itemType("QUALITATIVE")
                .sampleSize(1)
                .build();
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(standard));
        when(qmsQualityStandardItemMapper.selectListByStandardId(703L)).thenReturn(List.of(item));
        when(qmsQualityStandardContentHashService.hashIqcStandardItems(any())).thenReturn("iqc-only-hash");

        Long id = service.createIqc(buildCreateReq());

        assertEquals(603L, id);
        QmsIqcOrderDO inserted = captureInsertedOrder();
        assertEquals(703L, inserted.getStandardId());
        assertEquals("iqc-only-hash", inserted.getStandardSnapshotHash());
        verify(qmsIqcItemMapper).insert(any(QmsIqcItemDO.class));
    }

    @Test
    void shouldFallbackToUniversalIqcStandardWhenMaterialStandardMissing() {
        prepareCommonCreateMocks(604L);
        QmsQualityStandardDO standard = universalStandard(704L, "HC/W-21");
        standard.setMaterialId(999L);
        QmsQualityStandardItemDO item = standardItem(704L);
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(standard));
        when(qmsQualityStandardItemMapper.selectListByStandardId(704L)).thenReturn(List.of(item));
        when(qmsQualityStandardContentHashService.hashIqcStandardItems(any())).thenReturn("iqc-universal-hash");

        Long id = service.createIqc(buildCreateReq());

        assertEquals(604L, id);
        QmsIqcOrderDO inserted = captureInsertedOrder();
        assertEquals(704L, inserted.getStandardId());
        assertEquals("HC/W-21", inserted.getStandardNo());
        assertEquals("UNIVERSAL", inserted.getStandardMatchMode());
        assertEquals("MAT-A", inserted.getMaterialCode());
        assertEquals("物料A", inserted.getMaterialName());
    }

    @Test
    void shouldPreferMaterialStandardOverUniversalIqcStandard() {
        prepareCommonCreateMocks(605L);
        QmsQualityStandardDO materialStandard = matchedStandard(705L, "IQC-MAT");
        QmsQualityStandardDO universalStandard = universalStandard(706L, "HC/W-21");
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(universalStandard, materialStandard));
        when(qmsQualityStandardItemMapper.selectListByStandardId(705L)).thenReturn(List.of(standardItem(705L)));
        when(qmsQualityStandardItemMapper.selectListByStandardId(706L)).thenReturn(List.of(standardItem(706L)));
        when(qmsQualityStandardContentHashService.hashIqcStandardItems(any())).thenReturn("iqc-material-hash");

        Long id = service.createIqc(buildCreateReq());

        assertEquals(605L, id);
        QmsIqcOrderDO inserted = captureInsertedOrder();
        assertEquals(705L, inserted.getStandardId());
        assertEquals("IQC-MAT", inserted.getStandardNo());
        assertEquals("MATERIAL", inserted.getStandardMatchMode());
    }

    @Test
    void shouldReturnSelectableMaterialAndUniversalCandidatesByMaterial() {
        QmsQualityStandardDO materialStandard = matchedStandard(707L, "IQC-MAT");
        QmsQualityStandardDO universalStandard = universalStandard(708L, "HC/W-21");
        universalStandard.setMaterialId(999L);
        when(qmsQualityStandardMapper.selectList(any())).thenReturn(List.of(universalStandard, materialStandard));
        when(qmsQualityStandardItemMapper.selectListByStandardId(707L)).thenReturn(List.of(standardItem(707L)));
        when(qmsQualityStandardItemMapper.selectListByStandardId(708L)).thenReturn(List.of(standardItem(708L)));

        List<QmsInspectionStandardCandidateRespVO> candidates =
                service.getStandardCandidatesByMaterial(21L, "MAT-A");

        assertEquals(2, candidates.size());
        assertEquals(707L, candidates.get(0).getId());
        assertEquals("MATERIAL", candidates.get(0).getMatchType());
        assertTrue(candidates.get(0).getRecommended());
        assertEquals(708L, candidates.get(1).getId());
        assertEquals("UNIVERSAL", candidates.get(1).getMatchType());
        assertFalse(Boolean.TRUE.equals(candidates.get(1).getRecommended()));
    }

    private void prepareCommonCreateMocks(Long id) {
        when(qmsNoGeneratorService.generateNo("IQC")).thenReturn("IQC-PENDING-" + id);
        when(mesSupplierMapper.selectById(11L)).thenReturn(MesSupplierDO.builder()
                .id(11L)
                .supplierCode("SUP-01")
                .supplierName("合格供应商")
                .materialCode("MAT-A")
                .status("QUALIFIED")
                .build());
        when(qmsIqcOrderMapper.insert(any(QmsIqcOrderDO.class))).thenAnswer(invocation -> {
            QmsIqcOrderDO entity = invocation.getArgument(0);
            entity.setId(id);
            return 1;
        });
    }

    private QmsIqcOrderDO captureInsertedOrder() {
        ArgumentCaptor<QmsIqcOrderDO> captor = ArgumentCaptor.forClass(QmsIqcOrderDO.class);
        verify(qmsIqcOrderMapper).insert(captor.capture());
        return captor.getValue();
    }

    private QmsIqcSaveReqVO buildCreateReq() {
        QmsIqcSaveReqVO reqVO = new QmsIqcSaveReqVO();
        reqVO.setSupplierId(11L);
        reqVO.setMaterialId(21L);
        reqVO.setMaterialCode("MAT-A");
        reqVO.setMaterialName("物料A");
        reqVO.setBatchNo("BATCH-A");
        reqVO.setReceiveQty(BigDecimal.TEN);
        reqVO.setArrivalDate(LocalDate.of(2026, 7, 29));
        return reqVO;
    }

    private QmsQualityStandardDO matchedStandard(Long id, String standardNo) {
        return QmsQualityStandardDO.builder()
                .id(id)
                .standardNo(standardNo)
                .applyType("IQC")
                .status(1)
                .auditStatus(20)
                .materialId(21L)
                .materialCode("MAT-A")
                .build();
    }

    private QmsQualityStandardDO universalStandard(Long id, String standardNo) {
        return QmsQualityStandardDO.builder()
                .id(id)
                .standardNo(standardNo)
                .applyType("IQC")
                .status(1)
                .auditStatus(20)
                .build();
    }

    private QmsQualityStandardItemDO standardItem(Long standardId) {
        return QmsQualityStandardItemDO.builder()
                .id(800L + standardId)
                .standardId(standardId)
                .inspectionItem("外观")
                .itemType("QUALITATIVE")
                .sampleSize(1)
                .build();
    }
}
