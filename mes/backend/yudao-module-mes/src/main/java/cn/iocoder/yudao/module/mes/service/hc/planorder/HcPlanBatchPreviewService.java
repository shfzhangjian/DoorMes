package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderBatchPreviewReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 计划查询专用预览，不修改计划字段，不占用批号流水。 */
@Service
@Slf4j
public class HcPlanBatchPreviewService {

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcPlanOrderService hcPlanOrderService;

    public Map<Long, String> previewMissingRootBatches(Collection<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }
        List<HcPlanOrderDO> plans = hcPlanOrderMapper.selectBatchIds(planIds).stream()
                .filter(plan -> StrUtil.isAllBlank(plan.getBatchNo(), plan.getProductionBatchNo(),
                        plan.getParentProductionBatchNo(), plan.getInventorySourceBatchNos()))
                .filter(plan -> !"GENERATED".equalsIgnoreCase(plan.getBatchStatus()))
                .toList();
        if (plans.isEmpty()) {
            return Map.of();
        }
        Map<Long, HcPlanOrderOperationDO> firstOperations = new LinkedHashMap<>();
        hcPlanOrderOperationMapper.selectList(new LambdaQueryWrapperX<HcPlanOrderOperationDO>()
                        .in(HcPlanOrderOperationDO::getPlanId, plans.stream().map(HcPlanOrderDO::getId).toList())
                        .orderByAsc(HcPlanOrderOperationDO::getOpSeq)
                        .orderByAsc(HcPlanOrderOperationDO::getSort)
                        .orderByAsc(HcPlanOrderOperationDO::getId))
                .forEach(operation -> firstOperations.putIfAbsent(operation.getPlanId(), operation));
        Map<Long, String> previews = new LinkedHashMap<>();
        for (HcPlanOrderDO plan : plans) {
            HcPlanOrderOperationDO first = firstOperations.get(plan.getId());
            if (first == null || !("FORMULA".equalsIgnoreCase(StrUtil.trim(first.getOpCode()))
                    || "配料".equals(StrUtil.trim(first.getOpName())))
                    || !List.of("NOT_RELEASED", "RELEASED", "PENDING").contains(
                            StrUtil.blankToDefault(first.getOperationStatus(), ""))) {
                continue;
            }
            HcPlanOrderBatchPreviewReqVO request = new HcPlanOrderBatchPreviewReqVO();
            request.setId(plan.getId());
            request.setPlanDate(plan.getPlanDate());
            request.setProductionStartDate(plan.getProductionStartDate());
            request.setMaterialCode(plan.getMaterialCode());
            request.setCategoryCode(plan.getCategoryCode());
            request.setProdType(plan.getProdType());
            request.setModelCode(plan.getModelCode());
            request.setMotherModelCode(plan.getMotherModelCode());
            request.setBatchRuleId(plan.getBatchRuleId());
            request.setBatchRuleCode(plan.getBatchRuleCode());
            request.setUseBoundRule(true);
            request.setOpCode(first.getOpCode());
            request.setOpName(first.getOpName());
            request.setWorkCenterId(first.getWorkCenterId());
            try {
                var preview = hcPlanOrderService.previewRootBatchNo(request);
                if (preview != null && StrUtil.isNotBlank(preview.getBatchNo())) {
                    previews.put(plan.getId(), preview.getBatchNo());
                }
            } catch (RuntimeException exception) {
                // 历史规则缺失不能阻断整个计划表，也不能伪造正式批号。
                log.warn("[计划表母批预览失败] planId={} planNo={}：{}",
                        plan.getId(), plan.getPlanNo(), exception.getMessage());
            }
        }
        return previews;
    }
}
