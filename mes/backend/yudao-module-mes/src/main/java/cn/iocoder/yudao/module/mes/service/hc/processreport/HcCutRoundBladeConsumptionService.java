package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 刀片实物更换与边库扣减必须加入裁切更换事务，毛毡仍沿用原流程。 */
@Service
@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
public class HcCutRoundBladeConsumptionService {
    public static final String SOURCE = "CUT_ROUND_BLADE_REPLACE";
    @Resource private HcCutRoundSpareMapper spareMapper;
    @Resource private HcCutRoundSpareRecordMapper recordMapper;
    @Resource private HcToolingConsumableLedgerMapper ledgerMapper;
    @Resource private HcToolingConsumableConsumeMapper consumeMapper;

    public Long replace(HcPressSlotConsumableReplaceReqVO req, HcPlanOrderOperationDO operation, Long equipmentId) {
        validate(req);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(tenantId, operation.getTenantId())) {
            throw invalidParamException("裁切工序不属于当前租户");
        }
        // 先锁设备状态，再锁领用台账；同机更换和维护串行，不同机共享台账也不能超扣。
        HcCutRoundSpareDO state = spareMapper.selectForUpdate(equipmentId, "CUTTING_BLADE");
        if (state == null || !Objects.equals(state.getTenantId(), tenantId)) {
            throw invalidParamException("请先在裁切备件管理维护刀片基础信息");
        }
        String hash = DigestUtil.sha256Hex(JsonUtils.toJsonString(req));
        // 直接以唯一索引占位，不先对不存在的请求做范围锁，避免不同设备更换的间隙锁死锁。
        var reservation = HcCutRoundSpareRecordDO.builder().tenantId(tenantId).spareId(state.getId())
                .equipmentId(equipmentId).spareType("CUTTING_BLADE").eventType("REPLACE")
                .planOperationId(operation.getId()).requestKey(req.getRequestKey()).requestHash(hash)
                .eventTime(req.getReplaceTime()).build();
        try {
            recordMapper.insert(reservation);
        } catch (DuplicateKeyException duplicate) {
            HcCutRoundSpareRecordDO previous = recordMapper.byRequest(req.getRequestKey());
            if (previous == null || !Objects.equals(previous.getRequestHash(), hash)
                    || !Objects.equals(previous.getEquipmentId(), equipmentId)
                    || !Objects.equals(previous.getPlanOperationId(), operation.getId())) {
                throw invalidParamException("本次更换已提交，内容发生变化，请刷新后重新操作");
            }
            return previous.getSpareId();
        }
        var ledger = ledgerMapper.selectByIdForUpdate(req.getLedgerId());
        if (ledger == null || !Objects.equals(tenantId, ledger.getTenantId())
                || !"CUT_ROUND".equals(ledger.getProcessCode()) || !"BLADE".equals(ledger.getConsumableType())) {
            throw invalidParamException("请选择裁切工序的刀片领用记录");
        }
        if (!"ACTIVE".equals(ledger.getUsageStatus())) {
            throw invalidParamException("所选刀片台账已完成或已退库，请重新领用");
        }
        BigDecimal qty = req.getReplaceQuantity();
        // 使用锁定读取，避免事务此前快照导致并发下读到旧消耗。
        BigDecimal balance = (ledger.getReceiveQty() == null ? BigDecimal.ZERO : ledger.getReceiveQty())
                .subtract(consumeMapper.sumConsumeQtyForUpdate(ledger.getId()));
        if (balance.compareTo(qty) < 0) {
            throw invalidParamException("裁切边库刀片余量不足，请先完成刀片领用；当前余量：" + balance);
        }
        var consume = HcToolingConsumableConsumeDO.builder()
                .ledgerId(ledger.getId()).tenantId(tenantId).consumableType("BLADE").consumableTypeName("刀片")
                .processCode("CUT_ROUND").processName("裁切").model(ledger.getModel()).batchNo(ledger.getBatchNo())
                .consumeQty(qty).consumeTime(req.getReplaceTime()).planNo(req.getPlanNo())
                .planOperationId(operation.getId()).consumeSource(SOURCE).consumeType("REPLACE")
                .remark("设备：" + state.getEquipmentCode() + "；更换原因：" + req.getReplaceReason()).build();
        consumeMapper.insert(consume);
        int beforeCount = state.getUseCount() == null ? 0 : state.getUseCount();
        Long operatorId = req.getOperatorId() == null ? SecurityFrameworkUtils.getLoginUserId() : req.getOperatorId();
        String operatorName = req.getOperatorName() == null ? SecurityFrameworkUtils.getLoginUserNickname() : req.getOperatorName();
        var record = HcCutRoundSpareRecordDO.builder().id(reservation.getId())
                .tenantId(tenantId).spareId(state.getId()).equipmentId(equipmentId)
                .equipmentCode(state.getEquipmentCode()).equipmentName(state.getEquipmentName())
                .workCenterId(operation.getWorkCenterId()).workCenterCode(operation.getWorkCenterCode())
                .workCenterName(operation.getWorkCenterName()).spareType("CUTTING_BLADE").eventType("REPLACE")
                .planId(operation.getPlanId()).planNo(req.getPlanNo()).planOperationId(operation.getId())
                .operationCode(operation.getOpCode()).operationName(operation.getOpName())
                .beforeUseCount(beforeCount).afterUseCount(0).changeUseCount(-beforeCount).finalUseCount(0)
                .beforeMaterialCode(state.getMaterialCode()).beforeBatchNo(state.getBatchNo())
                .afterMaterialCode(ledger.getErpMaterialCode()).afterBatchNo(ledger.getBatchNo())
                .beforeAvailableQuantity(balance).afterAvailableQuantity(balance.subtract(qty)).changeQuantity(qty.negate())
                .onlineQuantity(qty).offlineQuantity(state.getOnlineQuantity())
                .ledgerId(ledger.getId()).consumeId(consume.getId()).replaceQuantity(qty)
                .requestKey(req.getRequestKey()).requestHash(hash)
                .operatorId(operatorId).operatorName(operatorName).eventTime(req.getReplaceTime())
                .replaceReason(req.getReplaceReason()).remark(req.getReplaceReason()).build();
        recordMapper.updateById(record); // 库存不足或后续失败时，占位与消耗一并回滚。
        var update = HcCutRoundSpareDO.builder().id(state.getId()).materialCode(ledger.getErpMaterialCode())
                .materialName(ledger.getModel()).batchNo(ledger.getBatchNo()).onlineQuantity(qty)
                .useCount(0).warningFlag(0).status("ACTIVE").lastReplaceTime(req.getReplaceTime())
                .lastReplacePlanNo(req.getPlanNo()).lastReplaceReason(req.getReplaceReason())
                .lastOperatorId(operatorId).lastOperatorName(operatorName).lastEventTime(req.getReplaceTime()).build();
        spareMapper.updateById(update);
        return state.getId();
    }

    private void validate(HcPressSlotConsumableReplaceReqVO req) {
        if (req.getRequestKey() == null || !req.getRequestKey().matches("[A-Za-z0-9_-]{16,64}")) {
            throw invalidParamException("更换请求标识无效，请重新打开更换窗口");
        }
        if (req.getLedgerId() == null) throw invalidParamException("请先领用刀片并选择对应领用记录");
        BigDecimal qty = req.getReplaceQuantity();
        if (qty == null || qty.signum() <= 0 || qty.stripTrailingZeros().scale() > 0) {
            throw invalidParamException("刀片更换数量必须为正整数");
        }
        if (req.getInitialUseCount() != null && req.getInitialUseCount() != 0) {
            throw invalidParamException("更换新刀片的初始使用片数必须为0，历史纠错请使用备件维护");
        }
        if (req.getReplaceTime() == null || req.getReplaceTime().getYear() < 2000) {
            throw invalidParamException("请填写有效的本次更换时间");
        }
        if (req.getReplaceReason() == null || req.getReplaceReason().isBlank()) {
            throw invalidParamException("请填写刀片更换原因");
        }
    }
}
