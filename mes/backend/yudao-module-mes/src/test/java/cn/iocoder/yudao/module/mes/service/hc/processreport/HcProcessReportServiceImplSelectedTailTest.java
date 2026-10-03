package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplSelectedTailTest {
    @Spy @InjectMocks HcProcessReportServiceImpl service;
    @Mock HcPlanOrderMapper hcPlanOrderMapper;
    @Mock HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Mock HcAdhesive2ReportMapper hcAdhesive2ReportMapper;
    @Mock HcAdhesive2TailSelectionMapper hcAdhesive2TailSelectionMapper;

    @BeforeEach void setup() {
        HcPlanOrderDO plan = HcPlanOrderDO.builder().id(111L).tenantId(1L).build();
        HcPlanOrderOperationDO op = HcPlanOrderOperationDO.builder().id(894L).planId(111L).opCode("OP-ADHESIVE2").build();
        when(hcPlanOrderOperationMapper.selectByIdForUpdate(894L)).thenReturn(op);
        when(hcPlanOrderMapper.selectById(111L)).thenReturn(plan);
        when(hcPlanOrderOperationMapper.selectById(894L)).thenReturn(op);
        HcAdhesiveSourceRespVO a = HcAdhesiveSourceRespVO.builder().build(); a.setProductionBatchNo("W26H163AP013A");
        HcAdhesiveSourceRespVO b = HcAdhesiveSourceRespVO.builder().build(); b.setProductionBatchNo("W26H163AP014A");
        doReturn(List.of(a,b)).when(service).getAdhesive2SourceList(111L,894L,null);
    }
    private HcAdhesive2TailSelectedAssignReqVO req(String source) {
        HcAdhesive2TailSelectedAssignReqVO req = new HcAdhesive2TailSelectedAssignReqVO();
        req.setPlanId(111L); req.setPlanOperationId(894L);
        HcAdhesive2TailSelectedAssignReqVO.Item item = new HcAdhesive2TailSelectedAssignReqVO.Item();
        item.setSourceProductionBatchNo(source); item.setActualSizeRule("740mm"); req.setItems(List.of(item)); return req;
    }
    private HcAdhesive2ReportDO report(long id, String source, String status) {
        HcAdhesive2ReportDO report = new HcAdhesive2ReportDO(); report.setId(id);
        report.setSourceProductionBatchNo(source); report.setProductionBatchNo(source); report.setReportStatus(status); return report;
    }
    @Test void onlySelectedSourceGetsPresetWithoutCreatingReport() {
        assertEquals(1, service.assignAdhesive2TailForSelectedSources(req("W26H163AP013A")));
        verify(hcAdhesive2TailSelectionMapper).insert(argThat((HcAdhesive2TailSelectionDO value) ->
                "W26H163AP013A".equals(value.getSourceProductionBatchNo()) && "B".equals(value.getActualSizeSuffix())));
        verify(hcAdhesive2ReportMapper, never()).updateById(any(HcAdhesive2ReportDO.class));
        verify(hcAdhesive2ReportMapper, never()).insert(any(HcAdhesive2ReportDO.class));
    }
    @Test void selectedDraftAndPresetChangeTogetherWhileOtherConfirmedPieceStays() {
        when(hcAdhesive2ReportMapper.selectListByPlanOperationId(894L)).thenReturn(List.of(
                report(1L,"W26H163AP013A","DRAFT"), report(2L,"W26H163AP014A","CONFIRMED")));
        HcAdhesive2TailSelectionDO preset = HcAdhesive2TailSelectionDO.builder().id(7L).sourceProductionBatchNo("W26H163AP013A").build();
        when(hcAdhesive2TailSelectionMapper.selectListByPlanOperationId(894L)).thenReturn(List.of(preset));
        assertEquals(1,service.assignAdhesive2TailForSelectedSources(req("W26H163AP013A")));
        verify(hcAdhesive2ReportMapper).updateById(argThat((HcAdhesive2ReportDO value) ->
                value.getId()==1L && "W26H163AP013B".equals(value.getProductionBatchNo())));
        verify(hcAdhesive2TailSelectionMapper).updateById(argThat((HcAdhesive2TailSelectionDO value) -> value.getId()==7L && "740mm".equals(value.getActualSizeRule())));
    }
    @Test void rejectsUnknownSourceBeforeWrites() {
        assertThrows(RuntimeException.class, () -> service.assignAdhesive2TailForSelectedSources(req("OTHER")));
        verifyNoInteractions(hcAdhesive2TailSelectionMapper);
    }
    @Test void rejectsDuplicateInput() {
        var req=req("W26H163AP013A"); req.setItems(List.of(req.getItems().get(0),req.getItems().get(0)));
        assertThrows(RuntimeException.class, () -> service.assignAdhesive2TailForSelectedSources(req));
        verifyNoInteractions(hcAdhesive2TailSelectionMapper);
    }
    @Test void rejectsConfirmedBeforePresetWrites() {
        when(hcAdhesive2ReportMapper.selectListByPlanOperationId(894L)).thenReturn(List.of(report(1L,"W26H163AP013A","CONFIRMED")));
        assertThrows(RuntimeException.class, () -> service.assignAdhesive2TailForSelectedSources(req("W26H163AP013A")));
        verifyNoInteractions(hcAdhesive2TailSelectionMapper);
    }
    @Test void rejectsOccupiedTargetEvenWhenSourceHasNoReport() {
        when(hcAdhesive2ReportMapper.selectByProductionBatchNo("W26H163AP013B")).thenReturn(report(9L,"OTHER","DRAFT"));
        assertThrows(RuntimeException.class, () -> service.assignAdhesive2TailForSelectedSources(req("W26H163AP013A")));
        verifyNoInteractions(hcAdhesive2TailSelectionMapper);
    }
    @Test void rejectsStockedDraft() {
        var report=report(1L,"W26H163AP013A","DRAFT"); report.setOutputStockId(9L);
        when(hcAdhesive2ReportMapper.selectListByPlanOperationId(894L)).thenReturn(List.of(report));
        assertThrows(RuntimeException.class, () -> service.assignAdhesive2TailForSelectedSources(req("W26H163AP013A")));
        verifyNoInteractions(hcAdhesive2TailSelectionMapper);
    }
    @Test void rejectsPrintedDraftBeforeWrites() {
        var report=report(1L,"W26H163AP013A","DRAFT"); report.setExtraJson("{\"printCount\":1}");
        when(hcAdhesive2ReportMapper.selectListByPlanOperationId(894L)).thenReturn(List.of(report));
        assertThrows(RuntimeException.class, () -> service.assignAdhesive2TailForSelectedSources(req("W26H163AP013A")));
        verifyNoInteractions(hcAdhesive2TailSelectionMapper);
    }
    @Test void validatesAllSelectedRowsBeforeAnyWrite() {
        var req=req("W26H163AP013A");
        var second=req("W26H163AP014A").getItems().get(0);
        req.setItems(List.of(req.getItems().get(0),second));
        when(hcAdhesive2ReportMapper.selectListByPlanOperationId(894L)).thenReturn(List.of(report(2L,"W26H163AP014A","CONFIRMED")));
        assertThrows(RuntimeException.class, () -> service.assignAdhesive2TailForSelectedSources(req));
        verifyNoInteractions(hcAdhesive2TailSelectionMapper);
        verify(hcAdhesive2ReportMapper, never()).updateById(any(HcAdhesive2ReportDO.class));
    }
}
