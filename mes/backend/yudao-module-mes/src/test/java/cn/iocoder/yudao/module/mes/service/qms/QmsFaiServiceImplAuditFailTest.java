package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiAuditReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemAuditHistoryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemGroupAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiReturnRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSampleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiAbnormalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemAuditHistoryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemGroupAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiReturnRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSampleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetCellValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetStatResultMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QmsFaiServiceImplAuditFailTest {

    @InjectMocks
    private QmsFaiServiceImpl service;

    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Mock
    private QmsFaiItemMapper qmsFaiItemMapper;
    @Mock
    private QmsFaiItemAuditHistoryMapper qmsFaiItemAuditHistoryMapper;
    @Mock
    private QmsFaiItemGroupAuditMapper qmsFaiItemGroupAuditMapper;
    @Mock
    private QmsFaiSampleMapper qmsFaiSampleMapper;
    @Mock
    private QmsFaiAbnormalMapper qmsFaiAbnormalMapper;
    @Mock
    private QmsFaiReturnRecordMapper qmsFaiReturnRecordMapper;
    @Mock
    private QmsFaiSheetCellValueMapper qmsFaiSheetCellValueMapper;
    @Mock
    private QmsFaiSheetStatResultMapper qmsFaiSheetStatResultMapper;
    @Mock
    private QmsAbnormalLockService qmsAbnormalLockService;
    @Mock
    private QmsSampleAbnormalLockService qmsSampleAbnormalLockService;
    @Mock
    private QmsFaiSampleResultWritebackService qmsFaiSampleResultWritebackService;
    @Mock
    private cn.iocoder.yudao.module.mes.service.hc.processreport.HcFinishedPackagingService coaFreezePackagingService;
    @Mock
    private QmsFaiOaNotifyService qmsFaiOaNotifyService;

    @Test
    void shouldCompleteAndReleasePressSlotWithSinglePassResult() {
        Long faiId = 551L;
        QmsFaiOrderDO order = buildPressSlotWaitingAuditOrder(faiId);
        when(qmsFaiOrderMapper.selectById(faiId)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(faiId)).thenReturn(List.of(
                QmsFaiItemDO.builder().id(5101L).faiId(faiId).qaResult("OK").build()));

        QmsFaiAuditReqVO reqVO = buildPressSlotAuditReq(faiId, "PASS");
        service.auditProgramEntry(reqVO);

        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).updateById(captor.capture());
        QmsFaiOrderDO update = captor.getValue();
        assertEquals("COMPLETED", update.getStatus());
        assertEquals("OK", update.getJudgment());
        assertEquals("RELEASED", update.getReleaseResult());
        verify(qmsAbnormalLockService, never()).syncFromFai(faiId);
    }

    @Test
    void shouldLockPressSlotWithSingleFailResult() {
        Long faiId = 553L;
        QmsFaiOrderDO order = buildPressSlotWaitingAuditOrder(faiId);
        List<QmsFaiItemDO> items = List.of(
                QmsFaiItemDO.builder().id(5301L).faiId(faiId).qaResult("OK").build());
        when(qmsFaiOrderMapper.selectById(faiId)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(faiId)).thenReturn(items);
        when(qmsFaiSampleMapper.selectListByFaiId(faiId)).thenReturn(Collections.emptyList());
        when(qmsFaiAbnormalMapper.selectListByFaiId(faiId)).thenReturn(Collections.emptyList());

        QmsFaiAuditReqVO reqVO = buildPressSlotAuditReq(faiId, "FAIL");
        service.auditProgramEntry(reqVO);

        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).updateById(captor.capture());
        QmsFaiOrderDO update = captor.getValue();
        assertEquals("REJECTED", update.getStatus());
        assertEquals("NG", update.getJudgment());
        assertEquals("LOCKED", update.getReleaseResult());
        verify(qmsAbnormalLockService).syncFromFai(faiId);
    }

    @Test
    void shouldReturnPressSlotWithoutFinalJudgment() {
        Long faiId = 555L;
        QmsFaiOrderDO order = buildPressSlotWaitingAuditOrder(faiId);
        when(qmsFaiOrderMapper.selectById(faiId)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(faiId)).thenReturn(List.of(
                QmsFaiItemDO.builder().id(5501L).faiId(faiId).qaResult("OK").build()));

        QmsFaiAuditReqVO reqVO = new QmsFaiAuditReqVO();
        reqVO.setId(faiId);
        reqVO.setAuditResult("REJECT");
        reqVO.setRejectReason("检测记录需要补充");
        service.auditProgramEntry(reqVO);

        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).updateById(captor.capture());
        QmsFaiOrderDO update = captor.getValue();
        assertEquals("INSPECTING", update.getStatus());
        assertEquals("PENDING", update.getJudgment());
        assertEquals("LOCKED", update.getReleaseResult());
        verify(qmsFaiReturnRecordMapper).insert(any(QmsFaiReturnRecordDO.class));
        verify(qmsAbnormalLockService, never()).syncFromFai(faiId);
    }

    @Test
    void shouldKeepLegacySingleConclusionForPressSlotProcessCheck() {
        Long faiId = 557L;
        QmsFaiOrderDO order = buildPressSlotWaitingAuditOrder(faiId);
        order.setSourceReportNo("PS-TASK-PROCESS-CHECK-001");
        when(qmsFaiOrderMapper.selectById(faiId)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(faiId)).thenReturn(List.of(
                QmsFaiItemDO.builder().id(5701L).faiId(faiId).qaResult("OK").build()));

        QmsFaiAuditReqVO reqVO = new QmsFaiAuditReqVO();
        reqVO.setId(faiId);
        reqVO.setAuditResult("PASS");
        service.auditProgramEntry(reqVO);

        ArgumentCaptor<QmsFaiOrderDO> captor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).updateById(captor.capture());
        assertEquals("COMPLETED", captor.getValue().getStatus());
        assertEquals("OK", captor.getValue().getJudgment());
    }

    private QmsFaiOrderDO buildPressSlotWaitingAuditOrder(Long faiId) {
        return QmsFaiOrderDO.builder()
                .id(faiId)
                .faiNo("FAI-PRESS-SLOT-" + faiId)
                .sourceModule("PRESS_SLOT_REPORT")
                .sourceReportNo("PS-TASK-" + faiId)
                .processCategory("PRESS_SLOT")
                .status("WAITING_QA")
                .judgment("PENDING")
                .returnCount(0)
                .build();
    }

    private QmsFaiAuditReqVO buildPressSlotAuditReq(Long faiId, String auditResult) {
        QmsFaiAuditReqVO reqVO = new QmsFaiAuditReqVO();
        reqVO.setId(faiId);
        reqVO.setAuditResult(auditResult);
        return reqVO;
    }

    @Test
    void shouldCreateOrderLevelAbnormalWhenWholeOrderAuditFails() {
        Long faiId = 541L;
        QmsFaiOrderDO order = QmsFaiOrderDO.builder()
                .id(faiId)
                .faiNo("FAI-20260722-001")
                .status("WAITING_QA")
                .judgment("PENDING")
                .returnCount(0)
                .build();
        List<QmsFaiItemDO> items = List.of(
                QmsFaiItemDO.builder().id(1001L).faiId(faiId).qaResult("OK").build(),
                QmsFaiItemDO.builder().id(1002L).faiId(faiId).qaResult("OK").build());
        when(qmsFaiOrderMapper.selectById(faiId)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(faiId)).thenReturn(items);
        when(qmsFaiSampleMapper.selectListByFaiId(faiId)).thenReturn(Collections.emptyList());
        when(qmsFaiAbnormalMapper.selectListByFaiId(faiId)).thenReturn(Collections.emptyList());

        QmsFaiAuditReqVO reqVO = new QmsFaiAuditReqVO();
        reqVO.setId(faiId);
        reqVO.setAuditResult("FAIL");
        reqVO.setRejectReason("整单外观判定不合格");

        service.auditProgramEntry(reqVO);

        ArgumentCaptor<QmsFaiOrderDO> orderCaptor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).updateById(orderCaptor.capture());
        QmsFaiOrderDO update = orderCaptor.getValue();
        assertEquals("REJECTED", update.getStatus());
        assertEquals("NG", update.getJudgment());
        assertEquals(1, update.getAbnormalItemCount());
        assertEquals("整单外观判定不合格", update.getLastReturnReason());

        ArgumentCaptor<QmsFaiAbnormalDO> abnormalCaptor = ArgumentCaptor.forClass(QmsFaiAbnormalDO.class);
        verify(qmsFaiAbnormalMapper).insert(abnormalCaptor.capture());
        QmsFaiAbnormalDO abnormal = abnormalCaptor.getValue();
        assertEquals(faiId, abnormal.getFaiId());
        assertEquals("FAI-20260722-001", abnormal.getFaiNo());
        assertNull(abnormal.getFaiItemId());
        assertEquals("QA", abnormal.getAbnormalRole());
        assertEquals("PENDING", abnormal.getProcessStatus());
        assertEquals("REWORK", abnormal.getActionRequired());
        assertTrue(abnormal.getAbnormalDesc().contains("整单外观判定不合格"));

        verify(qmsFaiItemMapper, never()).updateById(any(QmsFaiItemDO.class));
        verify(qmsAbnormalLockService).syncFromFai(faiId);
        verify(qmsSampleAbnormalLockService).syncFromFai(faiId);
    }

    @Test
    void shouldKeepWholeOrderRejectFlowWhenItemIsRejectedForRecheck() {
        Long faiId = 542L;
        QmsFaiOrderDO order = QmsFaiOrderDO.builder()
                .id(faiId)
                .faiNo("FAI-20260722-002")
                .status("WAITING_QA")
                .judgment("PENDING")
                .returnCount(0)
                .currentStepCode("CONFIRM")
                .build();
        List<QmsFaiItemDO> items = List.of(
                QmsFaiItemDO.builder().id(2001L).faiId(faiId).inspectionItem("厚度").qaResult("OK").build(),
                QmsFaiItemDO.builder().id(2002L).faiId(faiId).inspectionItem("外观").qaResult("NG").build());
        List<QmsFaiSampleDO> qaSamples = List.of(
                QmsFaiSampleDO.builder()
                        .id(3001L)
                        .faiId(faiId)
                        .faiItemId(2001L)
                        .sampleRole("QA")
                        .sampleSeq(1)
                        .samplePosition("L5")
                        .sampleResult("OK")
                        .build(),
                QmsFaiSampleDO.builder()
                        .id(3002L)
                        .faiId(faiId)
                        .faiItemId(2002L)
                        .sampleRole("QA")
                        .sampleSeq(1)
                        .samplePosition("L3")
                        .sampleResult("NG")
                        .build());
        when(qmsFaiOrderMapper.selectById(faiId)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(faiId)).thenReturn(items);
        when(qmsFaiSampleMapper.selectListByFaiId(faiId)).thenReturn(qaSamples);
        when(qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(eq(faiId), anyCollection(), eq("QA")))
                .thenReturn(qaSamples);
        when(qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(
                eq(faiId), argThat(ids -> ids != null && ids.size() == 1 && ids.contains(2002L)), eq("QA")))
                .thenReturn(Collections.emptyList());
        when(qmsFaiSheetCellValueMapper.selectListByFaiIdAndItemIds(eq(faiId), anyList()))
                .thenReturn(Collections.emptyList());
        when(qmsFaiSheetStatResultMapper.selectListByFaiIdAndItemIds(eq(faiId), anyList()))
                .thenReturn(Collections.emptyList());
        when(qmsFaiAbnormalMapper.selectListByFaiId(faiId)).thenReturn(Collections.emptyList());

        QmsFaiAuditReqVO reqVO = new QmsFaiAuditReqVO();
        reqVO.setId(faiId);
        reqVO.setAuditResult("REJECT");
        reqVO.setRejectReason("外观复检");
        QmsFaiAuditReqVO.FaiGroupAudit confirmGroup = new QmsFaiAuditReqVO.FaiGroupAudit();
        confirmGroup.setItemId(2001L);
        confirmGroup.setGroupKey("L5");
        confirmGroup.setSamplePosition("L5");
        confirmGroup.setAuditResult("CONFIRM");
        QmsFaiAuditReqVO.FaiGroupAudit rejectGroup = new QmsFaiAuditReqVO.FaiGroupAudit();
        rejectGroup.setItemId(2002L);
        rejectGroup.setGroupKey("L3");
        rejectGroup.setSamplePosition("L3");
        rejectGroup.setAuditResult("REJECT_RECHECK");
        rejectGroup.setAuditRemark("外观划伤，重检");
        reqVO.setGroups(List.of(confirmGroup, rejectGroup));

        service.auditProgramEntry(reqVO);

        ArgumentCaptor<QmsFaiOrderDO> orderCaptor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).updateById(orderCaptor.capture());
        QmsFaiOrderDO update = orderCaptor.getValue();
        assertEquals("INSPECTING", update.getStatus());
        assertEquals("PENDING", update.getJudgment());
        assertEquals("LOCKED", update.getReleaseResult());
        assertEquals(Boolean.FALSE, update.getSheetLocked());
        assertEquals("外观复检", update.getLastReturnReason());

        ArgumentCaptor<QmsFaiItemAuditHistoryDO> historyCaptor =
                ArgumentCaptor.forClass(QmsFaiItemAuditHistoryDO.class);
        verify(qmsFaiItemAuditHistoryMapper).insert(historyCaptor.capture());
        QmsFaiItemAuditHistoryDO history = historyCaptor.getValue();
        assertEquals(faiId, history.getFaiId());
        assertEquals(2002L, history.getFaiItemId());
        assertEquals("l3", history.getGroupKey());
        assertEquals("L3", history.getSamplePosition());
        assertEquals("REJECT_RECHECK", history.getAuditResult());
        assertTrue(history.getSnapshotJson().contains("\"inspectionItem\":\"外观\""));

        ArgumentCaptor<QmsFaiItemGroupAuditDO> groupAuditCaptor =
                ArgumentCaptor.forClass(QmsFaiItemGroupAuditDO.class);
        verify(qmsFaiItemGroupAuditMapper, times(2)).insert(groupAuditCaptor.capture());
        List<QmsFaiItemGroupAuditDO> groupAudits = groupAuditCaptor.getAllValues();
        QmsFaiItemGroupAuditDO rejectedAudit = groupAudits.stream()
                .filter(audit -> Long.valueOf(2002L).equals(audit.getFaiItemId()))
                .findFirst()
                .orElseThrow();
        assertEquals("l3", rejectedAudit.getGroupKey());
        assertEquals("L3", rejectedAudit.getSamplePosition());
        assertEquals("REJECT_RECHECK", rejectedAudit.getAuditResult());
        assertEquals("WAIT_RECHECK", rejectedAudit.getItemRecheckStatus());
        assertTrue(Boolean.TRUE.equals(rejectedAudit.getRecheckItemFlag()));

        verify(qmsFaiSampleMapper).deleteBatchIds(eq(List.of(3002L)));
        verify(qmsFaiSheetCellValueMapper)
                .deleteByFaiIdAndItemIdAndDisplayLabels(eq(faiId), eq(2002L), eq(List.of("L3")));
        verify(qmsFaiReturnRecordMapper).insert(any(QmsFaiReturnRecordDO.class));
        verify(qmsAbnormalLockService, never()).syncFromFai(faiId);
        verify(qmsSampleAbnormalLockService).syncFromFai(faiId);
    }

    @Test
    void shouldAcceptSubmittedGroupAuditWhenQaSamplesAreMissing() {
        Long faiId = 543L;
        QmsFaiOrderDO order = QmsFaiOrderDO.builder()
                .id(faiId)
                .faiNo("FAI-20260722-003")
                .status("WAITING_QA")
                .judgment("PENDING")
                .returnCount(0)
                .build();
        List<QmsFaiItemDO> items = List.of(
                QmsFaiItemDO.builder().id(3001L).faiId(faiId).inspectionItem("厚度").qaResult("OK").build());
        when(qmsFaiOrderMapper.selectById(faiId)).thenReturn(order);
        when(qmsFaiItemMapper.selectListByFaiId(faiId)).thenReturn(items);
        when(qmsFaiSampleMapper.selectListByFaiIdAndItemIdsAndRole(eq(faiId), anyCollection(), eq("QA")))
                .thenReturn(Collections.emptyList());
        when(qmsFaiSampleMapper.selectListByFaiId(faiId)).thenReturn(Collections.emptyList());
        when(qmsFaiAbnormalMapper.selectListByFaiId(faiId)).thenReturn(Collections.emptyList());

        QmsFaiAuditReqVO reqVO = new QmsFaiAuditReqVO();
        reqVO.setId(faiId);
        reqVO.setAuditResult("PASS");
        QmsFaiAuditReqVO.FaiGroupAudit confirmGroup = new QmsFaiAuditReqVO.FaiGroupAudit();
        confirmGroup.setItemId(3001L);
        confirmGroup.setGroupKey("R5");
        confirmGroup.setSamplePosition("R5");
        confirmGroup.setAuditResult("CONFIRM");
        confirmGroup.setAuditRemark("页面明细已确认");
        reqVO.setGroups(List.of(confirmGroup));

        service.auditProgramEntry(reqVO);

        ArgumentCaptor<QmsFaiItemGroupAuditDO> groupAuditCaptor =
                ArgumentCaptor.forClass(QmsFaiItemGroupAuditDO.class);
        verify(qmsFaiItemGroupAuditMapper).insert(groupAuditCaptor.capture());
        QmsFaiItemGroupAuditDO groupAudit = groupAuditCaptor.getValue();
        assertEquals(faiId, groupAudit.getFaiId());
        assertEquals(3001L, groupAudit.getFaiItemId());
        assertEquals("r5", groupAudit.getGroupKey());
        assertEquals("R5", groupAudit.getSamplePosition());
        assertEquals("CONFIRM", groupAudit.getAuditResult());

        ArgumentCaptor<QmsFaiOrderDO> orderCaptor = ArgumentCaptor.forClass(QmsFaiOrderDO.class);
        verify(qmsFaiOrderMapper).updateById(orderCaptor.capture());
        assertEquals("COMPLETED", orderCaptor.getValue().getStatus());
        assertEquals("OK", orderCaptor.getValue().getJudgment());
        verify(qmsSampleAbnormalLockService).syncFromFai(faiId);
    }
}
