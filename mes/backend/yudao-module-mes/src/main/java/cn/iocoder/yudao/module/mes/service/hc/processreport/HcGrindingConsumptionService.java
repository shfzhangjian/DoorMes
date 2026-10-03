package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingConsumptionVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingConsumptionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingConsumptionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.service.hc.toolingconsumableledger.HcToolingConsumableLedgerService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** 必须加入来源业务的事务；来源写入失败时消耗和请求占位一起回滚。 */
@Service
@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
public class HcGrindingConsumptionService {
    @Resource private HcGrindingConsumptionMapper mapper;
    @Resource private HcToolingConsumableLedgerMapper ledgerMapper;
    @Resource private HcToolingConsumableConsumeMapper consumeMapper;
    @Resource private HcToolingConsumableLedgerService ledgerService;

    public HcGrindingConsumptionDO begin(String type, HcGrindingConsumptionVO value, Object request) {
        if (value == null) return null; // 无更换的历史调用兼容。
        if (value.getRequestKey() == null || !value.getRequestKey().matches("[A-Za-z0-9_-]{16,64}")) {
            throw invalidParamException("消耗请求标识无效，请重新打开表单");
        }
        String hash = DigestUtil.sha256Hex(JsonUtils.toJsonString(request));
        HcGrindingConsumptionDO row = new HcGrindingConsumptionDO();
        row.setTenantId(TenantContextHolder.getRequiredTenantId());
        row.setRequestKey(value.getRequestKey());
        row.setSourceType(type);
        row.setRequestHash(hash);
        row.setCancelled(false);
        try { mapper.insert(row); }
        catch (DuplicateKeyException duplicate) {
            row = mapper.byRequest(value.getRequestKey());
            if (row == null || !Objects.equals(row.getSourceType(), type)
                    || !Objects.equals(row.getRequestHash(), hash)) {
                throw invalidParamException("本次请求已提交，内容发生变化，请刷新后重新操作");
            }
            if (Boolean.TRUE.equals(row.getCancelled())) throw invalidParamException("本次记录已撤销，请重新登记");
        }
        return row;
    }

    public void finish(HcGrindingConsumptionDO row, Long sourceId, Long resultId, HcGrindingConsumptionVO value,
                       boolean sandpaperChanged, String sandpaperBatch, boolean guideChanged, String guideBatch,
                       LocalDateTime time, String planNo, String batchNo) {
        validate(sandpaperChanged, value == null ? null : value.getSandpaperLedgerId(),
                value == null ? null : value.getSandpaperQty(), "砂纸");
        validate(guideChanged, value == null ? null : value.getGuideClothLedgerId(),
                value == null ? null : value.getGuideClothQty(), "导布");
        if (row == null) return;
        row.setSourceId(sourceId);
        row.setResultId(resultId);
        if (sandpaperChanged) row.setSandpaperConsumeId(book(value, true, sandpaperBatch, time, planNo, batchNo, row));
        if (guideChanged) row.setGuideClothConsumeId(book(value, false, guideBatch, time, planNo, batchNo, row));
        row.setSnapshotJson(JsonUtils.toJsonString(value));
        mapper.updateById(row);
    }

    private void validate(boolean changed, Long ledgerId, BigDecimal qty, String name) {
        if (changed && (ledgerId == null || qty == null || qty.signum() <= 0 || qty.scale() > 3)) {
            throw invalidParamException("更换" + name + "须选择领用台账并填写大于0的消耗量，最多3位小数");
        }
        if (!changed && (ledgerId != null || (qty != null && qty.signum() != 0))) {
            throw invalidParamException("未更换" + name + "不能登记消耗");
        }
    }

