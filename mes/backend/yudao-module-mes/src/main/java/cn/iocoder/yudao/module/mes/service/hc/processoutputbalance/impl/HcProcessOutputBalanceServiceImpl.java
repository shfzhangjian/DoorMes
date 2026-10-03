package cn.iocoder.yudao.module.mes.service.hc.processoutputbalance.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processoutputbalance.vo.HcProcessOutputBalancePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processoutputbalance.HcProcessOutputBalanceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processoutputbalance.HcProcessOutputBalanceMapper;
import cn.iocoder.yudao.module.mes.service.hc.processoutputbalance.HcProcessOutputBalanceService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcProcessOutputBalanceServiceImpl implements HcProcessOutputBalanceService {

    private static final String LOCK_STATUS_ACTIVE = "ACTIVE";
    private static final String LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String LOCK_STATUS_CANCELLED = "CANCELLED";
    private static final String LOCK_STATUS_RELEASED = "RELEASED";

    @Resource
    private HcProcessOutputBalanceMapper processOutputBalanceMapper;
    @Resource
    private HcPlanOrderInventoryLockMapper planOrderInventoryLockMapper;

    @Override
    public PageResult<HcProcessOutputBalanceDO> getProcessOutputBalancePage(HcProcessOutputBalancePageReqVO pageReqVO) {
        PageResult<HcProcessOutputBalanceDO> pageResult = processOutputBalanceMapper.selectPage(pageReqVO);
        fillSourceLockSummary(pageResult.getList());
        return pageResult;
    }

    private void fillSourceLockSummary(List<HcProcessOutputBalanceDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (HcProcessOutputBalanceDO row : rows) {
            if (row == null || row.getSourceTable() == null || row.getSourceId() == null) {
                continue;
            }
            List<HcPlanOrderInventoryLockDO> locks = planOrderInventoryLockMapper
                    .selectListBySource(row.getSourceTable(), row.getSourceId());
            if (locks == null || locks.isEmpty()) {
                continue;
            }
            HcPlanOrderInventoryLockDO activeLock = locks.stream()
                    .filter(this::isActiveWipLock)
                    .findFirst()
                    .orElse(locks.get(0));
            row.setSourcePlanLockId(activeLock.getId());
            row.setSourceLockStatus(activeLock.getLockStatus());
            row.setSourceLockedQty(zeroIfNull(activeLock.getLockQty()));
            row.setSourceLockRemainingQty(calculateRemainingLockQty(activeLock));
            row.setSourceLockTargetPlanNo(activeLock.getTargetPlanNo());
        }
    }

    private boolean isActiveWipLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null || Boolean.TRUE.equals(lock.getDeleted())) {
            return false;
        }
        String status = lock.getLockStatus() == null ? LOCK_STATUS_ACTIVE : lock.getLockStatus();
        if (LOCK_STATUS_CONSUMED.equalsIgnoreCase(status)
                || LOCK_STATUS_CANCELLED.equalsIgnoreCase(status)
                || LOCK_STATUS_RELEASED.equalsIgnoreCase(status)) {
            return false;
        }
        return calculateRemainingLockQty(lock).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateRemainingLockQty(HcPlanOrderInventoryLockDO lock) {
        BigDecimal remainingQty = zeroIfNull(lock.getLockQty())
                .subtract(zeroIfNull(lock.getConsumedQty()))
                .subtract(zeroIfNull(lock.getReleasedQty()));
        return remainingQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingQty;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
