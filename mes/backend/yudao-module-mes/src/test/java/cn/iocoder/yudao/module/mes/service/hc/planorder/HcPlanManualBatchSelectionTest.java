package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotinstance.HcLotInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HcPlanManualBatchSelectionTest {
    private final HcPlanOrderServiceImpl service = spy(new HcPlanOrderServiceImpl());
    private final HcPlanOrderMapper plans = mock(HcPlanOrderMapper.class);
    private final HcLotRuleMapper rules = mock(HcLotRuleMapper.class);
    private final HcLotRuleService ruleService = mock(HcLotRuleService.class);
    private final HcPlanOrderDO oldPlan = HcPlanOrderDO.builder().id(1L).planStatus("DRAFT")
            .productionBatchContextJson("{\"replanAfterWithdrawal\":true,\"retiredBatchNos\":[\"W26J171A\"]}").build();
    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "hcPlanOrderMapper", plans);
        ReflectionTestUtils.setField(service, "hcLotRuleMapper", rules);
        ReflectionTestUtils.setField(service, "hcLotRuleService", ruleService);
        ReflectionTestUtils.setField(service, "hcLotInstanceMapper", mock(HcLotInstanceMapper.class));
        ReflectionTestUtils.setField(service, "hcProcessReportMapper", mock(HcProcessReportMapper.class));
        HcPlanOrderBatchPreviewRespVO preview = new HcPlanOrderBatchPreviewRespVO();
        preview.setRuleId(10L); preview.setBatchNo("W26J172A");
        doReturn(preview).when(service).previewRootBatchNo(any());
        when(rules.selectById(10L)).thenReturn(HcLotRuleDO.builder().id(10L).allowManualOverride(false).build());
    }
    private HcPlanOrderSaveReqVO save(String batchNo) {
        HcPlanOrderSaveReqVO req = new HcPlanOrderSaveReqVO();
        req.setId(1L); req.setBatchNo(batchNo);
        HcPlanOrderOperationReqVO op = new HcPlanOrderOperationReqVO(); op.setOpCode("FORMULA");
        req.setOperations(List.of(op));
        ReflectionTestUtils.invokeMethod(service, "normalizeAndValidateManualRootBatchNo", req, oldPlan);
        return req;
    }
    @Test void manualSelectionDoesNotRequireGlobalOverrideFlag() {
        assertEquals("W26J199A", save("w26j199a").getBatchNo());
        verify(ruleService).parseLotNo(any());
    }
    @Test void withdrawnNumberCanBeReused() {
        assertEquals("W26J171A", save("W26J171A").getBatchNo());
    }
    @Test void explicitSelectionEqualToPreviewMustNotBecomeAutomatic() {
        assertEquals("W26J172A", save("W26J172A").getBatchNo());
    }
    @Test void savedSelectionSurvivesAnotherSave() {
        oldPlan.setBatchNo(save("W26J171A").getBatchNo());
        assertEquals("W26J171A", save(oldPlan.getBatchNo()).getBatchNo());
    }
    @Test void currentOccupancyStillBlocksManualSelection() {
        when(plans.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThrows(RuntimeException.class, () -> save("W26J171A"));
    }
    @Test void invalidRuleFormatStillBlocksManualSelection() {
        when(ruleService.parseLotNo(any())).thenThrow(new IllegalArgumentException("格式无效"));
        assertThrows(RuntimeException.class, () -> save("BAD"));
    }
}
