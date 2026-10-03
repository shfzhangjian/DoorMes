// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qtime/MesQTimeServiceImpl.java
package cn.iocoder.yudao.module.mes.service.qtime;

import cn.iocoder.yudao.module.mes.dal.dataobject.route.QtimeRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.workorder.MesWorkOrderSubDO;
import cn.iocoder.yudao.module.mes.dal.mysql.route.QtimeRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.workordersub.MesWorkOrderSubMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class MesQTimeServiceImpl implements MesQTimeService {

    @Resource
    private QtimeRuleMapper qtimeRuleMapper;
    @Resource
    private MesWorkOrderSubMapper subOrderMapper;

    @Override
    public boolean checkQTime(Long subOrderId, Long currentProcessId) {
        MesWorkOrderSubDO currentJob = subOrderMapper.selectById(subOrderId);

        // 1. 查询是否有针对当前工序的入场规则 (To Process = Current)
        List<QtimeRuleDO> rules = qtimeRuleMapper.selectList("to_process_id", currentProcessId);
        if (rules.isEmpty()) {
            return true; // 无规则即通过
        }

        // 2. 遍历规则进行检查
        for (QtimeRuleDO rule : rules) {
            // 查找上一道工序的完工时间
            // 简化逻辑：查询同一个工单下，process_id = rule.getFromProcessId() 的 sub_order
            MesWorkOrderSubDO prevJob = subOrderMapper.selectOne(
                    MesWorkOrderSubDO::getWorkOrderId, currentJob.getWorkOrderId(),
                    MesWorkOrderSubDO::getProcessId, rule.getFromProcessId()
            );

            if (prevJob == null || prevJob.getActualEndTime() == null) {
                // 上道工序没做完，理论上不能开始本道，这里返回 false
                return false;
            }

            // 计算时间差
            long minutesDiff = Duration.between(prevJob.getActualEndTime(), LocalDateTime.now()).toMinutes();

            // 最小静置时间约束 (MIN_WAIT)
            if ("MIN_WAIT".equals(rule.getConstraintType())) {
                BigDecimal threshold = rule.getThresholdValue(); // 假设单位是分钟
                if (new BigDecimal(minutesDiff).compareTo(threshold) < 0) {
                    return false; // 时间未到
                }
            }
        }

        return true;
    }
}
