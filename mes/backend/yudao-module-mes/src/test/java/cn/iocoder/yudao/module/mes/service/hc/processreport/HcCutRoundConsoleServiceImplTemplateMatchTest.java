package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveCheckItemRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcCutRoundConsoleServiceImplTemplateMatchTest {

    @InjectMocks
    private HcCutRoundConsoleServiceImpl service;

    @Mock
    private HcProductModelMapper hcProductModelMapper;
    @Mock
    private HcStationFormMapper hcStationFormMapper;
    @Mock
    private HcStationFormItemMapper hcStationFormItemMapper;

    @Test
    void shouldResolveBlackPadTemplateByProductModelCategory() {
        when(hcProductModelMapper.selectByModelCode("HCSP10")).thenReturn(HcProductModelDO.builder()
                .modelCode("HCSP10")
                .categoryCode("BLACK_PAD")
                .build());
        when(hcStationFormMapper.selectEnabledByProcess("CUT_ROUND")).thenReturn(List.of(
                HcStationFormDO.builder()
                        .id(94L)
                        .formCode("CUT_ROUND_PRODUCTION_CHECK_BLACK")
                        .formName("CMP黑垫裁切点检表")
                        .sortNo(430)
                        .schemaJson("{\"processFormType\":\"PRODUCTION_CHECK\","
                                + "\"materialType\":\"BLACK_PAD\",\"modelCodePrefixes\":[\"HCR\"]}")
                        .build(),
                HcStationFormDO.builder()
                        .id(95L)
                        .formCode("CUT_ROUND_PRODUCTION_CHECK_WHITE")
                        .formName("CMP软垫裁切点检表")
                        .sortNo(440)
                        .schemaJson("{\"processFormType\":\"PRODUCTION_CHECK\","
                                + "\"materialType\":\"WHITE_PAD\",\"modelCodePrefixes\":[\"W\"]}")
                        .build()));
        when(hcStationFormItemMapper.selectByFormId(94L)).thenReturn(List.of(
                HcStationFormItemDO.builder()
                        .id(1L)
                        .formId(94L)
                        .itemSeq(1)
                        .itemCategory("环境")
                        .itemName("温度")
                        .standardText("23±4℃")
                        .defaultResult("OK")
                        .build()));

        List<HcAdhesiveCheckItemRespVO> result = service.getCheckTemplate(" hcsp10 ");

        assertEquals(1, result.size());
        assertEquals("温度", result.get(0).getItemName());
        verify(hcProductModelMapper).selectByModelCode("HCSP10");
        verify(hcStationFormItemMapper).selectByFormId(94L);
    }
}
