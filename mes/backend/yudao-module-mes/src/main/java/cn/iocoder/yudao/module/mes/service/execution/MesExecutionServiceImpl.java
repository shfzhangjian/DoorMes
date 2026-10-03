// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/execution/MesExecutionServiceImpl.java
package cn.iocoder.yudao.module.mes.service.execution;

import cn.iocoder.yudao.module.mes.controller.admin.execution.vo.MesActionExecuteReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessActionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderActionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.dal.mysql.routeprocess.RouteProcessActionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workorderaction.MesWorkOrderActionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workordersub.MesWorkOrderSubMapper;
import cn.iocoder.yudao.module.mes.service.feed.MesProdFeedService; // 假设存在
import cn.iocoder.yudao.module.mes.service.qms.MesQmsService; // 假设存在
import cn.iocoder.yudao.module.mes.service.qtime.MesQTimeService; // 假设存在
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Validated
public class MesExecutionServiceImpl implements MesExecutionService {

    @Resource
    private MesWorkOrderSubMapper subOrderMapper;
    @Resource
    private RouteProcessActionMapper routeActionMapper;
    @Resource
    private MesWorkOrderActionMapper workOrderActionMapper;

    // 依赖的其他领域服务 (Mock 目标)
    @Resource private MesProdFeedService feedService;
    @Resource private MesQmsService qmsService;
    @Resource private MesQTimeService qTimeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeAction(MesActionExecuteReqVO req) {
        // 1. 获取上下文 (Context)
        MesWorkOrderSubDO subOrder = subOrderMapper.selectById(req.getSubOrderId());
        if (subOrder == null) {
            throw new RuntimeException("派工单不存在");
        }

        // 2. 获取动作定义 (Definition)
        RouteProcessActionDO actionDef = routeActionMapper.selectById(req.getActionId());
        if (actionDef == null) {
            throw new RuntimeException("SOP动作定义不存在");
        }

        // 3. 规则校验: Q-Time (仅针对 PRE_CHECK 动作)
        if ("PRE_CHECK".equals(actionDef.getTriggerMoment())) {
            boolean qTimePass = qTimeService.checkQTime(subOrder.getId(), actionDef.getProcessId());
            if (!qTimePass) {
                throw new RuntimeException("Q-Time 冷却/静置时间未到，禁止作业");
            }
        }

        // 4. 执行结果判定 (NG 处理)
        if (Boolean.FALSE.equals(req.getIsPass())) {
            handleFailure(req, subOrder, actionDef);
        }

        // 5. 数据分流 (Data Routing) - 核心逻辑
        // 根据 dataMapping 或 actionCode 特征分流
        dispatchData(req, subOrder, actionDef);

        // 6. 持久化通用动作记录 (Snapshot)
        saveActionRecord(req, subOrder, actionDef);
    }

    /**
     * 核心分流逻辑: 决定数据写入 Feed 表还是 Check 表
     */
    private void dispatchData(MesActionExecuteReqVO req, MesWorkOrderSubDO subOrder, RouteProcessActionDO actionDef) {
        Map<String, Object> mapping = actionDef.getDataMapping();
        String actionName = actionDef.getActionName();

        // 策略 A: 投料类 (Scan Material)
        // 识别特征: 名字含"投料" 或 mapping 包含 target=mes_prod_feed
        if (actionName.contains("投料") || (mapping != null && "mes_prod_feed".equals(mapping.get("target")))) {
            feedService.createFeedRecord(subOrder.getId(), req.getActionValue());
        }

        // 策略 B: 质检类 (Quality Check)
        // 识别特征: 名字含"检验" 或 "检测"
        else if (actionName.contains("检验") || actionName.contains("检测")) {
            // qmsService.createCheckRecord(...) // 暂略
        }
    }

    /**
     * 异常处理策略
     */
    private void handleFailure(MesActionExecuteReqVO req, MesWorkOrderSubDO subOrder, RouteProcessActionDO actionDef) {
        // 1. 创建不合格记录 (NCR)
        qmsService.createNcRecord(subOrder.getId(), req.getActionId(), req.getFailureReason());

        // 2. 如果策略是阻断 (BLOCK)，则锁定工单
        if ("BLOCK".equals(actionDef.getErrorStrategy())) {
            subOrder.setStatus("BLOCKED");
            subOrderMapper.updateById(subOrder);
            throw new RuntimeException("检测到异常，工单已自动锁定: " + req.getFailureReason());
        }
    }

    /**
     * 保存动作执行快照 (Traceability)
     */
    private void saveActionRecord(MesActionExecuteReqVO req, MesWorkOrderSubDO subOrder, RouteProcessActionDO actionDef) {
        MesWorkOrderActionDO record = MesWorkOrderActionDO.builder()
                .tenantId(subOrder.getTenantId())
                .workOrderId(subOrder.getWorkOrderId())
                .subOrderId(subOrder.getId())
                .routeId(subOrder.getRouteId()) // 需从 SubOrder 或关联表获取
                .routeProcessActionId(actionDef.getId())
                .stationId(req.getStationId()) // 实际执行工位
                // 快照冗余
                .actionCode(actionDef.getActionCode())
                .actionName(actionDef.getActionName())
                .actionConfig(actionDef.getActionConfig()) // 保存当时的 Schema
                .dataMapping(actionDef.getDataMapping())   // 保存当时的 Mapping
                // 实绩数据
                .actionValue(req.getActionValue())
                .pass(req.getIsPass())
                .failureReason(req.getFailureReason())
                .executeTime(LocalDateTime.now())
                .executorUser("1") // TODO: 从 SecurityContext 获取
                .remark(req.getRemark())
                .build();

        workOrderActionMapper.insert(record);
    }
}
