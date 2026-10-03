package cn.iocoder.yudao.module.mes.service.hc.productionbatch;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** 计划占号信息复用生产批号上下文，保持 NOT_GEN 与实际开工语义分离。 */
public final class HcRootBatchReservation {
    private HcRootBatchReservation() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> context(HcPlanOrderDO plan) {
        if (plan == null || StrUtil.isBlank(plan.getProductionBatchContextJson())) {
            return Map.of();
        }
        Map<String, Object> values = JsonUtils.parseObject(plan.getProductionBatchContextJson(), Map.class);
        return values == null ? Map.of() : values;
    }

    public static boolean isReplanning(HcPlanOrderDO plan) {
        return Boolean.TRUE.equals(context(plan).get("replanAfterWithdrawal"));
    }

    /** 历史预约保留在上下文，当前预约失效；旧流水不回收。 */
    public static String withdrawnContext(HcPlanOrderDO plan) {
        Map<String, Object> next = new LinkedHashMap<>(context(plan));
        List<Object> history = new ArrayList<>();
        if (next.get("withdrawnReservations") instanceof List<?> previous) history.addAll(previous);
        if (StrUtil.isNotBlank(plan.getBatchNo()) && isReserved(plan)) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("batchNo", plan.getBatchNo());
            snapshot.put("reservationDate", next.get("reservationDate"));
            snapshot.put("reservationAttributes", next.get("reservationAttributes"));
            snapshot.put("ruleId", plan.getBatchRuleId());
            snapshot.put("ruleCode", plan.getBatchRuleCode());
            snapshot.put("ruleVersion", plan.getBatchRuleVersion());
            history.add(snapshot);
        }
        next.remove("reservationDate");
        next.remove("reservedBatchNo");
        next.remove("reservationAttributes");
        next.put("rootBatchReserved", false);
        next.put("replanAfterWithdrawal", true);
        next.put("withdrawnReservations", history);
        // 兼容旧版字段仅作为历史留存，不再用于阻止复用。
        return JsonUtils.toJsonString(next);
    }

    public static boolean isReserved(HcPlanOrderDO plan) {
        return Boolean.TRUE.equals(context(plan).get("rootBatchReserved"));
    }
}
