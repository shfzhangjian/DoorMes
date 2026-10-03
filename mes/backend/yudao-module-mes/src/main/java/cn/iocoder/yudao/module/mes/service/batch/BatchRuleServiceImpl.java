package cn.iocoder.yudao.module.mes.service.batch;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.batch.BatchRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.batch.BatchRuleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Validated
public class BatchRuleServiceImpl implements BatchRuleService {

    @Resource
    private BatchRuleMapper batchRuleMapper;

    @Override
    public String generateBatchNo(Long productId) {
        // 1. 获取规则 (需处理乐观锁重试)
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            BatchRuleDO rule = getRuleByProductId(productId);

            // 2. 计算新流水号 (含重置逻辑)
            int nextVal = calculateNextValue(rule);

            // 3. 尝试更新 (CAS: Compare And Swap)
            rule.setCurrentVal(nextVal);
            // 手动触更新时间，用于下次重置判断
            rule.setUpdateTime(LocalDateTime.now());

            if (batchRuleMapper.updateById(rule) > 0) {
                // 4. 更新成功，格式化返回
                return formatBatchNo(rule, nextVal);
            }

            // 更新失败(版本号冲突)，重新读取重试
            retryCount++;
        }

        throw new RuntimeException("系统繁忙，批次号生成失败，请重试");
    }

    @Override
    public String generateChildBatchNo(Long productId, String parentBatchNo) {
        BatchRuleDO rule = getRuleByProductId(productId);

        // 继承规则较为简单，通常不占用主流水号，而是加后缀
        // 假设后缀也是通过 current_val 控制 (简单起见复用 update 逻辑，或者应该有独立的 child_seq)
        // 此处演示: 父号 + 分隔符 + 2位随机/流水
        // 实际业务中，分切通常需要单独的子流水计数器，这里简化为引用主规则流水

        // 1. 尝试更新流水
        generateBatchNo(productId); // 借用一次流水更新动作来推进计数器(可选)

        // 2. 格式化
        String separator = StrUtil.blankToDefault(rule.getSeparator(), "-");
        // 模拟：直接在父号后加 01, 02... 需更复杂的子表支持，这里简化处理：
        // 假设 rule.currentVal 已经被上面 generateBatchNo 推着走了
        // 实际上分切场景往往是: 父卷A -> A-01, A-02。这需要记录"父卷A切到了第几个"。
        // 鉴于 BatchRule 表结构限制，V1版本暂不支持针对特定父卷的计数，
        // 我们暂时返回: Parent + Sep + 全局流水 (如 A-089)
        return parentBatchNo + separator + StrUtil.padPre(String.valueOf(rule.getCurrentVal()),
                ObjectUtil.defaultIfNull(rule.getInheritSuffixLen(), 2), '0');
    }

    // ================= 核心辅助逻辑 =================

    private BatchRuleDO getRuleByProductId(Long productId) {
        BatchRuleDO rule = batchRuleMapper.selectOne(BatchRuleDO::getProductId, productId);
        if (rule == null) {
            throw new RuntimeException("未配置产品[" + productId + "]的批次生成规则");
        }
        return rule;
    }

    private int calculateNextValue(BatchRuleDO rule) {
        // 检查重置周期
        boolean needReset = false;
        LocalDateTime lastTime = rule.getUpdateTime();
        LocalDateTime now = LocalDateTime.now();

        if ("DAY".equalsIgnoreCase(rule.getResetCycle())) {
            // 如果最后更新时间不是今天
            if (!DateUtil.isSameDay(DateUtil.date(lastTime), DateUtil.date(now))) {
                needReset = true;
            }
        } else if ("MONTH".equalsIgnoreCase(rule.getResetCycle())) {
            if (lastTime.getMonth() != now.getMonth() || lastTime.getYear() != now.getYear()) {
                needReset = true;
            }
        }

        return needReset ? 1 : (rule.getCurrentVal() + 1);
    }

    private String formatBatchNo(BatchRuleDO rule, int currentVal) {
        StringBuilder sb = new StringBuilder();

        // 前缀
        if (StrUtil.isNotBlank(rule.getPrefix())) {
            sb.append(rule.getPrefix());
        }

        // 日期
        if (StrUtil.isNotBlank(rule.getDateFmt())) {
            sb.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern(rule.getDateFmt())));
        }

        // 流水号
        sb.append(StrUtil.padPre(String.valueOf(currentVal), rule.getSeqLen(), '0'));

        return sb.toString();
    }
}
