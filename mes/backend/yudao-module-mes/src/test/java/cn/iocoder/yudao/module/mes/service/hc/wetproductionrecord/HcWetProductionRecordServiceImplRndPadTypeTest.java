package cn.iocoder.yudao.module.mes.service.hc.wetproductionrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo.HcWetProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.guideclothrecord.HcGuideClothRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcEquipmentConsumableEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.guideclothrecord.HcGuideClothRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcEquipmentConsumableEventMapper;
import cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcProductionRecordPadTypeResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcWetProductionRecordServiceImplRndPadTypeTest {

    @InjectMocks
    private HcWetProductionRecordServiceImpl service;

    @Mock
    private HcProcessReportMapper processReportMapper;

    @Mock
    private HcEquipmentConsumableEventMapper equipmentConsumableEventMapper;

    @Mock
    private HcGuideClothRecordMapper guideClothRecordMapper;

    @Mock
    private HcProductionRecordRevisionService productionRecordRevisionService;

    @Mock
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Test
    void shouldProjectRndPadTypeFromGuideClothLineInsteadOfResearchModel() {
        HcEquipmentConsumableEventDO blackLineEvent = rndEvent(
                101L, 21L, "RND-BLACK", "WHITE-MODEL", "BLACK-RND-BATCH");
        HcEquipmentConsumableEventDO whiteLineEvent = rndEvent(
                102L, 22L, "RND-WHITE", "BLACK-MODEL", "WHITE-RND-BATCH");
        when(processReportMapper.selectList(any())).thenReturn(List.of());
        when(equipmentConsumableEventMapper.selectList(any()))
                .thenReturn(List.of(blackLineEvent, whiteLineEvent));
        when(guideClothRecordMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                HcGuideClothRecordDO.builder().id(21L).lineCode("BLACK").build(),
                HcGuideClothRecordDO.builder().id(22L).lineCode("WHITE").build()));
        when(padTypeResolver.resolveByGuideClothLineCode("BLACK"))
                .thenReturn(HcProductionRecordPadTypeResolver.BLACK_PAD);
        when(padTypeResolver.resolveByGuideClothLineCode("WHITE"))
                .thenReturn(HcProductionRecordPadTypeResolver.WHITE_PAD);
        when(padTypeResolver.resolveByModelCodes(anyCollection())).thenReturn(Map.of(
                "WHITE-MODEL", HcProductionRecordPadTypeResolver.WHITE_PAD,
                "BLACK-MODEL", HcProductionRecordPadTypeResolver.BLACK_PAD));
        when(padTypeResolver.matchesFilter(nullable(String.class), nullable(String.class)))
                .thenAnswer(invocation -> {
                    String filter = invocation.getArgument(0);
                    String actual = invocation.getArgument(1);
                    return filter == null || filter.equals(actual);
                });
        when(productionRecordRevisionService.applyRevisions(eq("WET"), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        HcWetProductionRecordPageReqVO reqVO = new HcWetProductionRecordPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        PageResult<HcWetProductionRecordRespVO> page = service.getReportPage(reqVO);

        Map<String, HcWetProductionRecordRespVO> rowsByBatch = page.getList().stream()
                .collect(Collectors.toMap(HcWetProductionRecordRespVO::getBatchNo, Function.identity()));
        assertEquals(2L, page.getTotal());
        assertEquals(HcProductionRecordPadTypeResolver.BLACK_PAD,
                rowsByBatch.get("BLACK-RND-BATCH").getPadType());
        assertEquals(HcProductionRecordPadTypeResolver.WHITE_PAD,
                rowsByBatch.get("WHITE-RND-BATCH").getPadType());

        reqVO.setPadType(HcProductionRecordPadTypeResolver.BLACK_PAD);
        PageResult<HcWetProductionRecordRespVO> blackPage = service.getReportPage(reqVO);
        assertEquals(1L, blackPage.getTotal());
        assertEquals("BLACK-RND-BATCH", blackPage.getList().get(0).getBatchNo());
    }

    private HcEquipmentConsumableEventDO rndEvent(Long id, Long guideClothRecordId, String groupNo,
                                                   String modelCode, String productBatchNo) {
        return HcEquipmentConsumableEventDO.builder()
                .id(id)
                .guideClothRecordId(guideClothRecordId)
                .processCode("WET")
                .consumableType("GUIDE_CLOTH")
                .eventType("RND_MANUAL_USE")
                .recordSource("RND_MANUAL")
                .recordGroupNo(groupNo)
                .productModelCode(modelCode)
                .productMaterialCode("RND-MATERIAL")
                .productBatchNo(productBatchNo)
                .petModel("01.05.00132")
                .petBatchNo("PET-RND")
                .beforeBatchNo("GUIDE-RND")
                .afterBatchNo("GUIDE-RND")
                .afterUseCount(2)
                .wetInputKg(BigDecimal.ONE)
                .wetOutputMeter(BigDecimal.TEN)
                .operatorName("研发员")
                .eventTime(LocalDateTime.of(2026, 9, 1, 16, 0))
                .remark("研发登记")
                .build();
    }
}
