package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskItemSelectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskWizardCreateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcSampleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsQualityStandardMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsDispatchTaskInspectionServiceTest {

    @InjectMocks
    private QmsDispatchTaskInspectionService service;

    @Mock
    private QmsQualityStandardMapper standardMapper;
    @Mock
    private QmsQualityStandardItemMapper standardItemMapper;
    @Mock
    private QmsFaiOrderMapper faiOrderMapper;
    @Mock
    private QmsFaiItemMapper faiItemMapper;
    @Mock
    private QmsFaiSampleMapper faiSampleMapper;
    @Mock
    private QmsFqcOrderMapper fqcOrderMapper;
    @Mock
    private QmsFqcItemMapper fqcItemMapper;
    @Mock
    private QmsFqcSampleMapper fqcSampleMapper;
    @Mock
    private QmsIqcOrderMapper iqcOrderMapper;
    @Mock
    private QmsIqcItemMapper iqcItemMapper;
    @Mock
    private QmsIqcSampleMapper iqcSampleMapper;
    @Mock
    private QmsNoGeneratorService noGeneratorService;

    @Test
    void shouldExposeIqcItemMethodInstrumentAndMeasuredData() {
        QmsIqcItemDO item = QmsIqcItemDO.builder()
                .id(101L)
                .iqcId(9L)
                .standardItemId(201L)
                .inspectionItem("外观")
                .standardDesc("无破损")
                .unit("件")
                .inspectionMethod("目视")
                .testTool("检验灯")
                .sampleSize(2)
                .itemResult("OK")
                .sort(10)
                .build();
        QmsIqcSampleDO first = QmsIqcSampleDO.builder()
                .iqcId(9L).iqcItemId(101L).sampleSeq(1).qualitativeValue("无破损").build();
        QmsIqcSampleDO second = QmsIqcSampleDO.builder()
                .iqcId(9L).iqcItemId(101L).sampleSeq(2).measuredValue(new BigDecimal("1.20")).build();
        when(iqcItemMapper.selectListByIqcId(9L)).thenReturn(List.of(item));
        when(iqcSampleMapper.selectListByIqcId(9L)).thenReturn(List.of(first, second));

        var items = service.getSourceItems("IQC", 9L);

        assertEquals(1, items.size());
        assertEquals("目视", items.get(0).getInspectionMethod());
        assertEquals("检验灯", items.get(0).getTestTool());
        assertEquals(2, items.get(0).getSampleSize());
        assertEquals("第1组：无破损、第2组：1.20", items.get(0).getMeasuredData());
    }

    @Test
    void shouldResetOnlySelectedIqcItemsAndSamplesWhenRechecking() {
        QmsIqcOrderDO source = QmsIqcOrderDO.builder()
                .id(9L)
                .iqcNo("IQC-OLD")
                .status("COMPLETED")
                .judgment("NG")
                .build();
        QmsIqcItemDO selectedItem = QmsIqcItemDO.builder()
                .id(101L)
                .iqcId(9L)
                .iqcNo("IQC-OLD")
                .inspectionItem("外观")
                .maxValue(new BigDecimal("2.00"))
                .minValue(new BigDecimal("1.00"))
                .averageValue(new BigDecimal("1.50"))
                .itemResult("NG")
                .build();
        QmsIqcItemDO retainedItem = QmsIqcItemDO.builder()
                .id(102L)
                .iqcId(9L)
                .iqcNo("IQC-OLD")
                .inspectionItem("尺寸")
                .maxValue(new BigDecimal("8.00"))
                .minValue(new BigDecimal("7.00"))
                .averageValue(new BigDecimal("7.50"))
                .itemResult("OK")
                .build();
        QmsIqcSampleDO selectedSample = QmsIqcSampleDO.builder()
                .id(1001L)
                .iqcId(9L)
                .iqcItemId(101L)
                .iqcNo("IQC-OLD")
                .sampleSeq(1)
                .measuredValue(new BigDecimal("2.00"))
                .sampleResult("NG")
                .defectCode("D-001")
                .build();
        QmsIqcSampleDO retainedSample = QmsIqcSampleDO.builder()
                .id(1002L)
                .iqcId(9L)
                .iqcItemId(102L)
                .iqcNo("IQC-OLD")
                .sampleSeq(1)
                .measuredValue(new BigDecimal("7.50"))
                .sampleResult("OK")
                .build();
        when(iqcOrderMapper.selectById(9L)).thenReturn(source);
        when(iqcItemMapper.selectListByIqcId(9L)).thenReturn(List.of(selectedItem, retainedItem));
        when(iqcSampleMapper.selectListByIqcId(9L)).thenReturn(List.of(selectedSample, retainedSample));
        when(noGeneratorService.generateNo("IQC")).thenReturn("IQC-NEW");

        List<QmsIqcOrderDO> insertedOrders = new ArrayList<>();
        List<QmsIqcItemDO> insertedItems = new ArrayList<>();
        List<QmsIqcSampleDO> insertedSamples = new ArrayList<>();
        AtomicLong itemId = new AtomicLong(201L);
        AtomicLong sampleId = new AtomicLong(301L);
        when(iqcOrderMapper.insert(any(QmsIqcOrderDO.class))).thenAnswer(invocation -> {
            QmsIqcOrderDO order = invocation.getArgument(0);
            order.setId(19L);
            insertedOrders.add(order);
            return 1;
        });
        when(iqcItemMapper.insert(any(QmsIqcItemDO.class))).thenAnswer(invocation -> {
            QmsIqcItemDO item = invocation.getArgument(0);
            item.setId(itemId.getAndIncrement());
            insertedItems.add(item);
            return 1;
        });
        when(iqcSampleMapper.insert(any(QmsIqcSampleDO.class))).thenAnswer(invocation -> {
            QmsIqcSampleDO sample = invocation.getArgument(0);
            sample.setId(sampleId.getAndIncrement());
            insertedSamples.add(sample);
            return 1;
        });

        QmsDispatchTaskInspectionService.GeneratedInspection generated =
                service.cloneForTaskRecheck("IQC", 9L, List.of(101L));

        assertEquals("PENDING", insertedOrders.get(0).getStatus());
        assertEquals("PENDING", insertedOrders.get(0).getJudgment());
        assertEquals("IQC-NEW", insertedOrders.get(0).getIqcNo());

        QmsIqcItemDO resetItem = insertedItems.get(0);
        assertNull(resetItem.getMaxValue());
        assertNull(resetItem.getMinValue());
        assertNull(resetItem.getAverageValue());
        assertEquals("PENDING", resetItem.getItemResult());

        QmsIqcItemDO copiedItem = insertedItems.get(1);
        assertEquals(new BigDecimal("8.00"), copiedItem.getMaxValue());
        assertEquals(new BigDecimal("7.00"), copiedItem.getMinValue());
        assertEquals(new BigDecimal("7.50"), copiedItem.getAverageValue());
        assertEquals("OK", copiedItem.getItemResult());

        QmsIqcSampleDO resetSample = insertedSamples.get(0);
        assertNull(resetSample.getMeasuredValue());
        assertNull(resetSample.getDefectCode());
        assertEquals("PENDING", resetSample.getSampleResult());

        QmsIqcSampleDO copiedSample = insertedSamples.get(1);
        assertEquals(new BigDecimal("7.50"), copiedSample.getMeasuredValue());
        assertEquals("OK", copiedSample.getSampleResult());
        assertEquals(1, generated.items().size());
        assertEquals(101L, generated.items().get(0).sourceItemId());
    }

    @Test
    void shouldBuildTaskOwnedDefinitionFromSelectedStandardItems() {
        QmsQualityStandardDO standard = enabledStandard();
        QmsQualityStandardItemDO thickness = standardItem(
                101L, 20, "Thickness", "1.00-1.20", "mm", "Gauge", "Micrometer", 3);
        QmsQualityStandardItemDO appearance = standardItem(
                102L, 10, "Appearance", "No scratch", null, "Visual", "Light box", 1);
        when(standardMapper.selectById(10L)).thenReturn(standard);
        when(standardItemMapper.selectListByStandardId(10L)).thenReturn(List.of(thickness, appearance));

        QmsDispatchTaskWizardCreateReqVO request = new QmsDispatchTaskWizardCreateReqVO();
        request.setCheckType("FAI");
        request.setStandardId(10L);
        request.setSelectedItemIds(List.of(101L, 102L));

        QmsDispatchTaskInspectionService.TaskOwnedDefinition definition =
                service.buildTaskOwnedDefinition(request);

        assertSame(standard, definition.standard());
        assertEquals(2, definition.items().size());
        QmsDispatchTaskInspectionService.GeneratedItem first = definition.items().get(0);
        assertEquals(102L, first.standardItemId());
        assertEquals("Appearance", first.inspectionItem());
        assertEquals("Visual", first.inspectionMethod());
        assertEquals("Light box", first.testTool());
        assertEquals(1, first.sampleSize());
        assertNull(first.sourceItemId());
        assertNull(first.executionItemId());
    }

    @Test
    void shouldRejectDuplicateSelectedStandardItems() {
        QmsQualityStandardDO standard = enabledStandard();
        QmsQualityStandardItemDO item = standardItem(
                101L, 10, "Thickness", "1.00-1.20", "mm", "Gauge", "Micrometer", 3);
        when(standardMapper.selectById(10L)).thenReturn(standard);
        when(standardItemMapper.selectListByStandardId(10L)).thenReturn(List.of(item));

        QmsDispatchTaskWizardCreateReqVO request = new QmsDispatchTaskWizardCreateReqVO();
        request.setCheckType("FAI");
        request.setStandardId(10L);
        request.setSelectedItemIds(List.of(101L, 101L));

        assertThrows(ServiceException.class, () -> service.buildTaskOwnedDefinition(request));
    }

    @Test
    void shouldRejectStandardFromAnotherInspectionType() {
        QmsQualityStandardDO standard = enabledStandard();
        when(standardMapper.selectById(10L)).thenReturn(standard);

        QmsDispatchTaskWizardCreateReqVO request = new QmsDispatchTaskWizardCreateReqVO();
        request.setCheckType("OQC");
        request.setStandardId(10L);
        request.setSelectedItemIds(List.of(101L));

        assertThrows(ServiceException.class, () -> service.buildTaskOwnedDefinition(request));
    }

    @Test
    void shouldBuildFaiTaskItemsFromSelectedStandardPositions() {
        QmsQualityStandardDO standard = enabledStandard();
        QmsQualityStandardItemDO item = standardItem(
                101L, 10, "Thickness", "1.00-1.20", "mm", "Gauge", "Micrometer", 3);
        item.setTemplateParams("{\"positions\":[{\"code\":\"L5\",\"name\":\"L5\"},"
                + "{\"code\":\"R5\",\"name\":\"R5\"}]}");
        when(standardMapper.selectById(10L)).thenReturn(standard);
        when(standardItemMapper.selectListByStandardId(10L)).thenReturn(List.of(item));

        QmsDispatchTaskWizardCreateReqVO request = standardTaskRequest();
        request.setSelectedScopes(List.of(positionScope(101L, "L5")));

        QmsDispatchTaskInspectionService.TaskOwnedDefinition definition =
                service.buildTaskOwnedDefinition(request);

        assertEquals(1, definition.items().size());
        assertEquals("L5", definition.items().get(0).pieceNo());
        assertEquals(1, definition.items().get(0).sampleSize());
    }

    @Test
    void shouldBuildGlueBoardTaskItemsFromSelectedStandardPositions() {
        QmsQualityStandardDO standard = enabledStandard();
        standard.setApplyType("GLUE_BOARD_FAI");
        QmsQualityStandardItemDO item = standardItem(
                101L, 10, "厚度", "1.00-1.20", "mm", "测厚", "测厚仪", 3);
        item.setTemplateParams("{\"positions\":[{\"code\":\"前段\",\"name\":\"前段\"},"
                + "{\"code\":\"后段\",\"name\":\"后段\"}]}");
        when(standardMapper.selectById(10L)).thenReturn(standard);
        when(standardItemMapper.selectListByStandardId(10L)).thenReturn(List.of(item));

        QmsDispatchTaskWizardCreateReqVO request = standardTaskRequest();
        request.setCheckType("GLUE_BOARD_FAI");
        request.setSelectedScopes(List.of(positionScope(101L, "前段")));

        QmsDispatchTaskInspectionService.TaskOwnedDefinition definition =
                service.buildTaskOwnedDefinition(request);

        assertEquals(1, definition.items().size());
        assertEquals("前段", definition.items().get(0).pieceNo());
        assertEquals("厚度", definition.items().get(0).inspectionItem());
        assertEquals(1, definition.items().get(0).sampleSize());
    }

    @Test
    void shouldRejectUnknownFaiStandardPosition() {
        QmsQualityStandardDO standard = enabledStandard();
        QmsQualityStandardItemDO item = standardItem(
                101L, 10, "Thickness", "1.00-1.20", "mm", "Gauge", "Micrometer", 3);
        item.setTemplateParams("{\"positions\":[{\"code\":\"L5\",\"name\":\"L5\"}]}");
        when(standardMapper.selectById(10L)).thenReturn(standard);
        when(standardItemMapper.selectListByStandardId(10L)).thenReturn(List.of(item));

        QmsDispatchTaskWizardCreateReqVO request = standardTaskRequest();
        request.setSelectedScopes(List.of(positionScope(101L, "UNKNOWN")));

        assertThrows(ServiceException.class, () -> service.buildTaskOwnedDefinition(request));
    }

    @Test
    void shouldResetOnlySelectedFaiPosition() {
        QmsFaiSampleDO left = new QmsFaiSampleDO();
        left.setFaiItemId(101L);
        left.setSamplePosition("L5");
        QmsFaiSampleDO right = new QmsFaiSampleDO();
        right.setFaiItemId(101L);
        right.setSamplePosition("R5");
        Map<Long, Set<String>> selectedPositions = Map.of(101L, Set.of("L5"));

        Boolean resetLeft = ReflectionTestUtils.invokeMethod(service, "shouldResetFaiSample",
                left, Set.of(101L), selectedPositions);
        Boolean resetRight = ReflectionTestUtils.invokeMethod(service, "shouldResetFaiSample",
                right, Set.of(101L), selectedPositions);

        assertTrue(resetLeft);
        assertFalse(resetRight);
    }

    @Test
    void shouldBuildGlueBoardFaiItemPositionGroupTree() {
        QmsFaiOrderDO order = new QmsFaiOrderDO();
        order.setId(9L);
        order.setSourceModule("GLUE_BOARD_FAI");
        QmsFaiItemDO item = QmsFaiItemDO.builder()
                .id(101L)
                .faiId(9L)
                .standardItemId(201L)
                .inspectionItem("厚度")
                .standardDesc("1.00-1.20mm")
                .unit("mm")
                .sort(10)
                .build();
        QmsFaiSampleDO first = QmsFaiSampleDO.builder()
                .id(1001L)
                .faiId(9L)
                .faiItemId(101L)
                .samplePosition("前段")
                .sampleGroupNo(1)
                .sampleSeq(1)
                .measuredValue(new BigDecimal("1.10"))
                .resultValue(new BigDecimal("1.08"))
                .sampleResult("OK")
                .build();
        QmsFaiSampleDO second = QmsFaiSampleDO.builder()
                .id(1002L)
                .faiId(9L)
                .faiItemId(101L)
                .samplePosition("前段")
                .sampleGroupNo(2)
                .sampleSeq(2)
                .measuredValue(new BigDecimal("1.30"))
                .resultValue(new BigDecimal("1.25"))
                .sampleResult("NG")
                .build();
        when(faiOrderMapper.selectById(9L)).thenReturn(order);
        when(faiItemMapper.selectListByFaiId(9L)).thenReturn(List.of(item));
        when(faiSampleMapper.selectListByFaiId(9L)).thenReturn(List.of(first, second));

        var tree = service.getSourceItemTree("GLUE_BOARD_FAI", 9L);

        assertEquals(1, tree.size());
        assertEquals("ITEM", tree.get(0).getNodeType());
        assertTrue(tree.get(0).getSelectable());
        assertEquals(1, tree.get(0).getChildren().size());
        var position = tree.get(0).getChildren().get(0);
        assertEquals("POSITION", position.getNodeType());
        assertTrue(position.getSelectable());
        assertEquals("NG", position.getCurrentResult());
        assertEquals(2, position.getChildren().size());
        var group = position.getChildren().get(0);
        assertEquals("GROUP", group.getNodeType());
        assertFalse(group.getSelectable());
        assertEquals(new BigDecimal("1.10"), group.getMeasuredValue());
        assertEquals(new BigDecimal("1.08"), group.getResultValue());
        assertEquals("OK", group.getCurrentResult());
    }

    @Test
    void shouldGroupFqcItemsByInspectionItemAndStandard() {
        QmsFqcOrderDO order = new QmsFqcOrderDO();
        order.setId(9L);
        QmsFqcItemDO first = fqcItem(101L, 10, "Appearance", "No scratch");
        QmsFqcItemDO second = fqcItem(102L, 20, "Appearance", "No scratch");
        QmsFqcSampleDO okSample = fqcSample(1001L, 101L, "PIECE-001", "OK");
        QmsFqcSampleDO ngSample = fqcSample(1002L, 102L, "PIECE-002", "NG");
        when(fqcOrderMapper.selectById(9L)).thenReturn(order);
        when(fqcItemMapper.selectListByFqcId(9L)).thenReturn(List.of(first, second));
        when(fqcSampleMapper.selectListByFqcId(9L)).thenReturn(List.of(okSample, ngSample));

        var tree = service.getSourceItemTree("FQC", 9L);

        assertEquals(1, tree.size());
        assertEquals(List.of(101L, 102L), tree.get(0).getSourceItemIds());
        assertEquals(2, tree.get(0).getPieceCount());
        assertEquals(1, tree.get(0).getOkPieceCount());
        assertEquals(1, tree.get(0).getNgPieceCount());
        assertEquals(0, tree.get(0).getPendingPieceCount());
        assertEquals(2, tree.get(0).getChildren().size());
        assertFalse(tree.get(0).getChildren().get(0).getSelectable());
    }

    @Test
    void shouldUseShippingFqcAggregationTreeForOqc() {
        QmsFqcOrderDO order = new QmsFqcOrderDO();
        order.setId(9L);
        order.setSourceModule(QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC);
        QmsFqcItemDO first = fqcItem(101L, 10, "Appearance", "No scratch");
        QmsFqcItemDO second = fqcItem(102L, 20, "Appearance", "No scratch");
        QmsFqcSampleDO okSample = fqcSample(1001L, 101L, "PIECE-001", "OK");
        QmsFqcSampleDO ngSample = fqcSample(1002L, 102L, "PIECE-002", "NG");
        when(fqcOrderMapper.selectById(9L)).thenReturn(order);
        when(fqcItemMapper.selectListByFqcId(9L)).thenReturn(List.of(first, second));
        when(fqcSampleMapper.selectListByFqcId(9L)).thenReturn(List.of(okSample, ngSample));

        var tree = service.getSourceItemTree("OQC", 9L);

        assertEquals(1, tree.size());
        assertEquals(List.of(101L, 102L), tree.get(0).getSourceItemIds());
        assertEquals(2, tree.get(0).getPieceCount());
        assertEquals(1, tree.get(0).getOkPieceCount());
        assertEquals(1, tree.get(0).getNgPieceCount());
        assertEquals(2, tree.get(0).getChildren().size());
        assertFalse(tree.get(0).getChildren().get(0).getSelectable());
    }

    @Test
    void shouldUseOkAndNgTotalAsFqcSubmittedQuantity() {
        QmsFqcOrderDO order = new QmsFqcOrderDO();
        order.setOkQty(7);
        order.setNgQty(3);

        BigDecimal quantity = ReflectionTestUtils.invokeMethod(service, "fqcSubmittedQty", order);

        assertEquals(new BigDecimal("10"), quantity);
    }

    @Test
    void shouldAllowFaiRecheckQuantityWithinOriginalSubmittedQuantity() {
        QmsFaiOrderDO source = new QmsFaiOrderDO();
        source.setInspectionQty(new BigDecimal("10"));

        BigDecimal quantity = ReflectionTestUtils.invokeMethod(service, "resolveFaiRecheckQty",
                source, new BigDecimal("6"));

        assertEquals(new BigDecimal("6"), quantity);
    }

    @Test
    void shouldRejectFaiRecheckQuantityAboveOriginalSubmittedQuantity() {
        QmsFaiOrderDO source = new QmsFaiOrderDO();
        source.setInspectionQty(new BigDecimal("10"));

        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "resolveFaiRecheckQty", source, new BigDecimal("11")));
    }

    @Test
    void shouldAllowFqcRecheckQuantityWithinOriginalSubmittedQuantity() {
        QmsFqcOrderDO source = new QmsFqcOrderDO();
        source.setOkQty(8);
        source.setNgQty(6);

        Integer quantity = ReflectionTestUtils.invokeMethod(service, "resolveFqcRecheckQty",
                source, new BigDecimal("3"));

        assertEquals(3, quantity);
    }

    @Test
    void shouldRejectFqcRecheckQuantityAboveOriginalSubmittedQuantity() {
        QmsFqcOrderDO source = new QmsFqcOrderDO();
        source.setOkQty(8);
        source.setNgQty(6);

        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "resolveFqcRecheckQty", source, new BigDecimal("15")));
    }

    @Test
    void shouldUseFqcSampleQtyForLegacyTaskRecheckWhenResultCountsAreEmpty() {
        QmsFqcOrderDO source = new QmsFqcOrderDO();
        source.setOkQty(0);
        source.setNgQty(0);
        source.setSampleQty(5);

        Integer quantity = ReflectionTestUtils.invokeMethod(service, "resolveFqcRecheckQty", source, null);

        assertEquals(5, quantity);
    }

    private QmsDispatchTaskWizardCreateReqVO standardTaskRequest() {
        QmsDispatchTaskWizardCreateReqVO request = new QmsDispatchTaskWizardCreateReqVO();
        request.setCheckType("FAI");
        request.setStandardId(10L);
        request.setSelectedItemIds(List.of(101L));
        return request;
    }

    private QmsDispatchTaskItemSelectionReqVO positionScope(Long itemId, String position) {
        QmsDispatchTaskItemSelectionReqVO scope = new QmsDispatchTaskItemSelectionReqVO();
        scope.setItemId(itemId);
        scope.setScopeType("POSITION");
        scope.setPositions(List.of(position));
        return scope;
    }

    private QmsQualityStandardDO enabledStandard() {
        return QmsQualityStandardDO.builder()
                .id(10L)
                .status(1)
                .auditStatus(20)
                .applyType("FAI")
                .standardNo("STD-001")
                .standardName("Standard")
                .build();
    }

    private QmsQualityStandardItemDO standardItem(Long id, Integer sort, String inspectionItem,
                                                    String standardDesc, String unit,
                                                    String inspectionMethod, String testTool,
                                                    Integer sampleSize) {
        return QmsQualityStandardItemDO.builder()
                .id(id)
                .standardId(10L)
                .sort(sort)
                .inspectionItem(inspectionItem)
                .standardDesc(standardDesc)
                .unit(unit)
                .inspectionMethod(inspectionMethod)
                .testTool(testTool)
                .sampleSize(sampleSize)
                .itemType("QUANTITATIVE")
                .build();
    }

    private QmsFqcItemDO fqcItem(Long id, Integer sort, String inspectionItem, String standardDesc) {
        return QmsFqcItemDO.builder()
                .id(id)
                .fqcId(9L)
                .standardItemId(201L)
                .inspectionItem(inspectionItem)
                .standardDesc(standardDesc)
                .sort(sort)
                .build();
    }

    private QmsFqcSampleDO fqcSample(Long id, Long itemId, String pieceNo, String result) {
        return QmsFqcSampleDO.builder()
                .id(id)
                .fqcId(9L)
                .fqcItemId(itemId)
                .productionBatchNo(pieceNo)
                .sampleResult(result)
                .build();
    }
}
