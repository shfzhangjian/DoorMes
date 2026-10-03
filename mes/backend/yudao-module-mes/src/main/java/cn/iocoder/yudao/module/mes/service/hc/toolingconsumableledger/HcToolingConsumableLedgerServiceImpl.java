package cn.iocoder.yudao.module.mes.service.hc.toolingconsumableledger;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableBalanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerMarkUsedUpReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableLedgerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingProcessConsumablePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingProcessConsumableSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.finishedglueboardmap.HcFinishedGlueBoardMapItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingProcessConsumableDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.unit.UnitDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingProcessConsumableMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.unit.UnitMapper;
import cn.iocoder.yudao.module.mes.service.hc.finishedglueboardmap.HcFinishedGlueBoardMapService;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcToolingConsumableLedgerServiceImpl implements HcToolingConsumableLedgerService {
    @Resource
    private cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingConsumptionMapper grindingConsumptionMapper;


    private static final String TYPE_SANDPAPER = "SANDPAPER";
    private static final String TYPE_GUIDE_CLOTH = "GUIDE_CLOTH";
    private static final String TYPE_PET = "PET";
    private static final String TYPE_GLUE_BOARD = "GLUE_BOARD";
    private static final String TYPE_BEARING = "BEARING";
    private static final String TYPE_PRESS_ROLLER = "PRESS_ROLLER";
    private static final String TYPE_FELT = "FELT";
    private static final String TYPE_BLADE = "BLADE";
    private static final String TYPE_PACKAGING_BAG = "PACKAGING_BAG";
    private static final String TYPE_ISOLATION_FILM = "ISOLATION_FILM";
    private static final String TYPE_PAPER_BOX = "PAPER_BOX";
    private static final String TYPE_PAPER_BOARD = "PAPER_BOARD";

    private static final String PROCESS_ROUGH_GRINDING = "ROUGH_GRINDING";
    private static final String PROCESS_WET = "WET";
    private static final String PROCESS_ADHESIVE = "ADHESIVE";
    private static final String PROCESS_ADHESIVE1 = "ADHESIVE1";
    private static final String PROCESS_ADHESIVE2 = "ADHESIVE2";
    private static final String PROCESS_PRESS_SLOT = "PRESS_SLOT";
    private static final String PROCESS_CUT_ROUND = "CUT_ROUND";
    private static final String PROCESS_PACKAGING = "PACKAGING";

    private static final String LEDGER_USAGE_STATUS_ACTIVE = "ACTIVE";
    private static final String LEDGER_USAGE_STATUS_USED_UP = "USED_UP";
    private static final String LEDGER_USAGE_STATUS_RETURNED = "RETURNED";

    private static final String GLUE_BOARD_SOURCE_TYPE_TOOLING_LEDGER = "TOOLING_CONSUMABLE_LEDGER";
    private static final String GLUE_BOARD_STOCK_ACTIVE = "ACTIVE";
    private static final String GLUE_BOARD_STOCK_USED_UP = "USED_UP";
    private static final String GLUE_BOARD_STOCK_LOCKED = "LOCKED";
    private static final String GLUE_BOARD_STOCK_RETURNED = "RETURNED";
    private static final String GLUE_BOARD_STOCK_MEASURE_LENGTH = "LENGTH";
    private static final String GLUE_BOARD_LIFETIME_NONE = "NONE";
    private static final String GLUE_BOARD_QUALITY_NORMAL = "NORMAL";
    private static final String GLUE_BOARD_ERP_TRANSFER_NOT_SYNCED = "NOT_SYNCED";
    private static final String CONSUME_SOURCE_LEDGER_MANUAL = "LEDGER_MANUAL";
    private static final String CONSUME_TYPE_NORMAL = "NORMAL";
    private static final String CONSUME_TYPE_CORRECTION = "CORRECTION";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> CONSUMABLE_TYPE_NAMES = Map.ofEntries(
            Map.entry(TYPE_SANDPAPER, "砂纸"),
            Map.entry(TYPE_GUIDE_CLOTH, "导布"),
            Map.entry(TYPE_PET, "PET"),
            Map.entry(TYPE_GLUE_BOARD, "胶板"),
            Map.entry(TYPE_BEARING, "轴承"),
            Map.entry(TYPE_PRESS_ROLLER, "压槽辊"),
            Map.entry(TYPE_FELT, "毛毡"),
            Map.entry(TYPE_BLADE, "刀片"),
            Map.entry(TYPE_PACKAGING_BAG, "包装袋"),
            Map.entry(TYPE_ISOLATION_FILM, "隔离膜"),
            Map.entry(TYPE_PAPER_BOX, "纸盒"),
            Map.entry(TYPE_PAPER_BOARD, "纸板")
    );

    private static final Map<String, String> PROCESS_NAMES = Map.of(
            PROCESS_ROUGH_GRINDING, "磨皮",
            PROCESS_WET, "湿法",
            PROCESS_ADHESIVE, "粘胶1",
            PROCESS_ADHESIVE1, "粘胶1",
            PROCESS_ADHESIVE2, "粘胶2",
            PROCESS_PRESS_SLOT, "压槽",
            PROCESS_CUT_ROUND, "裁切",
            PROCESS_PACKAGING, "包装工序"
    );

    private static final Map<String, String> LEDGER_USAGE_STATUS_NAMES = Map.of(
            LEDGER_USAGE_STATUS_ACTIVE, "使用中",
            LEDGER_USAGE_STATUS_USED_UP, "已用完",
            LEDGER_USAGE_STATUS_RETURNED, "已退库"
    );

    private static final Map<String, Set<String>> ALLOWED_PROCESS_BY_TYPE = Map.ofEntries(
            Map.entry(TYPE_SANDPAPER, Set.of(PROCESS_ROUGH_GRINDING)),
            Map.entry(TYPE_GUIDE_CLOTH, Set.of(PROCESS_ROUGH_GRINDING, PROCESS_WET)),
            Map.entry(TYPE_PET, Set.of(PROCESS_WET)),
            Map.entry(TYPE_GLUE_BOARD, Set.of(PROCESS_ADHESIVE, PROCESS_ADHESIVE1, PROCESS_ADHESIVE2)),
            Map.entry(TYPE_BEARING, Set.of(PROCESS_PRESS_SLOT)),
            Map.entry(TYPE_PRESS_ROLLER, Set.of(PROCESS_PRESS_SLOT)),
            Map.entry(TYPE_FELT, Set.of(PROCESS_CUT_ROUND)),
            Map.entry(TYPE_BLADE, Set.of(PROCESS_CUT_ROUND)),
            Map.entry(TYPE_PACKAGING_BAG, Set.of(PROCESS_PACKAGING)),
            Map.entry(TYPE_ISOLATION_FILM, Set.of(PROCESS_PACKAGING)),
            Map.entry(TYPE_PAPER_BOX, Set.of(PROCESS_PACKAGING)),
            Map.entry(TYPE_PAPER_BOARD, Set.of(PROCESS_PACKAGING))
    );

    @Resource
    private HcToolingConsumableLedgerMapper hcToolingConsumableLedgerMapper;

    @Resource
    private HcToolingConsumableConsumeMapper hcToolingConsumableConsumeMapper;

    @Resource
    private HcToolingProcessConsumableMapper hcToolingProcessConsumableMapper;

    @Resource
    private UnitMapper unitMapper;

    @Resource
    private HcAdhesiveGlueBoardStockMapper hcAdhesiveGlueBoardStockMapper;

    @Resource
    private HcAdhesiveGlueBoardUsageMapper hcAdhesiveGlueBoardUsageMapper;

    @Resource
    private HcFinishedGlueBoardMapService finishedGlueBoardMapService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLedger(HcToolingConsumableLedgerSaveReqVO reqVO) {
        HcToolingConsumableLedgerDO entity = buildLedgerDO(reqVO);
        entity.setUsageStatus(LEDGER_USAGE_STATUS_ACTIVE);
        entity.setUsedUpRemainQty(null);
        entity.setUsedUpActualDate(null);
        entity.setUsedUpRemark(null);
        entity.setUsedUpAuthUserId(null);
        entity.setUsedUpAuthUserName(null);
        entity.setUsedUpAuthTime(null);
        clearLedgerReturnInfo(entity);
        entity.setTenantId(TenantContextHolder.getRequiredTenantId());
        hcToolingConsumableLedgerMapper.insert(entity);
        syncGlueBoardStockAfterLedgerSave(null, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLedger(HcToolingConsumableLedgerSaveReqVO reqVO) {
        HcToolingConsumableLedgerDO oldLedger = getRequiredLedgerForUpdate(reqVO.getId());
        if (isLedgerReturned(oldLedger)) {
            throw invalidParamException("已退库的边库耗材台账不能修改");
        }
        if (isAdhesiveGlueBoardLedger(oldLedger)) {
            requireLedgerActive(oldLedger, "修改胶板台账");
        }
        HcToolingConsumableLedgerDO entity = buildLedgerDO(reqVO);
        entity.setUsageStatus(requireLedgerUsageStatus(oldLedger.getUsageStatus()));
        entity.setUsedUpRemainQty(oldLedger.getUsedUpRemainQty());
        entity.setUsedUpActualDate(oldLedger.getUsedUpActualDate());
        entity.setUsedUpRemark(oldLedger.getUsedUpRemark());
        entity.setUsedUpAuthUserId(oldLedger.getUsedUpAuthUserId());
        entity.setUsedUpAuthUserName(oldLedger.getUsedUpAuthUserName());
        entity.setUsedUpAuthTime(oldLedger.getUsedUpAuthTime());
        copyLedgerReturnInfo(oldLedger, entity);
        assertBladeLedgerUnchanged(oldLedger, entity);
        validateLedgerReceiveQtyEnough(entity.getId(), entity.getReceiveQty());
        hcToolingConsumableLedgerMapper.updateById(entity);
        syncGlueBoardStockAfterLedgerSave(oldLedger, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLedgerUsageStatus(Long id, String usageStatus) {
        HcToolingConsumableLedgerDO ledger = getRequiredLedgerForUpdate(id);
        String normalizedUsageStatus = requireLedgerUsageStatus(usageStatus);
        if (isAdhesiveGlueBoardLedger(ledger)) {
            throw invalidParamException("粘胶胶板请使用标记完成或退库登记，不能直接切换状态");
        }
        if (LEDGER_USAGE_STATUS_RETURNED.equals(normalizedUsageStatus)) {
            throw invalidParamException("退库请使用退库登记操作");
        }
        if (isLedgerReturned(ledger)) {
            throw invalidParamException("已退库的边库耗材台账不能修改使用状态");
        }
        HcToolingConsumableLedgerDO updateObj = new HcToolingConsumableLedgerDO();
        updateObj.setId(ledger.getId());
        updateObj.setUsageStatus(normalizedUsageStatus);
        hcToolingConsumableLedgerMapper.updateById(updateObj);
        if (LEDGER_USAGE_STATUS_USED_UP.equals(normalizedUsageStatus)) {
            markAdhesiveGlueBoardLedgerUsedUp(ledger);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markLedgerUsedUp(HcToolingConsumableLedgerMarkUsedUpReqVO reqVO) {
        HcToolingConsumableLedgerDO ledger = getRequiredLedgerForUpdate(reqVO.getId());
        requireLedgerActive(ledger, "标记完成");
        if (isAdhesiveGlueBoardLedger(ledger)) {
            settleAdhesiveGlueBoardLedger(ledger, reqVO);
            return;
        }
        validateQty(reqVO.getUsedUpRemainQty(), "用完余料量");
        if (reqVO.getUsedUpActualDate() == null) {
            throw invalidParamException("实际消耗日期不能为空");
        }
        String authUserName = requireText(reqVO.getUsedUpAuthUserName(), "认证人不能为空");
        HcToolingConsumableLedgerDO updateObj = new HcToolingConsumableLedgerDO();
        updateObj.setId(ledger.getId());
        updateObj.setUsageStatus(LEDGER_USAGE_STATUS_USED_UP);
        updateObj.setUsedUpRemainQty(reqVO.getUsedUpRemainQty());
        updateObj.setUsedUpActualDate(reqVO.getUsedUpActualDate());
        updateObj.setUsedUpRemark(trimToNull(reqVO.getUsedUpRemark()));
        updateObj.setUsedUpAuthUserId(reqVO.getUsedUpAuthUserId());
        updateObj.setUsedUpAuthUserName(authUserName);
        updateObj.setUsedUpAuthTime(LocalDateTime.now());
        hcToolingConsumableLedgerMapper.updateById(updateObj);
        markAdhesiveGlueBoardLedgerUsedUp(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnLedger(HcToolingConsumableLedgerReturnReqVO reqVO) {
        HcToolingConsumableLedgerDO ledger = getRequiredLedgerForUpdate(reqVO.getId());
        requireLedgerActive(ledger, "退库");
        String returnReason = requireText(reqVO.getReturnReason(), "退库原因不能为空");
        String authUserName = requireText(reqVO.getReturnAuthUserName(), "认证人不能为空");

        BigDecimal returnQty = calculateReturnQty(ledger);
        if (returnQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("当前边库耗材没有可退库余量");
        }
        markAdhesiveGlueBoardLedgerReturned(ledger);

        HcToolingConsumableLedgerDO updateObj = new HcToolingConsumableLedgerDO();
        updateObj.setId(ledger.getId());
        updateObj.setUsageStatus(LEDGER_USAGE_STATUS_RETURNED);
        updateObj.setReturnQty(returnQty);
        updateObj.setReturnReason(returnReason);
        updateObj.setReturnAuthUserId(reqVO.getReturnAuthUserId());
        updateObj.setReturnAuthUserName(authUserName);
        updateObj.setReturnAuthTime(LocalDateTime.now());
        hcToolingConsumableLedgerMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLedger(Long id) {
        HcToolingConsumableLedgerDO ledger = getRequiredLedgerForUpdate(id);
        if (isLedgerReturned(ledger)) {
            throw invalidParamException("已退库的边库耗材台账需保留审计记录，不能删除");
        }
        if (isAdhesiveGlueBoardLedger(ledger)) {
            requireLedgerActive(ledger, "删除胶板台账");
        }
        (TYPE_BLADE.equals(ledger.getConsumableType())
                ? hcToolingConsumableConsumeMapper.selectListForUpdate(id)
                : hcToolingConsumableConsumeMapper.selectListByLedgerIds(List.of(id)))
                .forEach(consume -> {
                    assertNotBladeConsumption(consume);
                    assertNotAutomaticConsumption(consume.getId());
                });
        deleteLinkedGlueBoardStockBeforeLedgerDelete(ledger);
        hcToolingConsumableConsumeMapper.deleteByLedgerId(id);
        hcToolingConsumableLedgerMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLedgerList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            deleteLedger(id);
        }
    }

    @Override
    public HcToolingConsumableLedgerDO getLedger(Long id) {
        return hcToolingConsumableLedgerMapper.selectById(id);
    }

    @Override
    public PageResult<HcToolingConsumableLedgerDO> getLedgerPage(HcToolingConsumableLedgerPageReqVO reqVO) {
        PageResult<HcToolingConsumableLedgerDO> page = hcToolingConsumableLedgerMapper.selectPage(reqVO);
        fillLedgerCurrentBalance(page.getList());
        return page;
    }

    @Override
    public PageResult<HcToolingConsumableBalanceRespVO> getBalancePage(HcToolingConsumableLedgerPageReqVO reqVO) {
        if (Boolean.TRUE.equals(reqVO.getOnlyPositiveBalance())) {
            return buildPositiveBalancePage(reqVO);
        }
        PageResult<HcToolingConsumableLedgerDO> page = hcToolingConsumableLedgerMapper.selectBalancePage(reqVO);
        return new PageResult<>(buildBalanceList(page.getList()), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConsume(HcToolingConsumableConsumeSaveReqVO reqVO) {
        HcToolingConsumableConsumeDO entity = buildConsumeDO(reqVO);
        prepareGlueBoardConsumeLink(entity, reqVO.getGlueBoardUsageId());
        validateConsumeBalance(entity.getLedgerId(), entity.getConsumeQty(), null);
        entity.setTenantId(TenantContextHolder.getRequiredTenantId());
        hcToolingConsumableConsumeMapper.insert(entity);
        syncGlueBoardStockAfterConsumeChange(null, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConsume(HcToolingConsumableConsumeSaveReqVO reqVO) {
        assertNotAutomaticConsumption(reqVO.getId());
        HcToolingConsumableConsumeDO oldConsume = getRequiredConsume(reqVO.getId());
        HcToolingConsumableConsumeDO entity = buildConsumeDO(reqVO);
        preserveGlueBoardConsumeLink(oldConsume, entity);
        validateConsumeBalance(entity.getLedgerId(), entity.getConsumeQty(), entity.getId());
        hcToolingConsumableConsumeMapper.updateById(entity);
        syncGlueBoardStockAfterConsumeChange(oldConsume, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConsume(Long id) {
        assertNotAutomaticConsumption(id);
        HcToolingConsumableConsumeDO consume = getRequiredConsume(id);
        requireLedgerActive(getRequiredLedgerForUpdate(consume.getLedgerId()), "修改消耗明细");
        syncGlueBoardStockAfterConsumeChange(consume, null);
        hcToolingConsumableConsumeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConsumeList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            deleteConsume(id);
        }
    }

    @Override
    public HcToolingConsumableConsumeDO getConsume(Long id) {
        return hcToolingConsumableConsumeMapper.selectById(id);
    }

    @Override
    public PageResult<HcToolingConsumableConsumeDO> getConsumePage(HcToolingConsumableConsumePageReqVO reqVO) {
        return hcToolingConsumableConsumeMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<HcToolingConsumableConsumeRespVO> getConsumeRecordPage(HcToolingConsumableConsumePageReqVO reqVO) {
        PageResult<HcToolingConsumableConsumeDO> page = hcToolingConsumableConsumeMapper.selectPage(reqVO);
        return new PageResult<>(buildConsumeRecordList(page.getList()), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProcessConsumable(HcToolingProcessConsumableSaveReqVO reqVO) {
        HcToolingProcessConsumableDO entity = buildProcessConsumableDO(reqVO);
        validateProcessConsumableUnique(entity.getId(), entity.getProcessCode(), entity.getConsumableType());
        entity.setTenantId(TenantContextHolder.getRequiredTenantId());
        hcToolingProcessConsumableMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessConsumable(HcToolingProcessConsumableSaveReqVO reqVO) {
        validateProcessConsumableExists(reqVO.getId());
        HcToolingProcessConsumableDO entity = buildProcessConsumableDO(reqVO);
        validateProcessConsumableUnique(entity.getId(), entity.getProcessCode(), entity.getConsumableType());
        hcToolingProcessConsumableMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessConsumable(Long id) {
        validateProcessConsumableExists(id);
        hcToolingProcessConsumableMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessConsumableList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        hcToolingProcessConsumableMapper.deleteByIds(ids);
    }

    @Override
    public HcToolingProcessConsumableDO getProcessConsumable(Long id) {
        return hcToolingProcessConsumableMapper.selectById(id);
    }

    @Override
    public PageResult<HcToolingProcessConsumableDO> getProcessConsumablePage(HcToolingProcessConsumablePageReqVO reqVO) {
        return hcToolingProcessConsumableMapper.selectPage(reqVO);
    }

    @Override
    public List<HcToolingProcessConsumableDO> getEnabledProcessConsumableList(String processCode) {
        String normalizedProcess = normalizeProcessCode(processCode, null);
        if (StrUtil.isNotBlank(processCode) && StrUtil.isBlank(normalizedProcess)) {
            throw invalidParamException("工序不支持");
        }
        List<HcToolingProcessConsumableDO> list = hcToolingProcessConsumableMapper.selectListByProcessCode(normalizedProcess);
        if (list.isEmpty() && PROCESS_ADHESIVE.equals(normalizedProcess)) {
            return hcToolingProcessConsumableMapper.selectListByProcessCode(PROCESS_ADHESIVE1);
        }
        return list;
    }

    @Override
    public List<HcToolingConsumableLedgerExcelVO> buildExportList(HcToolingConsumableLedgerPageReqVO reqVO) {
        List<HcToolingConsumableLedgerDO> ledgers = hcToolingConsumableLedgerMapper.selectList(reqVO);
        if (ledgers.isEmpty()) {
            return List.of();
        }
        fillLedgerCurrentBalance(ledgers);
        Map<Long, List<HcToolingConsumableConsumeDO>> consumeMap = hcToolingConsumableConsumeMapper
                .selectListByLedgerIds(ledgers.stream().map(HcToolingConsumableLedgerDO::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(HcToolingConsumableConsumeDO::getLedgerId,
                        LinkedHashMap::new, Collectors.toList()));
        List<HcToolingConsumableLedgerExcelVO> excelList = new ArrayList<>();
        for (HcToolingConsumableLedgerDO ledger : ledgers) {
            List<HcToolingConsumableConsumeDO> consumeList = consumeMap.get(ledger.getId());
            if (consumeList == null || consumeList.isEmpty()) {
                excelList.add(toExcelVO(ledger, null));
                continue;
            }
            consumeList.forEach(consume -> excelList.add(toExcelVO(ledger, consume)));
        }
        return excelList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcToolingConsumableLedgerImportRespVO importExcel(MultipartFile file, String processCode) throws IOException {
        HcToolingConsumableLedgerImportRespVO respVO = new HcToolingConsumableLedgerImportRespVO();
        if (file == null || file.isEmpty()) {
            addImportFailure(respVO, "导入文件为空");
            return respVO;
        }
        String fixedProcessCode = normalizeProcessCode(processCode, null);
        if (StrUtil.isNotBlank(processCode) && StrUtil.isBlank(fixedProcessCode)) {
            addImportFailure(respVO, "当前菜单工序不支持：" + processCode);
            return respVO;
        }

        List<HcToolingConsumableLedgerExcelVO> excelRows = ExcelUtils.read(file, HcToolingConsumableLedgerExcelVO.class);
        List<ImportRow> rows = new ArrayList<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcToolingConsumableLedgerExcelVO excelRow = excelRows.get(index);
            if (isBlankImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            int rowNo = index + 2;
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            int failureCountBefore = respVO.getFailures().size();
            ImportRow row = normalizeImportRow(excelRow, rowNo, fixedProcessCode, respVO);
            if (row == null || respVO.getFailures().size() > failureCountBefore) {
                continue;
            }
            rows.add(row);
        }
        if (!respVO.getFailures().isEmpty()) {
            respVO.setFailureCount(respVO.getFailures().size());
            respVO.getMessages().add("导入校验未通过，未写入任何数据");
            return respVO;
        }
        if (rows.isEmpty()) {
            addImportFailure(respVO, "导入文件没有有效数据行");
            return respVO;
        }

        Map<String, Long> importedLedgerIdMap = new HashMap<>();
        Set<Long> createdLedgerIds = new HashSet<>();
        Set<Long> updatedLedgerIds = new HashSet<>();
        for (ImportRow row : rows) {
            HcToolingConsumableLedgerDO ledger = resolveImportedLedger(row, importedLedgerIdMap,
                    createdLedgerIds, updatedLedgerIds);
            if (row.consumePresent) {
                saveImportedConsume(row, ledger.getId(), respVO);
            }
        }
        respVO.setCreatedLedgerCount(createdLedgerIds.size());
        respVO.setUpdatedLedgerCount(updatedLedgerIds.size());
        respVO.getMessages().add(String.format("导入完成：新增领用 %d 条，更新领用 %d 条，新增消耗 %d 条，更新消耗 %d 条",
                respVO.getCreatedLedgerCount(), respVO.getUpdatedLedgerCount(),
                respVO.getCreatedConsumeCount(), respVO.getUpdatedConsumeCount()));
        return respVO;
    }

    private HcToolingConsumableLedgerDO resolveImportedLedger(ImportRow row,
                                                             Map<String, Long> importedLedgerIdMap,
                                                             Set<Long> createdLedgerIds,
                                                             Set<Long> updatedLedgerIds) {
        HcToolingConsumableLedgerDO ledger = row.ledgerId == null ? null : hcToolingConsumableLedgerMapper.selectById(row.ledgerId);
        String importKey = row.importKey();
        if (ledger == null && importedLedgerIdMap.containsKey(importKey)) {
            ledger = hcToolingConsumableLedgerMapper.selectById(importedLedgerIdMap.get(importKey));
        }
        if (ledger == null && row.ledgerId == null) {
            ledger = hcToolingConsumableLedgerMapper.selectOneByImportKey(row.consumableType, row.processCode,
                    row.model, row.batchNo, row.receiveTime, row.receiverName);
        }
        HcToolingConsumableLedgerDO saveObj = buildLedgerDO(row);
        if (ledger == null) {
            saveObj.setTenantId(TenantContextHolder.getRequiredTenantId());
            hcToolingConsumableLedgerMapper.insert(saveObj);
            syncGlueBoardStockAfterLedgerSave(null, saveObj);
            importedLedgerIdMap.put(importKey, saveObj.getId());
            createdLedgerIds.add(saveObj.getId());
            return saveObj;
        }
        ledger = getRequiredLedgerForUpdate(ledger.getId());
        assertBladeLedgerUnchanged(ledger, saveObj);
        saveObj.setId(ledger.getId());
        validateLedgerReceiveQtyEnough(saveObj.getId(), saveObj.getReceiveQty());
        hcToolingConsumableLedgerMapper.updateById(saveObj);
        syncGlueBoardStockAfterLedgerSave(ledger, saveObj);
        importedLedgerIdMap.put(importKey, ledger.getId());
        updatedLedgerIds.add(ledger.getId());
        return saveObj;
    }

    private void saveImportedConsume(ImportRow row, Long ledgerId, HcToolingConsumableLedgerImportRespVO respVO) {
        if (row.consumeId != null) assertNotBladeConsumption(getRequiredConsume(row.consumeId));
        HcToolingConsumableConsumeDO consume = buildConsumeDO(row, ledgerId);
        validateConsumeBalance(consume.getLedgerId(), consume.getConsumeQty(), row.consumeId);
        if (row.consumeId == null) {
            prepareGlueBoardConsumeLink(consume, null);
            consume.setTenantId(TenantContextHolder.getRequiredTenantId());
            hcToolingConsumableConsumeMapper.insert(consume);
            syncGlueBoardStockAfterConsumeChange(null, consume);
            respVO.setCreatedConsumeCount(respVO.getCreatedConsumeCount() + 1);
            return;
        }
        HcToolingConsumableConsumeDO oldConsume = getRequiredConsume(row.consumeId);
        consume.setId(row.consumeId);
        preserveGlueBoardConsumeLink(oldConsume, consume);
        hcToolingConsumableConsumeMapper.updateById(consume);
        syncGlueBoardStockAfterConsumeChange(oldConsume, consume);
        respVO.setUpdatedConsumeCount(respVO.getUpdatedConsumeCount() + 1);
    }

    private HcToolingConsumableLedgerDO buildLedgerDO(HcToolingConsumableLedgerSaveReqVO reqVO) {
        HcToolingConsumableLedgerDO entity = BeanUtils.toBean(reqVO, HcToolingConsumableLedgerDO.class);
        applyLedgerValues(entity, reqVO.getConsumableType(), reqVO.getConsumableTypeName(),
                reqVO.getProcessCode(), reqVO.getProcessName(), reqVO.getReceiveQty(), reqVO.getReceiveTime(),
                reqVO.getReceiverName());
        entity.setModel(trimToNull(reqVO.getModel()));
        entity.setBatchNo(requireText(reqVO.getBatchNo(), "耗材批次号不能为空"));
        entity.setErpMaterialCode(trimToNull(reqVO.getErpMaterialCode()));
        applyLedgerUnitSnapshot(entity, reqVO.getUomId(), reqVO.getUom());
        entity.setReceiverId(reqVO.getReceiverId());
        entity.setUsageStatus(LEDGER_USAGE_STATUS_ACTIVE);
        entity.setRemark(trimToNull(reqVO.getRemark()));
        validateGlueBoardLedger(entity);
        return entity;
    }

    private HcToolingConsumableConsumeDO buildConsumeDO(HcToolingConsumableConsumeSaveReqVO reqVO) {
        HcToolingConsumableLedgerDO ledger = getRequiredLedger(reqVO.getLedgerId());
        HcToolingConsumableConsumeDO entity = BeanUtils.toBean(reqVO, HcToolingConsumableConsumeDO.class);
        applyConsumeValues(entity, ledger.getConsumableType(), ledger.getConsumableTypeName(),
                ledger.getProcessCode(), ledger.getProcessName(), reqVO.getConsumeQty(), reqVO.getConsumeTime());
        entity.setModel(ledger.getModel());
        entity.setBatchNo(ledger.getBatchNo());
        entity.setPlanNo(trimToNull(reqVO.getPlanNo()));
        entity.setProductionBatchNo(trimToNull(reqVO.getProductionBatchNo()));
        applyRAndDProductSnapshot(entity, ledger, reqVO);
        entity.setRemark(trimToNull(reqVO.getRemark()));
        return entity;
    }

    /**
     * 粘胶胶板的手工消耗专用于研发样品，不允许以量产计划号或生产批次号冒充产品标识。
     * 胶板自身的型号、批号仍分别存于 model、batchNo；研发产品快照必须独立保存。
     */
    private void applyRAndDProductSnapshot(HcToolingConsumableConsumeDO entity,
                                           HcToolingConsumableLedgerDO ledger,
                                           HcToolingConsumableConsumeSaveReqVO reqVO) {
        if (isAdhesiveGlueBoardLedger(ledger)) {
            entity.setProductModelCode(requireText(reqVO.getProductModelCode(), "研发产品型号不能为空"));
            entity.setProductMaterialCode(requireText(reqVO.getProductMaterialCode(), "研发产品料号不能为空"));
            entity.setProductBatchNo(requireText(reqVO.getProductBatchNo(), "研发产品批号不能为空"));
            entity.setProductInputQty(requirePositiveQty(reqVO.getProductInputQty(),
                    isAdhesive2GlueBoardLedger(ledger) ? "粘胶2投入(pcs)" : "投入米数(m)"));
            entity.setProductOutputQty(requirePositiveQty(reqVO.getProductOutputQty(),
                    isAdhesive2GlueBoardLedger(ledger) ? "粘胶2产出(pcs)" : "产出米数(m)"));
            return;
        }
        entity.setProductModelCode(trimToNull(reqVO.getProductModelCode()));
        entity.setProductMaterialCode(trimToNull(reqVO.getProductMaterialCode()));
        entity.setProductBatchNo(trimToNull(reqVO.getProductBatchNo()));
        entity.setProductInputQty(null);
        entity.setProductOutputQty(null);
    }

    private BigDecimal requirePositiveQty(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException(fieldName + "必须大于0");
        }
        return value;
    }

    private void prepareGlueBoardConsumeLink(HcToolingConsumableConsumeDO consume, Long requestedUsageId) {
        if (!isGlueBoardConsume(consume)) {
            return;
        }
        HcToolingConsumableLedgerDO ledger = getRequiredLedger(consume.getLedgerId());
        HcAdhesiveGlueBoardStockDO stock = hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(ledger.getId());
        if (stock == null || stock.getId() == null) {
            throw invalidParamException("胶板边库库存不存在，请先维护胶板领用台账");
        }
        consume.setGlueBoardStockId(stock.getId());
        consume.setConsumeSource(CONSUME_SOURCE_LEDGER_MANUAL);
        consume.setConsumeType(nullToZero(consume.getConsumeQty()).compareTo(BigDecimal.ZERO) < 0
                ? CONSUME_TYPE_CORRECTION : CONSUME_TYPE_NORMAL);

        List<HcAdhesiveGlueBoardUsageDO> activeUsages = selectActiveProcessGlueBoardUsages(stock, ledger);
        HcAdhesiveGlueBoardUsageDO selectedUsage = null;
        if (requestedUsageId != null) {
            selectedUsage = activeUsages.stream()
                    .filter(item -> Objects.equals(item.getId(), requestedUsageId))
                    .findFirst()
                    .orElseThrow(() -> invalidParamException("指定的胶板领用记录不存在、已结束或不属于当前工序"));
        } else if (activeUsages.size() == 1) {
            selectedUsage = activeUsages.get(0);
        } else if (activeUsages.size() > 1) {
            throw invalidParamException("当前胶板存在多条有效领用记录，请先在报工看板结束多余领用后再登记消耗");
        }
        if (selectedUsage == null) {
            return;
        }
        consume.setGlueBoardUsageId(selectedUsage.getId());
        consume.setPlanOperationId(selectedUsage.getPlanOperationId());
        if (StrUtil.isBlank(consume.getPlanNo())) {
            consume.setPlanNo(trimToNull(selectedUsage.getPlanNo()));
        }
    }

    private void preserveGlueBoardConsumeLink(HcToolingConsumableConsumeDO oldConsume,
                                              HcToolingConsumableConsumeDO newConsume) {
        if (!Objects.equals(oldConsume.getLedgerId(), newConsume.getLedgerId())) {
            throw invalidParamException("消耗明细不能更换领用台账");
        }
        if (!isGlueBoardConsume(newConsume)) {
            return;
        }
        if (oldConsume.getGlueBoardStockId() == null) {
            prepareGlueBoardConsumeLink(newConsume, null);
            return;
        }
        newConsume.setGlueBoardStockId(oldConsume.getGlueBoardStockId());
        newConsume.setGlueBoardUsageId(oldConsume.getGlueBoardUsageId());
        newConsume.setPlanOperationId(oldConsume.getPlanOperationId());
        newConsume.setConsumeSource(firstNotBlank(oldConsume.getConsumeSource(), CONSUME_SOURCE_LEDGER_MANUAL));
        newConsume.setConsumeType(nullToZero(newConsume.getConsumeQty()).compareTo(BigDecimal.ZERO) < 0
                ? CONSUME_TYPE_CORRECTION : CONSUME_TYPE_NORMAL);
    }

    private PageResult<HcToolingConsumableBalanceRespVO> buildPositiveBalancePage(
            HcToolingConsumableLedgerPageReqVO reqVO) {
        List<HcToolingConsumableBalanceRespVO> allList = buildBalanceList(hcToolingConsumableLedgerMapper.selectBalanceList(reqVO))
                .stream()
                .filter(item -> nullToZero(item.getBalanceQty()).compareTo(BigDecimal.ZERO) > 0)
                .toList();
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int pageSize = reqVO.getPageSize() == null ? allList.size() : reqVO.getPageSize();
        if (pageSize <= 0) {
            return new PageResult<>(allList, (long) allList.size());
        }
        int fromIndex = Math.min((pageNo - 1) * pageSize, allList.size());
        int toIndex = Math.min(fromIndex + pageSize, allList.size());
        return new PageResult<>(allList.subList(fromIndex, toIndex), (long) allList.size());
    }

    private List<HcToolingConsumableBalanceRespVO> buildBalanceList(List<HcToolingConsumableLedgerDO> ledgers) {
        if (ledgers == null || ledgers.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> consumedQtyMap = hcToolingConsumableConsumeMapper
                .selectListByLedgerIds(ledgers.stream().map(HcToolingConsumableLedgerDO::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(HcToolingConsumableConsumeDO::getLedgerId,
                        Collectors.mapping(HcToolingConsumableConsumeDO::getConsumeQty,
                                Collectors.reducing(BigDecimal.ZERO, qty -> qty == null ? BigDecimal.ZERO : qty,
                                        BigDecimal::add))));
        Map<Long, HcAdhesiveGlueBoardStockDO> glueBoardStockMap = buildGlueBoardStockMap(ledgers);
        return ledgers.stream().map(ledger -> {
            BigDecimal receiveQty = nullToZero(ledger.getReceiveQty());
            BigDecimal consumedQty = consumedQtyMap.getOrDefault(ledger.getId(), BigDecimal.ZERO);
            BigDecimal balanceQty = receiveQty.subtract(consumedQty);
            HcAdhesiveGlueBoardStockDO glueBoardStock = glueBoardStockMap.get(ledger.getId());
            if (glueBoardStock != null) {
                balanceQty = nullToZero(glueBoardStock.getAvailableLength());
                BigDecimal stockUsedQty = nullToZero(glueBoardStock.getUsedLength())
                        .add(nullToZero(glueBoardStock.getLossLength()));
                consumedQty = stockUsedQty.compareTo(BigDecimal.ZERO) > 0
                        ? stockUsedQty : receiveQty.subtract(balanceQty).max(BigDecimal.ZERO);
                HcAdhesiveGlueBoardUsageDO processUsage = selectActiveProcessGlueBoardUsage(glueBoardStock, ledger);
                if (processUsage != null) {
                    balanceQty = nullToZero(processUsage.getAvailableLength());
                    consumedQty = nullToZero(processUsage.getAqcSampleLength())
                            .add(nullToZero(processUsage.getConsumedLength()))
                            .add(nullToZero(processUsage.getLossLength()));
                }
            }
            HcToolingConsumableBalanceRespVO respVO = new HcToolingConsumableBalanceRespVO();
            respVO.setLedgerId(ledger.getId());
            respVO.setConsumableType(ledger.getConsumableType());
            respVO.setConsumableTypeName(ledger.getConsumableTypeName());
            respVO.setProcessCode(ledger.getProcessCode());
            respVO.setProcessName(ledger.getProcessName());
            respVO.setModel(ledger.getModel());
            respVO.setBatchNo(ledger.getBatchNo());
            respVO.setErpMaterialCode(ledger.getErpMaterialCode());
            respVO.setReceiveQty(receiveQty);
            respVO.setConsumedQty(consumedQty);
            respVO.setBalanceQty(balanceQty);
            respVO.setUomId(ledger.getUomId());
            respVO.setUomCode(ledger.getUomCode());
            respVO.setUomName(ledger.getUomName());
            respVO.setUom(ledger.getUom());
            respVO.setReceiveTime(ledger.getReceiveTime());
            respVO.setReceiverId(ledger.getReceiverId());
            respVO.setReceiverName(ledger.getReceiverName());
            respVO.setRemark(ledger.getRemark());
            return respVO;
        }).toList();
    }

    /**
     * 为领用台账补充同一时点的实际消耗与剩余量，禁止由前端自行以领用量减消耗明细推算。
     */
    private void fillLedgerCurrentBalance(List<HcToolingConsumableLedgerDO> ledgers) {
        if (ledgers == null || ledgers.isEmpty()) {
            return;
        }
        Map<Long, HcToolingConsumableBalanceRespVO> balanceMap = buildBalanceList(ledgers).stream()
                .collect(Collectors.toMap(HcToolingConsumableBalanceRespVO::getLedgerId, item -> item));
        ledgers.forEach(ledger -> {
            HcToolingConsumableBalanceRespVO balance = balanceMap.get(ledger.getId());
            if (balance == null) {
                return;
            }
            ledger.setConsumedQty(balance.getConsumedQty());
            ledger.setBalanceQty(balance.getBalanceQty());
        });
    }

    private Map<Long, HcAdhesiveGlueBoardStockDO> buildGlueBoardStockMap(List<HcToolingConsumableLedgerDO> ledgers) {
        Map<Long, HcAdhesiveGlueBoardStockDO> stockMap = new HashMap<>();
        for (HcToolingConsumableLedgerDO ledger : ledgers) {
            if (!isGlueBoardLedger(ledger)) {
                continue;
            }
            HcAdhesiveGlueBoardStockDO stock = hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(ledger.getId());
            if (stock != null) {
                stockMap.put(ledger.getId(), stock);
            }
        }
        return stockMap;
    }

    private HcAdhesiveGlueBoardUsageDO selectActiveProcessGlueBoardUsage(HcAdhesiveGlueBoardStockDO stock,
                                                                         HcToolingConsumableLedgerDO ledger) {
        Map<Long, HcAdhesiveGlueBoardUsageDO> usageMap = new LinkedHashMap<>();
        for (HcAdhesiveGlueBoardUsageDO usage : selectActiveProcessGlueBoardUsages(stock, ledger)) {
            usageMap.put(usage.getId(), usage);
        }
        return selectLatestGlueBoardUsage(usageMap);
    }

    private List<HcAdhesiveGlueBoardUsageDO> selectActiveProcessGlueBoardUsages(
            HcAdhesiveGlueBoardStockDO stock, HcToolingConsumableLedgerDO ledger) {
        if (stock == null || stock.getId() == null || !isAdhesiveGlueBoardLedger(ledger)) {
            return List.of();
        }
        String processCode = StrUtil.trimToEmpty(ledger.getProcessCode()).toUpperCase();
        return hcAdhesiveGlueBoardUsageMapper.selectListByGlueBoardStockId(stock.getId()).stream()
                .filter(usage -> isActiveProcessGlueBoardUsage(usage, processCode))
                .toList();
    }

    private HcAdhesiveGlueBoardUsageDO selectLatestGlueBoardUsage(Map<Long, HcAdhesiveGlueBoardUsageDO> usageMap) {
        if (usageMap == null || usageMap.isEmpty()) {
            return null;
        }
        return usageMap.values().stream()
                .max((left, right) -> Long.compare(
                        left.getId() == null ? 0L : left.getId(),
                        right.getId() == null ? 0L : right.getId()))
                .orElse(null);
    }

    private boolean isActiveProcessGlueBoardUsage(HcAdhesiveGlueBoardUsageDO usage, String processCode) {
        if (usage == null || !LEDGER_USAGE_STATUS_ACTIVE.equalsIgnoreCase(StrUtil.blankToDefault(usage.getUsageStatus(), ""))) {
            return false;
        }
        String operationCode = StrUtil.trimToEmpty(usage.getOperationCode()).toUpperCase();
        String operationName = StrUtil.trimToEmpty(usage.getOperationName());
        if (PROCESS_ADHESIVE.equals(processCode) || PROCESS_ADHESIVE1.equals(processCode)) {
            return PROCESS_ADHESIVE1.equals(operationCode)
                    || PROCESS_ADHESIVE.equals(operationCode)
                    || "ADHESIVE".equals(operationCode)
                    || "OP-ADHESIVE1".equals(operationCode)
                    || operationName.contains("粘双面胶")
                    || operationName.contains("粘胶1")
                    || operationName.contains("粘胶一");
        }
        if (PROCESS_ADHESIVE2.equals(processCode)) {
            return PROCESS_ADHESIVE2.equals(operationCode)
                    || "OP-ADHESIVE2".equals(operationCode)
                    || operationName.contains("粘胶2")
                    || operationName.contains("粘胶二");
        }
        return false;
    }

    private List<HcToolingConsumableConsumeRespVO> buildConsumeRecordList(
            List<HcToolingConsumableConsumeDO> consumes) {
        if (consumes == null || consumes.isEmpty()) {
            return List.of();
        }
        Set<Long> ledgerIds = consumes.stream()
                .map(HcToolingConsumableConsumeDO::getLedgerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, HcToolingConsumableLedgerDO> ledgerMap = ledgerIds.isEmpty()
                ? Map.of()
                : hcToolingConsumableLedgerMapper.selectBatchIds(ledgerIds).stream()
                        .collect(Collectors.toMap(HcToolingConsumableLedgerDO::getId, item -> item, (left, right) -> left));
        return consumes.stream().map(consume -> {
            HcToolingConsumableConsumeRespVO respVO = BeanUtils.toBean(consume, HcToolingConsumableConsumeRespVO.class);
            HcToolingConsumableLedgerDO ledger = ledgerMap.get(consume.getLedgerId());
            if (ledger != null) {
                respVO.setUomId(ledger.getUomId());
                respVO.setUomCode(ledger.getUomCode());
                respVO.setUomName(ledger.getUomName());
                respVO.setUom(ledger.getUom());
            }
            respVO.setCreatorName(firstNotBlank(respVO.getCreator(), "-"));
            return respVO;
        }).toList();
    }

    private HcToolingConsumableLedgerDO buildLedgerDO(ImportRow row) {
        HcToolingConsumableLedgerDO entity = new HcToolingConsumableLedgerDO();
        entity.setConsumableType(row.consumableType);
        entity.setConsumableTypeName(row.consumableTypeName);
        entity.setProcessCode(row.processCode);
        entity.setProcessName(row.processName);
        entity.setModel(row.model);
        entity.setBatchNo(row.batchNo);
        entity.setErpMaterialCode(row.erpMaterialCode);
        entity.setReceiveQty(row.receiveQty);
        entity.setUomId(row.uomId);
        entity.setUomCode(row.uomCode);
        entity.setUomName(row.uomName);
        entity.setUom(row.uom);
        entity.setReceiveTime(row.receiveTime);
        entity.setReceiverId(row.receiverId);
        entity.setReceiverName(row.receiverName);
        entity.setUsageStatus(row.usageStatus);
        entity.setRemark(row.remark);
        validateGlueBoardLedger(entity);
        return entity;
    }

    private HcToolingConsumableConsumeDO buildConsumeDO(ImportRow row, Long ledgerId) {
        HcToolingConsumableLedgerDO ledger = getRequiredLedger(ledgerId);
        validateImportedConsumeMatchesLedger(row, ledger);
        HcToolingConsumableConsumeDO entity = new HcToolingConsumableConsumeDO();
        entity.setLedgerId(ledgerId);
        entity.setConsumableType(ledger.getConsumableType());
        entity.setConsumableTypeName(ledger.getConsumableTypeName());
        entity.setProcessCode(ledger.getProcessCode());
        entity.setProcessName(ledger.getProcessName());
        entity.setModel(ledger.getModel());
        entity.setBatchNo(ledger.getBatchNo());
        entity.setConsumeQty(row.consumeQty);
        entity.setConsumeTime(row.consumeTime);
        entity.setPlanNo(row.planNo);
        entity.setProductionBatchNo(row.productionBatchNo);
        entity.setRemark(row.remark);
        return entity;
    }

    private HcToolingConsumableLedgerExcelVO toExcelVO(HcToolingConsumableLedgerDO ledger,
                                                       HcToolingConsumableConsumeDO consume) {
        HcToolingConsumableLedgerExcelVO excelVO = new HcToolingConsumableLedgerExcelVO();
        excelVO.setLedgerId(ledger.getId());
        excelVO.setConsumableType(ledger.getConsumableType());
        excelVO.setConsumableTypeName(ledger.getConsumableTypeName());
        excelVO.setProcessCode(ledger.getProcessCode());
        excelVO.setProcessName(ledger.getProcessName());
        excelVO.setModel(ledger.getModel());
        excelVO.setBatchNo(ledger.getBatchNo());
        excelVO.setErpMaterialCode(ledger.getErpMaterialCode());
        excelVO.setReceiveQty(ledger.getReceiveQty());
        excelVO.setConsumedQty(ledger.getConsumedQty());
        excelVO.setBalanceQty(ledger.getBalanceQty());
        excelVO.setUom(firstNotBlank(ledger.getUomName(), ledger.getUomCode(), ledger.getUom()));
        excelVO.setReceiveTime(formatDateTime(ledger.getReceiveTime()));
        excelVO.setReceiverId(ledger.getReceiverId());
        excelVO.setReceiverName(ledger.getReceiverName());
        excelVO.setUsageStatus(normalizeLedgerUsageStatus(ledger.getUsageStatus()));
        excelVO.setUsageStatusName(ledgerUsageStatusName(ledger.getUsageStatus()));
        excelVO.setRemark(ledger.getRemark());
        if (consume != null) {
            excelVO.setConsumeId(consume.getId());
            excelVO.setConsumeConsumableType(consume.getConsumableType());
            excelVO.setConsumeConsumableTypeName(consume.getConsumableTypeName());
            excelVO.setConsumeProcessCode(consume.getProcessCode());
            excelVO.setConsumeProcessName(consume.getProcessName());
            excelVO.setConsumeModel(consume.getModel());
            excelVO.setConsumeBatchNo(consume.getBatchNo());
            excelVO.setConsumeQty(consume.getConsumeQty());
            excelVO.setConsumeTime(formatDateTime(consume.getConsumeTime()));
            excelVO.setPlanNo(consume.getPlanNo());
            excelVO.setProductionBatchNo(consume.getProductionBatchNo());
            excelVO.setRemark(firstNotBlank(consume.getRemark(), ledger.getRemark()));
        }
        return excelVO;
    }

    private ImportRow normalizeImportRow(HcToolingConsumableLedgerExcelVO excelRow, int rowNo, String fixedProcessCode,
                                         HcToolingConsumableLedgerImportRespVO respVO) {
        ImportRow row = new ImportRow();
        row.rowNo = rowNo;
        row.ledgerId = excelRow.getLedgerId();
        if (row.ledgerId != null && hcToolingConsumableLedgerMapper.selectById(row.ledgerId) == null) {
            addImportFailure(respVO, String.format("第%d行：领用ID不存在：%d", rowNo, row.ledgerId));
        }
        row.consumableType = normalizeConsumableType(excelRow.getConsumableType(), excelRow.getConsumableTypeName());
        if (StrUtil.isBlank(row.consumableType)) {
            addImportFailure(respVO, String.format("第%d行：耗材种类不能为空或不支持", rowNo));
        }
        row.processCode = normalizeProcessCode(excelRow.getProcessCode(), excelRow.getProcessName());
        if (StrUtil.isBlank(row.processCode)) {
            row.processCode = fixedProcessCode;
        } else if (StrUtil.isNotBlank(fixedProcessCode) && !fixedProcessCode.equals(row.processCode)) {
            addImportFailure(respVO, String.format("第%d行：工序必须为当前菜单工序：%s", rowNo, processName(fixedProcessCode)));
        }
        row.processCode = resolveImportProcess(row.consumableType, row.processCode, rowNo, "工序", respVO);
        row.consumableTypeName = typeName(row.consumableType);
        row.processName = processName(row.processCode);
        row.model = trimToNull(excelRow.getModel());
        row.batchNo = trimToNull(excelRow.getBatchNo());
        row.erpMaterialCode = trimToNull(excelRow.getErpMaterialCode());
        row.receiveQty = excelRow.getReceiveQty();
        row.uom = trimToNull(excelRow.getUom());
        applyImportUnitSnapshot(row, respVO);
        row.receiveTime = parseDateTime(excelRow.getReceiveTime(), rowNo, "领用时间", respVO);
        row.receiverId = excelRow.getReceiverId();
        row.receiverName = trimToNull(excelRow.getReceiverName());
        row.usageStatus = normalizeLedgerUsageStatus(
                firstNotBlank(excelRow.getUsageStatus(), excelRow.getUsageStatusName()));
        if (StrUtil.isBlank(row.usageStatus)) {
            addImportFailure(respVO, String.format("第%d行：使用状态不支持：%s",
                    rowNo, firstNotBlank(excelRow.getUsageStatus(), excelRow.getUsageStatusName())));
        }
        row.remark = trimToNull(excelRow.getRemark());
        validateRequiredLedger(row, respVO);

        row.consumeId = excelRow.getConsumeId();
        row.consumePresent = isConsumePresent(excelRow);
        if (!row.consumePresent) {
            return row;
        }
        HcToolingConsumableConsumeDO existedConsume = null;
        if (row.consumeId != null) {
            existedConsume = hcToolingConsumableConsumeMapper.selectById(row.consumeId);
            if (existedConsume == null) {
                addImportFailure(respVO, String.format("第%d行：消耗ID不存在：%d", rowNo, row.consumeId));
            } else if (row.ledgerId == null) {
                row.ledgerId = existedConsume.getLedgerId();
            } else if (!row.ledgerId.equals(existedConsume.getLedgerId())) {
                addImportFailure(respVO, String.format("第%d行：消耗ID不属于当前领用ID", rowNo));
            }
        }
        row.consumeConsumableType = normalizeConsumableType(
                firstNotBlank(excelRow.getConsumeConsumableType(), row.consumableType),
                firstNotBlank(excelRow.getConsumeConsumableTypeName(), row.consumableTypeName));
        row.consumeProcessCode = normalizeProcessCode(
                firstNotBlank(excelRow.getConsumeProcessCode(), row.processCode),
                firstNotBlank(excelRow.getConsumeProcessName(), row.processName));
        if (StrUtil.isBlank(row.consumeProcessCode)) {
            row.consumeProcessCode = fixedProcessCode;
        } else if (StrUtil.isNotBlank(fixedProcessCode) && !fixedProcessCode.equals(row.consumeProcessCode)) {
            addImportFailure(respVO, String.format("第%d行：消耗工序必须为当前菜单工序：%s", rowNo, processName(fixedProcessCode)));
        }
        row.consumeProcessCode = resolveImportProcess(row.consumeConsumableType, row.consumeProcessCode,
                rowNo, "消耗工序", respVO);
        row.consumeConsumableTypeName = typeName(row.consumeConsumableType);
        row.consumeProcessName = processName(row.consumeProcessCode);
        row.consumeModel = firstNotBlank(trimToNull(excelRow.getConsumeModel()), row.model);
        row.consumeBatchNo = firstNotBlank(trimToNull(excelRow.getConsumeBatchNo()), row.batchNo);
        row.consumeQty = excelRow.getConsumeQty();
        row.consumeTime = parseDateTime(excelRow.getConsumeTime(), rowNo, "消耗时间", respVO);
        row.planNo = trimToNull(excelRow.getPlanNo());
        row.productionBatchNo = trimToNull(excelRow.getProductionBatchNo());
        validateRequiredConsume(row, respVO);
        return row;
    }

    private void validateRequiredLedger(ImportRow row, HcToolingConsumableLedgerImportRespVO respVO) {
        if (StrUtil.isBlank(row.batchNo)) {
            addImportFailure(respVO, String.format("第%d行：耗材批次号不能为空", row.rowNo));
        }
        if (TYPE_GLUE_BOARD.equals(row.consumableType) && StrUtil.isBlank(row.model)) {
            addImportFailure(respVO, String.format("第%d行：胶板型号不能为空", row.rowNo));
        }
        if (row.receiveQty == null) {
            addImportFailure(respVO, String.format("第%d行：领用量不能为空", row.rowNo));
        } else if (row.receiveQty.compareTo(BigDecimal.ZERO) < 0) {
            addImportFailure(respVO, String.format("第%d行：领用量不能为负数", row.rowNo));
        } else if (TYPE_GLUE_BOARD.equals(row.consumableType) && row.receiveQty.compareTo(BigDecimal.ZERO) <= 0) {
            addImportFailure(respVO, String.format("第%d行：胶板领用量必须大于0", row.rowNo));
        }
        if (row.receiveTime == null) {
            addImportFailure(respVO, String.format("第%d行：领用时间不能为空", row.rowNo));
        }
        if (StrUtil.isBlank(row.receiverName)) {
            addImportFailure(respVO, String.format("第%d行：领用人不能为空", row.rowNo));
        }
        if (StrUtil.isBlank(row.uom)) {
            addImportFailure(respVO, String.format("第%d行：计量单位不能为空", row.rowNo));
        }
    }

    private void applyLedgerUnitSnapshot(HcToolingConsumableLedgerDO entity, Long uomId, String fallbackUom) {
        if (uomId == null) {
            throw invalidParamException("计量单位不能为空");
        }
        UnitDO unit = unitMapper.selectById(uomId);
        if (unit == null) {
            throw invalidParamException("计量单位不存在");
        }
        entity.setUomId(unit.getId());
        entity.setUomCode(trimToNull(unit.getCode()));
        entity.setUomName(trimToNull(unit.getName()));
        entity.setUom(firstNotBlank(unit.getName(), unit.getCode(), fallbackUom));
    }

    private void applyDefaultUnitSnapshot(HcToolingProcessConsumableDO entity, Long uomId) {
        if (uomId == null) {
            entity.setDefaultUomId(null);
            entity.setDefaultUomCode(null);
            entity.setDefaultUomName(null);
            return;
        }
        UnitDO unit = unitMapper.selectById(uomId);
        if (unit == null) {
            throw invalidParamException("默认计量单位不存在");
        }
        entity.setDefaultUomId(unit.getId());
        entity.setDefaultUomCode(trimToNull(unit.getCode()));
        entity.setDefaultUomName(trimToNull(unit.getName()));
    }

    private void applyImportUnitSnapshot(ImportRow row, HcToolingConsumableLedgerImportRespVO respVO) {
        if (StrUtil.isBlank(row.uom)) {
            return;
        }
        UnitDO unit = resolveUnitByText(row.uom);
        if (unit == null) {
            addImportFailure(respVO, String.format("第%d行：计量单位不存在：%s", row.rowNo, row.uom));
            return;
        }
        row.uomId = unit.getId();
        row.uomCode = trimToNull(unit.getCode());
        row.uomName = trimToNull(unit.getName());
        row.uom = firstNotBlank(unit.getName(), unit.getCode(), row.uom);
    }

    private UnitDO resolveUnitByText(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        addUnitCandidate(candidates, text);
        for (String part : text.split("[/／]")) {
            addUnitCandidate(candidates, part);
        }
        for (String candidate : candidates) {
            UnitDO unit = unitMapper.selectOneByCodeOrName(candidate);
            if (unit != null) {
                return unit;
            }
        }
        return null;
    }

    private void addUnitCandidate(List<String> candidates, String value) {
        String text = trimToNull(value);
        if (text != null && !candidates.contains(text)) {
            candidates.add(text);
        }
    }

    private HcToolingProcessConsumableDO buildProcessConsumableDO(HcToolingProcessConsumableSaveReqVO reqVO) {
        HcToolingProcessConsumableDO entity = BeanUtils.toBean(reqVO, HcToolingProcessConsumableDO.class);
        String type = normalizeConsumableType(reqVO.getConsumableType(), reqVO.getConsumableTypeName());
        String process = normalizeProcessCode(reqVO.getProcessCode(), reqVO.getProcessName());
        validateTypeAndProcess(type, process);
        validateStatus(reqVO.getStatus());
        entity.setConsumableType(type);
        entity.setConsumableTypeName(typeName(type));
        entity.setProcessCode(process);
        entity.setProcessName(processName(process));
        entity.setDefaultErpMaterialCode(trimToNull(reqVO.getDefaultErpMaterialCode()));
        entity.setDefaultBatchNo(trimToNull(reqVO.getDefaultBatchNo()));
        applyDefaultUnitSnapshot(entity, reqVO.getDefaultUomId());
        entity.setStatus(reqVO.getStatus());
        entity.setRemark(trimToNull(reqVO.getRemark()));
        return entity;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw invalidParamException("状态不支持");
        }
    }

    private void validateRequiredConsume(ImportRow row, HcToolingConsumableLedgerImportRespVO respVO) {
        if (StrUtil.isBlank(row.consumeBatchNo)) {
            addImportFailure(respVO, String.format("第%d行：消耗耗材批次号不能为空", row.rowNo));
        }
        if (row.consumeQty == null) {
            addImportFailure(respVO, String.format("第%d行：消耗量不能为空", row.rowNo));
        }
        if (row.consumeTime == null) {
            addImportFailure(respVO, String.format("第%d行：消耗时间不能为空", row.rowNo));
        }
    }

    private void applyLedgerValues(HcToolingConsumableLedgerDO entity, String typeText, String typeNameText,
                                   String processText, String processNameText, BigDecimal qty,
                                   LocalDateTime receiveTime, String receiverName) {
        String type = normalizeConsumableType(typeText, typeNameText);
        String process = normalizeProcessCode(processText, processNameText);
        validateTypeAndProcess(type, process);
        validateQty(qty, "领用量");
        if (receiveTime == null) {
            throw invalidParamException("领用时间不能为空");
        }
        entity.setConsumableType(type);
        entity.setConsumableTypeName(typeName(type));
        entity.setProcessCode(process);
        entity.setProcessName(processName(process));
        entity.setReceiveQty(qty);
        entity.setReceiveTime(receiveTime);
        entity.setReceiverName(requireText(receiverName, "领用人不能为空"));
    }

    private void applyConsumeValues(HcToolingConsumableConsumeDO entity, String typeText, String typeNameText,
                                    String processText, String processNameText, BigDecimal qty,
                                    LocalDateTime consumeTime) {
        String type = normalizeConsumableType(typeText, typeNameText);
        String process = normalizeProcessCode(processText, processNameText);
        validateTypeAndProcess(type, process);
        validateConsumeQty(qty);
        if (consumeTime == null) {
            throw invalidParamException("消耗时间不能为空");
        }
        entity.setConsumableType(type);
        entity.setConsumableTypeName(typeName(type));
        entity.setProcessCode(process);
        entity.setProcessName(processName(process));
        entity.setConsumeQty(qty);
        entity.setConsumeTime(consumeTime);
    }

    private String resolveImportProcess(String type, String process, int rowNo, String fieldName,
                                        HcToolingConsumableLedgerImportRespVO respVO) {
        if (StrUtil.isBlank(type)) {
            return process;
        }
        if (StrUtil.isBlank(process)) {
            Set<String> allowed = ALLOWED_PROCESS_BY_TYPE.get(type);
            if (allowed != null && allowed.size() == 1) {
                return allowed.iterator().next();
            }
            addImportFailure(respVO, String.format("第%d行：%s不能为空，%s支持多个工序，请明确选择",
                    rowNo, fieldName, typeName(type)));
            return null;
        }
        if (!isProcessAllowed(type, process)) {
            addImportFailure(respVO, String.format("第%d行：%s与耗材种类不匹配：%s/%s",
                    rowNo, fieldName, typeName(type), processName(process)));
        }
        return process;
    }

    private void validateTypeAndProcess(String type, String process) {
        if (StrUtil.isBlank(type) || !CONSUMABLE_TYPE_NAMES.containsKey(type)) {
            throw invalidParamException("耗材种类不支持");
        }
        if (StrUtil.isBlank(process) || !PROCESS_NAMES.containsKey(process)) {
            throw invalidParamException("工序不支持");
        }
        if (!isProcessAllowed(type, process)) {
            throw invalidParamException("耗材种类与工序不匹配：" + typeName(type) + "/" + processName(process));
        }
    }

    private boolean isProcessAllowed(String type, String process) {
        Set<String> allowed = ALLOWED_PROCESS_BY_TYPE.get(type);
        return allowed != null && allowed.contains(process);
    }

    private String normalizeConsumableType(String typeCode, String typeName) {
        String text = firstNotBlank(typeCode, typeName);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String normalized = text.trim().toUpperCase();
        if (CONSUMABLE_TYPE_NAMES.containsKey(normalized)) {
            return normalized;
        }
        for (Map.Entry<String, String> entry : CONSUMABLE_TYPE_NAMES.entrySet()) {
            if (entry.getValue().equals(text.trim())) {
                return entry.getKey();
            }
        }
        if ("PRESS_ROLL".equals(normalized) || "ROLLER".equals(normalized) || "压辊".equals(text.trim())) {
            return TYPE_PRESS_ROLLER;
        }
        if ("CUTTING_FELT".equals(normalized)) {
            return TYPE_FELT;
        }
        if ("CUTTING_BLADE".equals(normalized)) {
            return TYPE_BLADE;
        }
        if ("INNER_PACK_TAPE".equals(normalized) || "PACKAGING_TAPE".equals(normalized)
                || "INNER_PACKAGING_TAPE".equals(normalized) || "内包带".equals(text.trim())
                || "内包装带".equals(text.trim())) {
            return TYPE_PACKAGING_BAG;
        }
        return null;
    }

    private String normalizeProcessCode(String processCode, String processName) {
        String text = firstNotBlank(processCode, processName);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String normalized = text.trim().toUpperCase();
        if ("ADHESIVE".equals(normalized) || "ADHESIVE1".equals(normalized)
                || "ADHESIVE_1".equals(normalized) || "粘胶1".equals(text.trim())) {
            return PROCESS_ADHESIVE1;
        }
        if ("ADHESIVE2".equals(normalized) || "ADHESIVE_2".equals(normalized)
                || "粘胶2".equals(text.trim())) {
            return PROCESS_ADHESIVE2;
        }
        if (PROCESS_NAMES.containsKey(normalized)) {
            return normalized;
        }
        for (Map.Entry<String, String> entry : PROCESS_NAMES.entrySet()) {
            if (entry.getValue().equals(text.trim())) {
                return entry.getKey();
            }
        }
        if ("GRINDING".equals(normalized) || "ROUGH".equals(normalized)) {
            return PROCESS_ROUGH_GRINDING;
        }
        if ("PACKAGE".equals(normalized) || "包装".equals(text.trim())) {
            return PROCESS_PACKAGING;
        }
        return null;
    }

    private boolean isBlankImportRow(HcToolingConsumableLedgerExcelVO row) {
        if (row == null) {
            return true;
        }
        return row.getLedgerId() == null
                && StrUtil.isBlank(row.getConsumableType())
                && StrUtil.isBlank(row.getConsumableTypeName())
                && StrUtil.isBlank(row.getProcessCode())
                && StrUtil.isBlank(row.getProcessName())
                && StrUtil.isBlank(row.getBatchNo())
                && row.getReceiveQty() == null
                && StrUtil.isBlank(row.getReceiveTime())
                && StrUtil.isBlank(row.getReceiverName())
                && StrUtil.isBlank(row.getUsageStatus())
                && StrUtil.isBlank(row.getUsageStatusName())
                && row.getConsumeId() == null
                && row.getConsumeQty() == null
                && StrUtil.isBlank(row.getConsumeTime())
                && StrUtil.isBlank(row.getPlanNo())
                && StrUtil.isBlank(row.getProductionBatchNo());
    }

    private boolean isConsumePresent(HcToolingConsumableLedgerExcelVO row) {
        return row.getConsumeId() != null
                || StrUtil.isNotBlank(row.getConsumeConsumableType())
                || StrUtil.isNotBlank(row.getConsumeConsumableTypeName())
                || StrUtil.isNotBlank(row.getConsumeProcessCode())
                || StrUtil.isNotBlank(row.getConsumeProcessName())
                || StrUtil.isNotBlank(row.getConsumeModel())
                || StrUtil.isNotBlank(row.getConsumeBatchNo())
                || row.getConsumeQty() != null
                || StrUtil.isNotBlank(row.getConsumeTime())
                || StrUtil.isNotBlank(row.getPlanNo())
                || StrUtil.isNotBlank(row.getProductionBatchNo());
    }

    private LocalDateTime parseDateTime(String value, int rowNo, String fieldName,
                                        HcToolingConsumableLedgerImportRespVO respVO) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        String normalized = text.replace('/', '-').replace('T', ' ');
        try {
            if (normalized.length() == 10) {
                return LocalDate.parse(normalized).atStartOfDay();
            }
            if (normalized.length() == 16) {
                return LocalDateTime.parse(normalized, DATE_TIME_MINUTE_FORMATTER);
            }
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            addImportFailure(respVO, String.format("第%d行：%s格式不正确，应为 yyyy-MM-dd HH:mm:ss", rowNo, fieldName));
            return null;
        }
    }

    private void validateLedgerExists(Long id) {
        if (id == null || hcToolingConsumableLedgerMapper.selectById(id) == null) {
            throw invalidParamException("边库耗材领用台账不存在");
        }
    }

    private HcToolingConsumableLedgerDO getRequiredLedger(Long id) {
        HcToolingConsumableLedgerDO ledger = id == null ? null : hcToolingConsumableLedgerMapper.selectById(id);
        if (ledger == null) {
            throw invalidParamException("边库耗材领用台账不存在");
        }
        return ledger;
    }

    private HcToolingConsumableLedgerDO getRequiredLedgerForUpdate(Long id) {
        HcToolingConsumableLedgerDO ledger = id == null ? null : hcToolingConsumableLedgerMapper.selectByIdForUpdate(id);
        if (ledger == null) {
            throw invalidParamException("边库耗材领用台账不存在");
        }
        return ledger;
    }

    private void validateConsumeExists(Long id) {
        if (id == null || hcToolingConsumableConsumeMapper.selectById(id) == null) {
            throw invalidParamException("边库耗材消耗明细不存在");
        }
    }

    private HcToolingConsumableConsumeDO getRequiredConsume(Long id) {
        HcToolingConsumableConsumeDO consume = id == null ? null : hcToolingConsumableConsumeMapper.selectById(id);
        if (consume == null) {
            throw invalidParamException("边库耗材消耗明细不存在");
        }
        return consume;
    }

    private void validateLedgerReceiveQtyEnough(Long ledgerId, BigDecimal receiveQty) {
        BigDecimal consumedQty = hcToolingConsumableConsumeMapper.sumConsumeQtyByLedgerId(ledgerId, null);
        if (nullToZero(receiveQty).compareTo(consumedQty) < 0) {
            throw invalidParamException("领用量不能小于已消耗量，当前已消耗：" + consumedQty);
        }
    }

    private void validateConsumeBalance(Long ledgerId, BigDecimal consumeQty, Long consumeId) {
        HcToolingConsumableLedgerDO ledger = getRequiredLedgerForUpdate(ledgerId);
        requireLedgerActive(ledger, "登记消耗");
        BigDecimal existedConsumedQty = TYPE_BLADE.equals(ledger.getConsumableType())
                ? hcToolingConsumableConsumeMapper.selectListForUpdate(ledgerId).stream()
                    .filter(row -> !Objects.equals(row.getId(), consumeId))
                    .map(HcToolingConsumableConsumeDO::getConsumeQty).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : hcToolingConsumableConsumeMapper.sumConsumeQtyByLedgerId(ledgerId, consumeId);
        BigDecimal nextConsumedQty = existedConsumedQty.add(nullToZero(consumeQty));
        BigDecimal receiveQty = nullToZero(ledger.getReceiveQty());
        if (nextConsumedQty.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException(String.format("修订后累计消耗不能小于0：已消耗 %s，本次修订 %s",
                    existedConsumedQty, nullToZero(consumeQty)));
        }
        if (nextConsumedQty.compareTo(receiveQty) > 0) {
            throw invalidParamException(String.format("消耗量超过边库余额：领用量 %s，已消耗 %s，本次消耗 %s",
                    receiveQty, existedConsumedQty, nullToZero(consumeQty)));
        }
    }

    private void syncGlueBoardStockAfterLedgerSave(HcToolingConsumableLedgerDO oldLedger,
                                                   HcToolingConsumableLedgerDO ledger) {
        if (ledger == null || ledger.getId() == null) {
            return;
        }
        boolean oldGlueBoard = isGlueBoardLedger(oldLedger);
        boolean newGlueBoard = isGlueBoardLedger(ledger);
        if (!oldGlueBoard && !newGlueBoard) {
            return;
        }
        BigDecimal consumedQty = hcToolingConsumableConsumeMapper.sumConsumeQtyByLedgerId(ledger.getId(), null);
        HcAdhesiveGlueBoardStockDO stock = hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(ledger.getId());
        if (oldGlueBoard && !newGlueBoard) {
            if (stock != null && hasExternalGlueBoardStockMovement(stock, ledger.getId())) {
                throw invalidParamException("胶板库存已被粘胶报工或送检使用，不能改为非胶板耗材");
            }
            if (stock != null) {
                hcAdhesiveGlueBoardStockMapper.deleteById(stock.getId());
            }
            return;
        }
        if (!oldGlueBoard && consumedQty.compareTo(BigDecimal.ZERO) > 0) {
            throw invalidParamException("当前台账已有消耗记录，不能改为胶板耗材");
        }
        validateGlueBoardLedger(ledger);
        if (stock == null) {
            ensureGlueBoardBatchAvailable(ledger, null);
            hcAdhesiveGlueBoardStockMapper.insert(buildGlueBoardStockFromLedger(ledger, consumedQty));
            return;
        }
        boolean externalMovement = hasExternalGlueBoardStockMovement(stock, ledger.getId());
        if (externalMovement && hasGlueBoardStockCoreChanged(stock, ledger)) {
            throw invalidParamException("胶板库存已被粘胶报工或送检使用，不能修改型号、批号或领用量");
        }
        if (hasGlueBoardIdentityChanged(oldLedger, ledger)
                && consumedQty.compareTo(BigDecimal.ZERO) > 0) {
            throw invalidParamException("当前胶板台账已有消耗记录，不能修改工序、型号或批次号");
        }
        if (!sameText(stock.getGlueBoardBatchNo(), ledger.getBatchNo())) {
            ensureGlueBoardBatchAvailable(ledger, stock.getId());
        }
        if (externalMovement) {
            hcAdhesiveGlueBoardStockMapper.updateById(buildGlueBoardStockSnapshotUpdate(stock.getId(), ledger));
            return;
        }
        hcAdhesiveGlueBoardStockMapper.updateById(buildGlueBoardStockBalanceUpdate(stock.getId(), ledger, consumedQty));
    }

    private void deleteLinkedGlueBoardStockBeforeLedgerDelete(HcToolingConsumableLedgerDO ledger) {
        if (!isGlueBoardLedger(ledger)) {
            return;
        }
        HcAdhesiveGlueBoardStockDO stock = hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(ledger.getId());
        if (stock == null) {
            return;
        }
        if (hasExternalGlueBoardStockMovement(stock, ledger.getId())) {
            throw invalidParamException("胶板库存已被粘胶报工或送检使用，不能删除领用台账");
        }
        hcAdhesiveGlueBoardStockMapper.deleteById(stock.getId());
    }

    private void syncGlueBoardStockAfterConsumeChange(HcToolingConsumableConsumeDO oldConsume,
                                                      HcToolingConsumableConsumeDO newConsume) {
        if (oldConsume != null && isGlueBoardConsume(oldConsume)) {
            applyGlueBoardConsumeDelta(oldConsume, nullToZero(oldConsume.getConsumeQty()).negate());
        }
        if (newConsume != null && isGlueBoardConsume(newConsume)) {
            applyGlueBoardConsumeDelta(newConsume, nullToZero(newConsume.getConsumeQty()));
        }
    }

    /** 完成时盘点结算；保留报工明细，以原系统余量和结算备注追溯差额。 */
    private void settleAdhesiveGlueBoardLedger(HcToolingConsumableLedgerDO ledger,
                                              HcToolingConsumableLedgerMarkUsedUpReqVO reqVO) {
        BigDecimal remaining = reqVO.getBalanceQty();
        if (remaining == null) {
            throw invalidParamException("请输入当前剩余量，请刷新页面后重试");
        }
        validateQty(remaining, "当前剩余量");
        if (remaining.scale() > 3) {
            throw invalidParamException("当前剩余量最多保留三位小数");
        }
        String authName = requireText(reqVO.getUsedUpAuthUserName(), "认证人不能为空");
        HcAdhesiveGlueBoardStockDO linked = hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(ledger.getId());
        HcAdhesiveGlueBoardStockDO stock = linked == null ? null
                : hcAdhesiveGlueBoardStockMapper.selectByIdForUpdate(linked.getId());
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
            throw invalidParamException("胶板关联库存不存在，不能标记完成");
        }
        List<HcAdhesiveGlueBoardUsageDO> usages = selectActiveProcessGlueBoardUsages(stock, ledger);
        if (usages.size() > 1) {
            throw invalidParamException("胶板存在多条在用记录，请先核对后再标记完成");
        }
        HcAdhesiveGlueBoardUsageDO usage = usages.isEmpty() ? null : usages.get(0);
        BigDecimal systemRemaining = usage == null ? nullToZero(stock.getAvailableLength())
                : nullToZero(usage.getAvailableLength());
        BigDecimal stockUsed = nullToZero(stock.getReceiveLength())
                .subtract(nullToZero(stock.getLossLength())).subtract(remaining);
        BigDecimal usageConsumed = usage == null ? BigDecimal.ZERO : nullToZero(usage.getReceiveLength())
                .subtract(nullToZero(usage.getAqcSampleLength()))
                .subtract(nullToZero(usage.getLossLength())).subtract(remaining);
        if (remaining.compareTo(nullToZero(ledger.getReceiveQty())) > 0
                || stockUsed.signum() < 0 || usageConsumed.signum() < 0) {
            throw invalidParamException("当前剩余量不能超过扣除取样及损耗后的领用量");
        }
        if (usage != null) {
            hcAdhesiveGlueBoardUsageMapper.updateById(HcAdhesiveGlueBoardUsageDO.builder()
                    .id(usage.getId()).consumedLength(usageConsumed)
                    .availableLength(remaining)
                    .availableStartPosition(nullToZero(usage.getReceiveStartPosition())
                            .add(nullToZero(usage.getReceiveLength())).subtract(remaining))
                    .usageStatus(LEDGER_USAGE_STATUS_USED_UP).build());
        }
        Map<String, Object> extra = StrUtil.isBlank(stock.getExtraJson()) ? new LinkedHashMap<>()
                : new LinkedHashMap<>(JsonUtils.parseObject(stock.getExtraJson(), Map.class));
        extra.put("ledgerBalanceSettled", true);
        hcAdhesiveGlueBoardStockMapper.updateById(HcAdhesiveGlueBoardStockDO.builder()
                .id(stock.getId()).availableLength(remaining).usedLength(stockUsed)
                .availableStartPosition(nullToZero(stock.getReceiveStartPosition())
                        .add(nullToZero(stock.getReceiveLength())).subtract(remaining))
                .stockStatus(GLUE_BOARD_STOCK_USED_UP).extraJson(JsonUtils.toJsonString(extra)).build());
        HcToolingConsumableLedgerDO update = new HcToolingConsumableLedgerDO();
        update.setId(ledger.getId());
        update.setUsageStatus(LEDGER_USAGE_STATUS_USED_UP);
        update.setUsedUpRemainQty(systemRemaining);
        update.setUsedUpActualDate(LocalDate.now()); // 完成结算日期由服务端生成
        update.setUsedUpRemark("[BALANCE_SETTLEMENT_V1] 系统余量=" + systemRemaining.toPlainString()
                + "；当前剩余量=" + remaining.toPlainString()
                + "；消耗调整量=" + systemRemaining.subtract(remaining).toPlainString());
        update.setUsedUpAuthUserId(reqVO.getUsedUpAuthUserId());
        update.setUsedUpAuthUserName(authName);
        update.setUsedUpAuthTime(LocalDateTime.now());
        hcToolingConsumableLedgerMapper.updateById(update);
    }

    private void markAdhesiveGlueBoardLedgerUsedUp(HcToolingConsumableLedgerDO ledger) {
        if (!isAdhesiveGlueBoardLedger(ledger)) {
            return;
        }
        HcAdhesiveGlueBoardStockDO stock = hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(ledger.getId());
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
            return;
        }
        HcAdhesiveGlueBoardUsageDO activeUsage = selectActiveProcessGlueBoardUsage(stock, ledger);
        if (activeUsage != null) {
            markAdhesiveGlueBoardUsageUsedUp(activeUsage);
        }
        BigDecimal receiveStart = nullToZero(stock.getReceiveStartPosition());
        BigDecimal receiveLength = nullToZero(stock.getReceiveLength());
        BigDecimal nextAvailableStart = receiveStart.add(receiveLength);
        BigDecimal nextUsedLength = receiveLength.subtract(nullToZero(stock.getLossLength())).max(BigDecimal.ZERO);
        hcAdhesiveGlueBoardStockMapper.updateById(HcAdhesiveGlueBoardStockDO.builder()
                .id(stock.getId())
                .availableStartPosition(nextAvailableStart)
                .availableLength(BigDecimal.ZERO)
                .usedLength(nextUsedLength)
                .availableCount(BigDecimal.ZERO)
                .stockStatus(GLUE_BOARD_STOCK_USED_UP)
                .build());
    }

    /**
     * 退库登记只关闭边库台账，不回补中心仓库存；胶板库存保留其当时的实际余量以供审计，
     * 并改为 RETURNED，禁止再被粘胶报工选择。
     */
    private void markAdhesiveGlueBoardLedgerReturned(HcToolingConsumableLedgerDO ledger) {
        if (!isGlueBoardLedger(ledger)) {
            return;
        }
        HcAdhesiveGlueBoardStockDO linkedStock = hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(ledger.getId());
        HcAdhesiveGlueBoardStockDO stock = linkedStock == null || linkedStock.getId() == null
                ? null : hcAdhesiveGlueBoardStockMapper.selectByIdForUpdate(linkedStock.getId());
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
            return;
        }
        if (hcAdhesiveGlueBoardUsageMapper.existsActiveByGlueBoardStockId(stock.getId())) {
            throw invalidParamException("胶板已被粘胶报工领用，请先在报工看板退回释放后再执行边库退库");
        }
        hcAdhesiveGlueBoardStockMapper.updateById(HcAdhesiveGlueBoardStockDO.builder()
                .id(stock.getId())
                .stockStatus(GLUE_BOARD_STOCK_RETURNED)
                .build());
    }

    private BigDecimal calculateReturnQty(HcToolingConsumableLedgerDO ledger) {
        if (isGlueBoardLedger(ledger)) {
            HcAdhesiveGlueBoardStockDO linkedStock = hcAdhesiveGlueBoardStockMapper
                    .selectByToolingLedgerId(ledger.getId());
            HcAdhesiveGlueBoardStockDO stock = linkedStock == null || linkedStock.getId() == null
                    ? null : hcAdhesiveGlueBoardStockMapper.selectByIdForUpdate(linkedStock.getId());
            if (stock != null) {
                if (hcAdhesiveGlueBoardUsageMapper.existsActiveByGlueBoardStockId(stock.getId())) {
                    throw invalidParamException("胶板已被粘胶报工领用，请先在报工看板退回释放后再执行边库退库");
                }
                return nullToZero(stock.getAvailableLength());
            }
        }
        BigDecimal consumedQty = hcToolingConsumableConsumeMapper.sumConsumeQtyByLedgerId(ledger.getId(), null);
        return nullToZero(ledger.getReceiveQty()).subtract(consumedQty).max(BigDecimal.ZERO);
    }

    private void markAdhesiveGlueBoardUsageUsedUp(HcAdhesiveGlueBoardUsageDO usage) {
        BigDecimal receiveStart = nullToZero(usage.getReceiveStartPosition());
        BigDecimal receiveLength = nullToZero(usage.getReceiveLength());
        BigDecimal aqcLength = nullToZero(usage.getAqcSampleLength());
        BigDecimal lossLength = nullToZero(usage.getLossLength());
        BigDecimal consumedLength = receiveLength.subtract(aqcLength).subtract(lossLength).max(BigDecimal.ZERO);
        BigDecimal availableStart = receiveStart.add(aqcLength).add(consumedLength).add(lossLength);
        BigDecimal receiveCount = nullToZero(usage.getReceiveCount());
        BigDecimal lossCount = nullToZero(usage.getLossCount());
        BigDecimal consumedCount = receiveCount.subtract(lossCount).max(BigDecimal.ZERO);
        hcAdhesiveGlueBoardUsageMapper.updateById(HcAdhesiveGlueBoardUsageDO.builder()
                .id(usage.getId())
                .consumedLength(consumedLength)
                .consumedCount(consumedCount)
                .availableStartPosition(availableStart)
                .availableLength(BigDecimal.ZERO)
                .availableCount(BigDecimal.ZERO)
                .returnedStartPosition(availableStart)
                .returnedLength(BigDecimal.ZERO)
                .returnedCount(BigDecimal.ZERO)
                .usageStatus(LEDGER_USAGE_STATUS_USED_UP)
                .build());
    }

    private void applyGlueBoardConsumeDelta(HcToolingConsumableConsumeDO consume, BigDecimal delta) {
        if (consume == null || delta == null || delta.compareTo(BigDecimal.ZERO) == 0
                || !isGlueBoardConsume(consume)) {
            return;
        }
        HcAdhesiveGlueBoardStockDO linkedStock = consume.getGlueBoardStockId() == null
                ? hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(consume.getLedgerId())
                : hcAdhesiveGlueBoardStockMapper.selectById(consume.getGlueBoardStockId());
        HcAdhesiveGlueBoardStockDO stock = linkedStock == null || linkedStock.getId() == null
                ? null : hcAdhesiveGlueBoardStockMapper.selectByIdForUpdate(linkedStock.getId());
        if (stock == null) {
            throw invalidParamException("胶板边库库存不存在，请先维护胶板领用台账");
        }
        if (consume.getGlueBoardUsageId() != null) {
            applyGlueBoardUsageConsumeDelta(stock, consume, delta);
            return;
        }
        if (hcAdhesiveGlueBoardUsageMapper.existsActiveByGlueBoardStockId(stock.getId())) {
            throw invalidParamException("该历史消耗未绑定胶板领用记录，胶板当前已被报工占用，不能直接修改库存");
        }
        BigDecimal availableStart = nullToZero(stock.getAvailableStartPosition());
        BigDecimal availableLength = nullToZero(stock.getAvailableLength());
        BigDecimal usedLength = nullToZero(stock.getUsedLength());
        BigDecimal receiveStart = nullToZero(stock.getReceiveStartPosition());
        BigDecimal receiveLength = nullToZero(stock.getReceiveLength());
        BigDecimal nextAvailableStart;
        BigDecimal nextAvailableLength;
        BigDecimal nextUsedLength;
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            if (availableLength.compareTo(delta) < 0) {
                throw invalidParamException(String.format("胶板边库余量不足，当前可用 %s，本次消耗 %s",
                        availableLength, delta));
            }
            nextAvailableStart = availableStart.add(delta);
            nextAvailableLength = availableLength.subtract(delta);
            nextUsedLength = usedLength.add(delta);
        } else {
            BigDecimal returnQty = delta.abs();
            nextAvailableStart = availableStart.subtract(returnQty).max(receiveStart);
            BigDecimal receiveEnd = receiveStart.add(receiveLength);
            BigDecimal maxAvailableLength = receiveEnd.subtract(nextAvailableStart).max(BigDecimal.ZERO);
            nextAvailableLength = availableLength.add(returnQty).min(maxAvailableLength);
            nextUsedLength = usedLength.subtract(returnQty).max(BigDecimal.ZERO);
        }
        hcAdhesiveGlueBoardStockMapper.updateById(HcAdhesiveGlueBoardStockDO.builder()
                .id(stock.getId())
                .availableStartPosition(nextAvailableStart)
                .availableLength(nextAvailableLength)
                .usedLength(nextUsedLength)
                .stockStatus(resolveGlueBoardStockStatus(stock.getId(), nextAvailableLength))
                .build());
    }

    private void applyGlueBoardUsageConsumeDelta(HcAdhesiveGlueBoardStockDO stock,
                                                 HcToolingConsumableConsumeDO consume,
                                                 BigDecimal delta) {
        HcAdhesiveGlueBoardUsageDO usage = hcAdhesiveGlueBoardUsageMapper
                .selectByIdForUpdate(consume.getGlueBoardUsageId());
        if (usage == null || !Objects.equals(usage.getGlueBoardStockId(), stock.getId())) {
            throw invalidParamException("胶板消耗关联的报工领用记录不存在或库存关系不一致");
        }
        if (consume.getPlanOperationId() != null
                && !Objects.equals(consume.getPlanOperationId(), usage.getPlanOperationId())) {
            throw invalidParamException("胶板消耗关联的计划工序与报工领用记录不一致");
        }
        if (delta.compareTo(BigDecimal.ZERO) > 0
                && !LEDGER_USAGE_STATUS_ACTIVE.equalsIgnoreCase(StrUtil.blankToDefault(usage.getUsageStatus(), ""))) {
            throw invalidParamException("胶板报工领用已结束，不能继续登记消耗");
        }
        BigDecimal availableLength = nullToZero(usage.getAvailableLength());
        BigDecimal consumedLength = nullToZero(usage.getConsumedLength());
        BigDecimal nextAvailableLength = availableLength.subtract(delta);
        BigDecimal nextConsumedLength = consumedLength.add(delta);
        if (nextAvailableLength.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException(String.format("胶板报工领用余量不足，当前可用 %s，本次消耗 %s",
                    availableLength, delta));
        }
        if (nextConsumedLength.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException(String.format("胶板报工累计消耗不能小于0，当前已消耗 %s，本次修订 %s",
                    consumedLength, delta));
        }
        BigDecimal receiveStart = nullToZero(usage.getReceiveStartPosition());
        BigDecimal receiveEnd = receiveStart.add(nullToZero(usage.getReceiveLength()));
        BigDecimal nextAvailableStart = nullToZero(usage.getAvailableStartPosition()).add(delta);
        if (nextAvailableStart.compareTo(receiveStart) < 0 || nextAvailableStart.compareTo(receiveEnd) > 0) {
            throw invalidParamException("胶板消耗修订后可用位置超出本次领用区间");
        }
        String nextUsageStatus = nextAvailableLength.compareTo(BigDecimal.ZERO) > 0
                ? LEDGER_USAGE_STATUS_ACTIVE : LEDGER_USAGE_STATUS_USED_UP;
        hcAdhesiveGlueBoardUsageMapper.updateById(HcAdhesiveGlueBoardUsageDO.builder()
                .id(usage.getId())
                .consumedLength(nextConsumedLength)
                .availableStartPosition(nextAvailableStart)
                .availableLength(nextAvailableLength)
                .usageStatus(nextUsageStatus)
                .build());
        hcAdhesiveGlueBoardStockMapper.updateById(HcAdhesiveGlueBoardStockDO.builder()
                .id(stock.getId())
                .stockStatus(resolveGlueBoardStockStatus(stock.getId(), stock.getAvailableLength()))
                .build());
    }

    private HcAdhesiveGlueBoardStockDO buildGlueBoardStockFromLedger(HcToolingConsumableLedgerDO ledger,
                                                                     BigDecimal consumedQty) {
        BigDecimal receiveQty = nullToZero(ledger.getReceiveQty());
        BigDecimal usedQty = nullToZero(consumedQty);
        boolean ledgerUsedUp = isLedgerUsedUp(ledger);
        BigDecimal availableLength = ledgerUsedUp ? BigDecimal.ZERO : receiveQty.subtract(usedQty);
        BigDecimal nextUsedQty = ledgerUsedUp ? receiveQty : usedQty;
        if (availableLength.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException("胶板已消耗量不能大于领用量");
        }
        LocalDateTime receiveTime = ledger.getReceiveTime() == null ? LocalDateTime.now() : ledger.getReceiveTime();
        return HcAdhesiveGlueBoardStockDO.builder()
                .tenantId(ledger.getTenantId() == null ? TenantContextHolder.getRequiredTenantId() : ledger.getTenantId())
                .toolingLedgerId(ledger.getId())
                .accessoryCategory(TYPE_GLUE_BOARD)
                .accessoryCategoryName(typeName(TYPE_GLUE_BOARD))
                .glueBoardMaterialCode(ledger.getErpMaterialCode())
                .glueBoardMaterialName(typeName(TYPE_GLUE_BOARD))
                .glueBoardModel(ledger.getModel())
                .glueBoardBatchNo(ledger.getBatchNo())
                .receiveStartPosition(BigDecimal.ZERO)
                .receiveLength(receiveQty)
                .stockMeasureMode(GLUE_BOARD_STOCK_MEASURE_LENGTH)
                .receiveCount(BigDecimal.ZERO)
                .usedLength(nextUsedQty)
                .usedCount(BigDecimal.ZERO)
                .availableStartPosition(nextUsedQty)
                .availableLength(availableLength)
                .availableCount(BigDecimal.ZERO)
                .lossLength(BigDecimal.ZERO)
                .lossCount(BigDecimal.ZERO)
                .lifetimeMode(GLUE_BOARD_LIFETIME_NONE)
                .lifetimeLimitLength(BigDecimal.ZERO)
                .lifetimeLimitCount(BigDecimal.ZERO)
                .lifeUsedLength(BigDecimal.ZERO)
                .lifeUsedCount(BigDecimal.ZERO)
                .stockStatus(availableLength.compareTo(BigDecimal.ZERO) > 0
                        ? GLUE_BOARD_STOCK_ACTIVE : GLUE_BOARD_STOCK_USED_UP)
                .qualityStatus(GLUE_BOARD_QUALITY_NORMAL)
                .receiverId(ledger.getReceiverId())
                .receiverName(ledger.getReceiverName())
                .receiveDate(receiveTime.toLocalDate())
                .receiveTime(receiveTime)
                .printCount(0)
                .erpTransferNo("TOOLING-LEDGER-" + ledger.getId())
                .transferQty(receiveQty)
                .transferUnit(resolveGlueBoardUnit(ledger))
                .unpackQty(receiveQty)
                .unpackUnit(resolveGlueBoardUnit(ledger))
                .erpTransferStatus(GLUE_BOARD_ERP_TRANSFER_NOT_SYNCED)
                .remark(ledger.getRemark())
                .extraJson(buildGlueBoardExtraJson(ledger))
                .build();
    }

    private HcAdhesiveGlueBoardStockDO buildGlueBoardStockBalanceUpdate(Long stockId,
                                                                        HcToolingConsumableLedgerDO ledger,
                                                                        BigDecimal consumedQty) {
        BigDecimal receiveQty = nullToZero(ledger.getReceiveQty());
        BigDecimal usedQty = nullToZero(consumedQty);
        boolean ledgerUsedUp = isLedgerUsedUp(ledger);
        BigDecimal availableLength = ledgerUsedUp ? BigDecimal.ZERO : receiveQty.subtract(usedQty);
        BigDecimal nextUsedQty = ledgerUsedUp ? receiveQty : usedQty;
        if (availableLength.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException("胶板已消耗量不能大于领用量");
        }
        LocalDateTime receiveTime = ledger.getReceiveTime() == null ? LocalDateTime.now() : ledger.getReceiveTime();
        return HcAdhesiveGlueBoardStockDO.builder()
                .id(stockId)
                .toolingLedgerId(ledger.getId())
                .accessoryCategory(TYPE_GLUE_BOARD)
                .accessoryCategoryName(typeName(TYPE_GLUE_BOARD))
                .glueBoardMaterialCode(ledger.getErpMaterialCode())
                .glueBoardMaterialName(typeName(TYPE_GLUE_BOARD))
                .glueBoardModel(ledger.getModel())
                .glueBoardBatchNo(ledger.getBatchNo())
                .receiveStartPosition(BigDecimal.ZERO)
                .receiveLength(receiveQty)
                .stockMeasureMode(GLUE_BOARD_STOCK_MEASURE_LENGTH)
                .receiveCount(BigDecimal.ZERO)
                .usedLength(nextUsedQty)
                .usedCount(BigDecimal.ZERO)
                .availableStartPosition(nextUsedQty)
                .availableLength(availableLength)
                .availableCount(BigDecimal.ZERO)
                .lossLength(BigDecimal.ZERO)
                .lossCount(BigDecimal.ZERO)
                .lifetimeMode(GLUE_BOARD_LIFETIME_NONE)
                .lifetimeLimitLength(BigDecimal.ZERO)
                .lifetimeLimitCount(BigDecimal.ZERO)
                .lifeUsedLength(BigDecimal.ZERO)
                .lifeUsedCount(BigDecimal.ZERO)
                .stockStatus(resolveGlueBoardStockStatus(stockId, availableLength))
                .qualityStatus(GLUE_BOARD_QUALITY_NORMAL)
                .receiverId(ledger.getReceiverId())
                .receiverName(ledger.getReceiverName())
                .receiveDate(receiveTime.toLocalDate())
                .receiveTime(receiveTime)
                .transferQty(receiveQty)
                .transferUnit(resolveGlueBoardUnit(ledger))
                .unpackQty(receiveQty)
                .unpackUnit(resolveGlueBoardUnit(ledger))
                .erpTransferStatus(GLUE_BOARD_ERP_TRANSFER_NOT_SYNCED)
                .remark(ledger.getRemark())
                .extraJson(buildGlueBoardExtraJson(ledger))
                .build();
    }

    private HcAdhesiveGlueBoardStockDO buildGlueBoardStockSnapshotUpdate(Long stockId,
                                                                         HcToolingConsumableLedgerDO ledger) {
        LocalDateTime receiveTime = ledger.getReceiveTime() == null ? LocalDateTime.now() : ledger.getReceiveTime();
        return HcAdhesiveGlueBoardStockDO.builder()
                .id(stockId)
                .toolingLedgerId(ledger.getId())
                .accessoryCategory(TYPE_GLUE_BOARD)
                .accessoryCategoryName(typeName(TYPE_GLUE_BOARD))
                .glueBoardMaterialCode(ledger.getErpMaterialCode())
                .glueBoardMaterialName(typeName(TYPE_GLUE_BOARD))
                .receiverId(ledger.getReceiverId())
                .receiverName(ledger.getReceiverName())
                .receiveDate(receiveTime.toLocalDate())
                .receiveTime(receiveTime)
                .transferUnit(resolveGlueBoardUnit(ledger))
                .unpackUnit(resolveGlueBoardUnit(ledger))
                .remark(ledger.getRemark())
                .extraJson(buildGlueBoardExtraJson(ledger))
                .build();
    }

    private void validateGlueBoardLedger(HcToolingConsumableLedgerDO ledger) {
        if (!isGlueBoardLedger(ledger)) {
            return;
        }
        if (StrUtil.isBlank(ledger.getModel())) {
            throw invalidParamException("胶板型号不能为空");
        }
        if (nullToZero(ledger.getReceiveQty()).compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("胶板领用量必须大于0");
        }
        if (isAdhesiveGlueBoardLedger(ledger)) {
            syncGlueBoardMaterialCodeFromMap(ledger);
        }
    }

    /**
     * 粘胶胶板料号以“成品胶板对照”作为唯一业务来源，不能采信页面或导入文件的默认料号。
     */
    private void syncGlueBoardMaterialCodeFromMap(HcToolingConsumableLedgerDO ledger) {
        String model = StrUtil.trimToEmpty(ledger.getModel());
        Set<String> materialCodes = finishedGlueBoardMapService
                .getGlueBoardModelItems(ledger.getProcessCode(), null)
                .stream()
                .filter(item -> model.equalsIgnoreCase(StrUtil.trimToEmpty(item.getGlueBoardModel())))
                .map(HcFinishedGlueBoardMapItemDO::getGlueBoardMaterialCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (materialCodes.isEmpty()) {
            throw invalidParamException("胶板型号“" + model + "”未维护料号，请先维护成品胶板对照表");
        }
        if (materialCodes.size() > 1) {
            throw invalidParamException("胶板型号“" + model + "”对应多个料号，请先在成品胶板对照表中消除歧义");
        }
        ledger.setErpMaterialCode(materialCodes.iterator().next());
    }

    private void ensureGlueBoardBatchAvailable(HcToolingConsumableLedgerDO ledger, Long ignoreStockId) {
        if (isAdhesiveGlueBoardLedger(ledger)) {
            return;
        }
        HcAdhesiveGlueBoardStockDO existed = hcAdhesiveGlueBoardStockMapper.selectByBatchNo(ledger.getBatchNo());
        if (existed != null && !Objects.equals(existed.getId(), ignoreStockId)) {
            throw invalidParamException("胶板边库批号已存在，不能重复领用");
        }
    }

    private boolean hasExternalGlueBoardStockMovement(HcAdhesiveGlueBoardStockDO stock, Long ledgerId) {
        if (stock == null) {
            return false;
        }
        if (hcAdhesiveGlueBoardUsageMapper.existsActiveByGlueBoardStockId(stock.getId())
                || stock.getLatestInspectionId() != null
                || stock.getInspectionSubmitTime() != null
                || nullToZero(stock.getLossLength()).compareTo(BigDecimal.ZERO) > 0
                || nullToZero(stock.getLossCount()).compareTo(BigDecimal.ZERO) > 0
                || nullToZero(stock.getUsedCount()).compareTo(BigDecimal.ZERO) > 0) {
            return true;
        }
        BigDecimal genericConsumedQty = hcToolingConsumableConsumeMapper.sumConsumeQtyByLedgerId(ledgerId, null);
        return nullToZero(stock.getUsedLength()).compareTo(genericConsumedQty) > 0;
    }

    private boolean hasGlueBoardStockCoreChanged(HcAdhesiveGlueBoardStockDO stock,
                                                 HcToolingConsumableLedgerDO ledger) {
        return !sameText(stock.getGlueBoardModel(), ledger.getModel())
                || !sameText(stock.getGlueBoardBatchNo(), ledger.getBatchNo())
                || !sameDecimal(stock.getReceiveLength(), ledger.getReceiveQty());
    }

    private boolean hasGlueBoardIdentityChanged(HcToolingConsumableLedgerDO oldLedger,
                                                HcToolingConsumableLedgerDO ledger) {
        return oldLedger != null
                && (!sameText(oldLedger.getProcessCode(), ledger.getProcessCode())
                || !sameText(oldLedger.getModel(), ledger.getModel())
                || !sameText(oldLedger.getBatchNo(), ledger.getBatchNo()));
    }

    private String resolveGlueBoardStockStatus(Long stockId, BigDecimal availableLength) {
        if (hcAdhesiveGlueBoardUsageMapper.existsActiveByGlueBoardStockId(stockId)) {
            return GLUE_BOARD_STOCK_LOCKED;
        }
        return nullToZero(availableLength).compareTo(BigDecimal.ZERO) <= 0
                ? GLUE_BOARD_STOCK_USED_UP : GLUE_BOARD_STOCK_ACTIVE;
    }

    private boolean isGlueBoardLedger(HcToolingConsumableLedgerDO ledger) {
        return ledger != null && TYPE_GLUE_BOARD.equals(ledger.getConsumableType());
    }

    private boolean isLedgerUsedUp(HcToolingConsumableLedgerDO ledger) {
        return ledger != null
                && LEDGER_USAGE_STATUS_USED_UP.equals(normalizeLedgerUsageStatus(ledger.getUsageStatus()));
    }

    private boolean isLedgerReturned(HcToolingConsumableLedgerDO ledger) {
        return ledger != null
                && LEDGER_USAGE_STATUS_RETURNED.equals(normalizeLedgerUsageStatus(ledger.getUsageStatus()));
    }

    private void requireLedgerActive(HcToolingConsumableLedgerDO ledger, String actionName) {
        if (ledger != null && LEDGER_USAGE_STATUS_ACTIVE.equals(normalizeLedgerUsageStatus(ledger.getUsageStatus()))) {
            return;
        }
        throw invalidParamException("当前边库耗材台账已" + ledgerUsageStatusName(ledger == null ? null : ledger.getUsageStatus())
                + "，不能" + actionName);
    }

    private void clearLedgerReturnInfo(HcToolingConsumableLedgerDO ledger) {
        ledger.setReturnQty(null);
        ledger.setReturnReason(null);
        ledger.setReturnAuthUserId(null);
        ledger.setReturnAuthUserName(null);
        ledger.setReturnAuthTime(null);
    }

    private void copyLedgerReturnInfo(HcToolingConsumableLedgerDO source,
                                      HcToolingConsumableLedgerDO target) {
        target.setReturnQty(source.getReturnQty());
        target.setReturnReason(source.getReturnReason());
        target.setReturnAuthUserId(source.getReturnAuthUserId());
        target.setReturnAuthUserName(source.getReturnAuthUserName());
        target.setReturnAuthTime(source.getReturnAuthTime());
    }

    private boolean isAdhesive1GlueBoardLedger(HcToolingConsumableLedgerDO ledger) {
        if (!isGlueBoardLedger(ledger)) {
            return false;
        }
        String processCode = StrUtil.trimToEmpty(ledger.getProcessCode());
        return PROCESS_ADHESIVE.equalsIgnoreCase(processCode)
                || PROCESS_ADHESIVE1.equalsIgnoreCase(processCode);
    }

    private boolean isAdhesive2GlueBoardLedger(HcToolingConsumableLedgerDO ledger) {
        return isGlueBoardLedger(ledger)
                && PROCESS_ADHESIVE2.equalsIgnoreCase(StrUtil.trimToEmpty(ledger.getProcessCode()));
    }

    private boolean isAdhesiveGlueBoardLedger(HcToolingConsumableLedgerDO ledger) {
        return isAdhesive1GlueBoardLedger(ledger) || isAdhesive2GlueBoardLedger(ledger);
    }

    private boolean isGlueBoardConsume(HcToolingConsumableConsumeDO consume) {
        return consume != null && TYPE_GLUE_BOARD.equals(consume.getConsumableType());
    }

    private String buildGlueBoardExtraJson(HcToolingConsumableLedgerDO ledger) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("sourceType", GLUE_BOARD_SOURCE_TYPE_TOOLING_LEDGER);
        extra.put("toolingLedgerId", ledger.getId());
        extra.put("processCode", ledger.getProcessCode());
        extra.put("processName", ledger.getProcessName());
        extra.put("ledgerBatchNo", ledger.getBatchNo());
        return JsonUtils.toJsonString(extra);
    }

    private String resolveGlueBoardUnit(HcToolingConsumableLedgerDO ledger) {
        return firstNotBlank(ledger.getUomName(), ledger.getUomCode(), ledger.getUom(), "米");
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        return nullToZero(left).compareTo(nullToZero(right)) == 0;
    }

    private void validateImportedConsumeMatchesLedger(ImportRow row, HcToolingConsumableLedgerDO ledger) {
        if (!sameText(row.consumeConsumableType, ledger.getConsumableType())
                || !sameText(row.consumeProcessCode, ledger.getProcessCode())
                || !sameText(row.consumeBatchNo, ledger.getBatchNo())
                || !sameText(row.consumeModel, ledger.getModel())) {
            throw invalidParamException(String.format("第%d行：消耗明细必须与领用批次一致", row.rowNo));
        }
    }

    private void validateProcessConsumableExists(Long id) {
        if (id == null || hcToolingProcessConsumableMapper.selectById(id) == null) {
            throw invalidParamException("工序耗材字典不存在");
        }
    }

    private void validateProcessConsumableUnique(Long id, String processCode, String consumableType) {
        HcToolingProcessConsumableDO existed = hcToolingProcessConsumableMapper
                .selectOneByProcessAndType(processCode, consumableType);
        if (existed != null && !existed.getId().equals(id)) {
            throw invalidParamException("当前工序下已配置该耗材种类");
        }
    }

    private void validateQty(BigDecimal qty, String fieldName) {
        if (qty == null) {
            throw invalidParamException(fieldName + "不能为空");
        }
        if (qty.compareTo(BigDecimal.ZERO) < 0) {
            throw invalidParamException(fieldName + "不能为负数");
        }
    }

    private void validateConsumeQty(BigDecimal qty) {
        if (qty == null) {
            throw invalidParamException("消耗量不能为空");
        }
    }

    private String requireText(String value, String message) {
        String text = trimToNull(value);
        if (StrUtil.isBlank(text)) {
            throw invalidParamException(message);
        }
        return text;
    }

    private void addImportFailure(HcToolingConsumableLedgerImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private String typeName(String value) {
        return value == null ? null : CONSUMABLE_TYPE_NAMES.getOrDefault(value, value);
    }

    private String processName(String value) {
        return value == null ? null : PROCESS_NAMES.getOrDefault(value, value);
    }

    private String ledgerUsageStatusName(String value) {
        String status = normalizeLedgerUsageStatus(value);
        return status == null ? value : LEDGER_USAGE_STATUS_NAMES.getOrDefault(status, status);
    }

    private String requireLedgerUsageStatus(String value) {
        String status = normalizeLedgerUsageStatus(value);
        if (StrUtil.isBlank(status)) {
            throw invalidParamException("使用状态不支持");
        }
        return status;
    }

    private String normalizeLedgerUsageStatus(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return LEDGER_USAGE_STATUS_ACTIVE;
        }
        String normalized = text.toUpperCase();
        if (LEDGER_USAGE_STATUS_NAMES.containsKey(normalized)) {
            return normalized;
        }
        if ("使用中".equals(text) || "未用完".equals(text) || "在用".equals(text) || "正常".equals(text)) {
            return LEDGER_USAGE_STATUS_ACTIVE;
        }
        if ("已用完".equals(text) || "用完".equals(text) || "即将用完".equals(text)) {
            return LEDGER_USAGE_STATUS_USED_UP;
        }
        if ("已退库".equals(text) || "退库".equals(text)) {
            return LEDGER_USAGE_STATUS_RETURNED;
        }
        return null;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String trimToNull(String value) {
        String text = StrUtil.trim(value);
        return StrUtil.isBlank(text) ? null : text;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean sameText(String left, String right) {
        return StrUtil.equals(trimToNull(left), trimToNull(right));
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static class ImportRow {
        private int rowNo;
        private Long ledgerId;
        private String consumableType;
        private String consumableTypeName;
        private String processCode;
        private String processName;
        private String model;
        private String batchNo;
        private String erpMaterialCode;
        private BigDecimal receiveQty;
        private Long uomId;
        private String uomCode;
        private String uomName;
        private String uom;
        private LocalDateTime receiveTime;
        private Long receiverId;
        private String receiverName;
        private String usageStatus;
        private String remark;
        private Long consumeId;
        private boolean consumePresent;
        private String consumeConsumableType;
        private String consumeConsumableTypeName;
        private String consumeProcessCode;
        private String consumeProcessName;
        private String consumeModel;
        private String consumeBatchNo;
        private BigDecimal consumeQty;
        private LocalDateTime consumeTime;
        private String planNo;
        private String productionBatchNo;

        private String importKey() {
            return String.join("|",
                    nullToEmpty(consumableType),
                    nullToEmpty(processCode),
                    nullToEmpty(model),
                    nullToEmpty(batchNo),
                    receiveTime == null ? "" : DATE_TIME_FORMATTER.format(receiveTime),
                    nullToEmpty(receiverName));
        }

        private String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }

    private void assertBladeLedgerUnchanged(HcToolingConsumableLedgerDO oldLedger,
                                            HcToolingConsumableLedgerDO next) {
        if (!TYPE_BLADE.equals(oldLedger.getConsumableType())) return;
        boolean linked = hcToolingConsumableConsumeMapper.selectListForUpdate(oldLedger.getId()).stream()
                .anyMatch(row -> "CUT_ROUND_BLADE_REPLACE".equals(row.getConsumeSource()));
        if (linked && (!Objects.equals(oldLedger.getConsumableType(), next.getConsumableType())
                || !Objects.equals(oldLedger.getProcessCode(), next.getProcessCode())
                || !Objects.equals(oldLedger.getModel(), next.getModel())
                || !Objects.equals(oldLedger.getBatchNo(), next.getBatchNo())
                || !Objects.equals(oldLedger.getErpMaterialCode(), next.getErpMaterialCode())
                || !Objects.equals(oldLedger.getUomId(), next.getUomId())
                || !Objects.equals(oldLedger.getUomCode(), next.getUomCode())
                || !Objects.equals(oldLedger.getUomName(), next.getUomName())
                || oldLedger.getReceiveQty().compareTo(next.getReceiveQty()) != 0)) {
            throw invalidParamException("刀片台账已关联更换记录，不能修改耗材、型号、批号、单位或领用量；补充库存请新增领用");
        }
    }

    private void assertNotBladeConsumption(HcToolingConsumableConsumeDO row) {
        if (row != null && "CUT_ROUND_BLADE_REPLACE".equals(row.getConsumeSource())) {
            throw invalidParamException("刀片更换产生的消耗需保留追溯，不能在台账编辑、删除或导入覆盖");
        }
    }

    private void assertNotAutomaticConsumption(Long id) {
        HcToolingConsumableConsumeDO row = hcToolingConsumableConsumeMapper.selectById(id);
        assertNotBladeConsumption(row);
        if (grindingConsumptionMapper.referencesConsume(id)) {
            throw invalidParamException("磨皮更换产生的消耗请从来源报工或研发记录维护，不能在台账单独修改或删除");
        }
    }
}
