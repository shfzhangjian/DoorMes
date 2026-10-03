package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.equipment.HcEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.service.hc.equipment.HcEquipmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcCutRoundConsoleServiceImplEquipmentGuardTest {

    @InjectMocks
    private HcCutRoundConsoleServiceImpl service;

    @Mock
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Mock
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Mock
    private HcProcessReportMapper hcProcessReportMapper;
    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock
    private HcEquipmentService hcEquipmentService;

    @Test
    void shouldRejectWhitePadPlanOnBlackPadEquipment() {
        HcPlanOrderDO plan = HcPlanOrderDO.builder()
                .id(91L)
                .planNo("20260723-001")
                .modelCode("W26P0100")
                .categoryCode("WHITE_PAD")
                .build();
        HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder()
                .id(737L)
                .planId(91L)
                .opCode("OP-CUT-ROUND")
                .opName("裁切")
                .operationStatus("RELEASED")
                .workCenterId(16L)
                .build();
        HcEquipmentDO blackPadEquipment = HcEquipmentDO.builder()
                .id(181L)
                .equipmentCode("HC-SB-181")
                .equipmentName("CMP黑垫振动切割机")
                .workCenterId(16L)
                .applicablePadType("BLACK_PAD")
                .status(0)
                .workStatus("IDLE")
                .build();
        when(hcPlanOrderMapper.selectById(91L)).thenReturn(plan);
        when(hcPlanOrderOperationMapper.selectById(737L)).thenReturn(operation);
        when(hcEquipmentService.getHcEquipment(181L)).thenReturn(blackPadEquipment);

        HcAdhesiveReportStartReqVO reqVO = new HcAdhesiveReportStartReqVO();
        reqVO.setPlanId(91L);
        reqVO.setPlanOperationId(737L);
        reqVO.setEquipmentId(181L);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.start(reqVO));

        assertTrue(exception.getMessage().contains("属于白垫"));
        assertTrue(exception.getMessage().contains("不能使用黑垫设备"));
    }

    @Test
    void shouldAllowStartWhenEquipmentIsDisplayingAnotherPlan() {
        HcPlanOrderDO plan = HcPlanOrderDO.builder()
                .id(92L)
                .planNo("20260825-001")
                .modelCode("W26P0100")
                .categoryCode("WHITE_PAD")
                .build();
        HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder()
                .id(738L)
                .planId(92L)
                .opCode("OP-CUT-ROUND")
                .opName("裁切")
                .operationStatus("RELEASED")
                .workCenterId(16L)
                .build();
        HcEquipmentDO whitePadEquipment = HcEquipmentDO.builder()
                .id(180L)
                .equipmentCode("HC-SB-180")
                .equipmentName("CMP白垫振动切割机")
                .workCenterId(16L)
                .applicablePadType("WHITE_PAD")
                .status(0)
                .workStatus("PRODUCING")
                .currentPlanNo("20260820-002")
                .build();
        when(hcPlanOrderMapper.selectById(92L)).thenReturn(plan);
        when(hcPlanOrderOperationMapper.selectById(738L)).thenReturn(operation);
        when(hcEquipmentService.getHcEquipment(180L)).thenReturn(whitePadEquipment);

        HcAdhesiveReportStartReqVO reqVO = new HcAdhesiveReportStartReqVO();
        reqVO.setPlanId(92L);
        reqVO.setPlanOperationId(738L);
        reqVO.setEquipmentId(180L);

        assertDoesNotThrow(() -> service.start(reqVO));

        verify(hcEquipmentService).occupyEquipment(eq(180L), eq("20260825-001"),
                eq("OP-CUT-ROUND"), eq("裁切"), any(), any(), any());
    }

    @Test
    void shouldOnlyReturnTasksCompatibleWithSelectedEquipment() {
        HcAdhesiveReportTaskRespVO whiteRunning = task(1L, "WHITE_PAD", "IN_PROGRESS", 181L);
        HcAdhesiveReportTaskRespVO blackRunning = task(2L, "BLACK_PAD", "IN_PROGRESS", 181L);
        HcAdhesiveReportTaskRespVO blackPending = task(3L, "BLACK_PAD", "PENDING", null);
        HcAdhesiveReportTaskRespVO blackOnOtherEquipment = task(4L, "BLACK_PAD", "IN_PROGRESS", 160L);
        when(hcProcessReportMapper.selectCutRoundTaskList("ALL", null, null, null, null, null))
                .thenReturn(List.of(whiteRunning, blackRunning, blackPending, blackOnOtherEquipment));
        when(hcCutRoundReportMapper.selectListByPlanOperationId(1L)).thenReturn(List.of());
        when(hcCutRoundReportMapper.selectListByPlanOperationId(2L)).thenReturn(List.of());
        when(hcCutRoundReportMapper.selectListByPlanOperationId(3L)).thenReturn(List.of());
        when(hcCutRoundReportMapper.selectListByPlanOperationId(4L)).thenReturn(List.of());
        when(hcEquipmentService.getHcEquipment(181L)).thenReturn(HcEquipmentDO.builder()
                .id(181L)
                .equipmentCode("HC-SB-181")
                .equipmentName("CMP黑垫振动切割机")
                .applicablePadType("BLACK_PAD")
                .status(0)
                .build());
        HcAdhesiveReportTaskPageReqVO reqVO = new HcAdhesiveReportTaskPageReqVO();
        reqVO.setEquipmentId(181L);
        reqVO.setTaskStatus("UNFINISHED");

        List<HcAdhesiveReportTaskRespVO> result = service.getTaskList(reqVO);

        assertEquals(List.of(2L, 3L), result.stream().map(HcAdhesiveReportTaskRespVO::getPlanOperationId).toList());
    }

    private HcAdhesiveReportTaskRespVO task(Long operationId, String categoryCode,
                                            String status, Long equipmentId) {
        HcAdhesiveReportTaskRespVO task = new HcAdhesiveReportTaskRespVO();
        task.setPlanId(operationId);
        task.setPlanOperationId(operationId);
        task.setPlanNo("PLAN-" + operationId);
        task.setModelCode("MODEL-" + operationId);
        task.setCategoryCode(categoryCode);
        task.setStatus(status);
        task.setEquipmentId(equipmentId);
        return task;
    }
}
