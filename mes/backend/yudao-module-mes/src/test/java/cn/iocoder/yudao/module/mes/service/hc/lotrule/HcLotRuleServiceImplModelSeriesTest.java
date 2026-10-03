package cn.iocoder.yudao.module.mes.service.hc.lotrule;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcLotRuleServiceImplModelSeriesTest {

    @InjectMocks
    private HcLotRuleServiceImpl service;

    @Mock
    private HcLotRuleMapper hcLotRuleMapper;

    @Test
    void shouldSelectW26AndW33PrefixRulesBeforeWhitePadFallback() {
        HcLotRuleDO fallback = rule(992002L, "LOT-CMP-WHITE-MASS-V2", "ALL", null, 100);
        HcLotRuleDO w26 = rule(992004L, "LOT-CMP-WHITE-W26-MASS-V1", "PREFIX", "W26", 200);
        HcLotRuleDO w33 = rule(992005L, "LOT-CMP-WHITE-W33-MASS-V1", "PREFIX", "W33", 200);
        when(hcLotRuleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(fallback, w26, w33));

        assertSame(w26, service.matchEnabledRule(context("W26P0100")));
        assertSame(w33, service.matchEnabledRule(context("W33P")));
        assertSame(w33, service.matchEnabledRule(context("W33P0300")));
        assertSame(fallback, service.matchEnabledRule(context("W34P")));
    }

    private HcLotRuleDO rule(Long id, String ruleCode, String matchMode, String matchValue, int priority) {
        return HcLotRuleDO.builder()
                .id(id)
                .ruleCode(ruleCode)
                .bizType("FG_LOT")
                .productCategoryCode("WHITE_PAD")
                .prodType("MASS")
                .generationTrigger("FORMULA_START")
                .generationScope("PLAN_ROOT")
                .modelMatchMode(matchMode)
                .modelMatchValue(matchValue)
                .priority(priority)
                .status(1)
                .build();
    }

    private HcLotRuleMatchContext context(String modelCode) {
        return HcLotRuleMatchContext.builder()
                .bizType("FG_LOT")
                .productCategoryCode("WHITE_PAD")
                .prodType("MASS")
                .generationTrigger("FORMULA_START")
                .generationScope("PLAN_ROOT")
                .modelCode(modelCode)
                .build();
    }
}