    private Long book(HcGrindingConsumptionVO value, boolean sandpaper, String batch,
                      LocalDateTime time, String planNo, String batchNo, HcGrindingConsumptionDO source) {
        Long ledgerId = sandpaper ? value.getSandpaperLedgerId() : value.getGuideClothLedgerId();
        var ledger = ledgerMapper.selectByIdForUpdate(ledgerId);
        String type = sandpaper ? "SANDPAPER" : "GUIDE_CLOTH";
        if (ledger == null || !Objects.equals(ledger.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !"ROUGH_GRINDING".equals(ledger.getProcessCode()) || !type.equals(ledger.getConsumableType())
                || !Objects.equals(ledger.getBatchNo(), batch)) {
            throw invalidParamException("所选耗材领用记录与磨皮工序、耗材类型或更换批号不一致");
        }
        HcToolingConsumableConsumeSaveReqVO req = new HcToolingConsumableConsumeSaveReqVO();
        req.setLedgerId(ledgerId);
        req.setConsumeQty(sandpaper ? value.getSandpaperQty() : value.getGuideClothQty());
        req.setConsumeTime(time);
        req.setPlanNo(planNo);
        req.setProductionBatchNo(batchNo);
        req.setRemark("磨皮更换耗材；来源=" + source.getSourceType() + ":" + source.getSourceId());
        if (sandpaper) value.setSandpaperUnit(ledger.getUomName());
        else value.setGuideClothUnit(ledger.getUomName());
        return ledgerService.createConsume(req);
    }

    public void cancel(String type, Long sourceId) {
        HcGrindingConsumptionDO row = mapper.bySource(type, sourceId, true);
        if (row == null || Boolean.TRUE.equals(row.getCancelled())) return;
        reverse(row.getSandpaperConsumeId());
        reverse(row.getGuideClothConsumeId());
        row.setCancelled(true);
        mapper.updateById(row);
    }

    private void reverse(Long id) {
        if (id == null) return;
        var consume = consumeMapper.selectById(id);
        if (consume == null) throw invalidParamException("关联消耗明细缺失，请核查领用台账");
        var ledger = ledgerMapper.selectByIdForUpdate(consume.getLedgerId());
        if (ledger == null || "RETURNED".equals(ledger.getUsageStatus())) {
            throw invalidParamException("关联耗材已退库或不存在，不能撤销消耗");
        }
        // 用完标记不妨碍来源撤销；保留标记，由台账管理人员按真实状态处理。
        consumeMapper.deleteById(id);
    }

    public void revise(String type, Long sourceId, HcGrindingConsumptionVO value,
                       boolean sandpaperChanged, String sandpaperBatch, boolean guideChanged, String guideBatch,
                       LocalDateTime time, String planNo, String batchNo) {
        HcGrindingConsumptionDO row = mapper.bySource(type, sourceId, true);
        if (row == null) {
            if (value == null) return; // 旧记录不自动补扣。
            row = begin(type, value, value);
            finish(row, sourceId, sourceId, value, sandpaperChanged, sandpaperBatch, guideChanged, guideBatch, time, planNo, batchNo);
            return;
        }
        if (value == null || Boolean.TRUE.equals(row.getCancelled())) throw invalidParamException("关联消耗信息缺失或已撤销，请刷新后维护");
        validate(sandpaperChanged, value.getSandpaperLedgerId(), value.getSandpaperQty(), "砂纸");
        validate(guideChanged, value.getGuideClothLedgerId(), value.getGuideClothQty(), "导布");
        Long sandpaperId = reviseItem(row.getSandpaperConsumeId(), value, true, sandpaperChanged, sandpaperBatch, time, planNo, batchNo, row);
        Long guideId = reviseItem(row.getGuideClothConsumeId(), value, false, guideChanged, guideBatch, time, planNo, batchNo, row);
        // 显式清空取消更换后的关联，避免 MyBatis 非空更新策略保留旧值。
        mapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<HcGrindingConsumptionDO>()
                .eq(HcGrindingConsumptionDO::getId, row.getId())
                .set(HcGrindingConsumptionDO::getSandpaperConsumeId, sandpaperId)
                .set(HcGrindingConsumptionDO::getGuideClothConsumeId, guideId)
                .set(HcGrindingConsumptionDO::getSnapshotJson, JsonUtils.toJsonString(value)));
    }

    private Long reviseItem(Long oldId, HcGrindingConsumptionVO value, boolean sandpaper, boolean changed,
                            String batch, LocalDateTime time, String planNo, String batchNo, HcGrindingConsumptionDO row) {
        if (!changed) { reverse(oldId); return null; }
        Long ledgerId = sandpaper ? value.getSandpaperLedgerId() : value.getGuideClothLedgerId();
        var old = oldId == null ? null : consumeMapper.selectById(oldId);
        if (oldId != null && old == null) throw invalidParamException("关联消耗明细缺失，请核查领用台账");
        if (old != null && Objects.equals(old.getLedgerId(), ledgerId)) {
            var ledger = ledgerMapper.selectByIdForUpdate(ledgerId);
            if (ledger == null || "RETURNED".equals(ledger.getUsageStatus())
                    || !"ROUGH_GRINDING".equals(ledger.getProcessCode())
                    || !(sandpaper ? "SANDPAPER" : "GUIDE_CLOTH").equals(ledger.getConsumableType())
                    || !Objects.equals(ledger.getBatchNo(), batch)) {
                throw invalidParamException("关联耗材已退库或与更换批号不一致，不能修订消耗");
            }
            BigDecimal qty = sandpaper ? value.getSandpaperQty() : value.getGuideClothQty();
            BigDecimal other = consumeMapper.sumConsumeQtyByLedgerId(ledgerId, oldId);
            if (other.add(qty).compareTo(ledger.getReceiveQty()) > 0) throw invalidParamException("修订后的消耗量超过领用余额");
            old.setConsumeQty(qty);
            old.setConsumeTime(time);
            old.setProductionBatchNo(batchNo);
            consumeMapper.updateById(old);
            if (sandpaper) value.setSandpaperUnit(ledger.getUomName());
            else value.setGuideClothUnit(ledger.getUomName());
            return oldId;
        }
        reverse(oldId);
        return book(value, sandpaper, batch, time, planNo, batchNo, row);
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public HcGrindingConsumptionVO get(String type, Long sourceId) {
        if (sourceId == null) return null;
        var row = mapper.bySource(type, sourceId, false);
        return row == null || Boolean.TRUE.equals(row.getCancelled()) || row.getSnapshotJson() == null
                ? null : JsonUtils.parseObject(row.getSnapshotJson(), HcGrindingConsumptionVO.class);
    }
}
