package cn.iocoder.yudao.module.mes.service.hc.productioninstruction;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionChangeoverPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionChangeoverStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionMessagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionOperationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionRevokeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo.HcProductionInstructionSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationStatusLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderStatusLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionChangeoverPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionRecipientDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelMaterialDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationStatusLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderStatusLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionChangeoverPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionRecipientMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMaterialMapper;
import cn.iocoder.yudao.module.mes.service.hc.nginventory.HcNgInventoryService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcProductionInstructionServiceImpl implements HcProductionInstructionService {

    private static final String STATUS_ISSUED = "ISSUED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String INSTRUCTION_TYPE_DAILY = "DAILY";
    private static final String INSTRUCTION_TYPE_PAUSE = "PAUSE";
    private static final String INSTRUCTION_TYPE_RESUME = "RESUME";
    private static final String INSTRUCTION_TYPE_CANCEL = "CANCEL";
    private static final String INSTRUCTION_TYPE_CHANGEOVER = "CHANGEOVER";
    private static final String INSTRUCTION_TYPE_FREEZE_STOCK = "FREEZE_STOCK";
    private static final String INSTRUCTION_TYPE_UNFREEZE_STOCK = "UNFREEZE_STOCK";
    private static final String SCOPE_TYPE_PLAN = "PLAN";
    private static final String SCOPE_TYPE_OPERATION = "OPERATION";
    private static final String SCOPE_TYPE_SEGMENT = "SEGMENT";
    private static final String EXECUTE_STATUS_PENDING = "PENDING";
    private static final String EXECUTE_STATUS_EXECUTING = "EXECUTING";
    private static final String EXECUTE_STATUS_COMPLETED = "COMPLETED";
    private static final String PIECE_STATUS_DONE = "DONE";
    private static final String PRE_PROCESS_SELF_CHECK_ATTRIBUTION_TYPE = "PRE_PROCESS_SELF_CHECK";
    private static final String PLAN_STATUS_RELEASED = "RELEASED";
    private static final String PLAN_STATUS_PAUSED = "PAUSED";
    private static final String PLAN_STATUS_CANCELLED = "CANCELLED";
    private static final String OP_STATUS_NOT_RELEASED = "NOT_RELEASED";
    private static final String OP_STATUS_RELEASED = "RELEASED";
    private static final String OP_STATUS_RUNNING = "RUNNING";
    private static final String OP_STATUS_PAUSED = "PAUSED";
    private static final String OP_STATUS_FINISHED = "FINISHED";
    private static final String OP_STATUS_CANCELLED = "CANCELLED";
    private static final String RECIPIENT_NOTIFY_READ = "READ";
    private static final DateTimeFormatter INSTRUCTION_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Resource
    private HcProductionInstructionMapper hcProductionInstructionMapper;

    @Resource
    private HcProductionInstructionChangeoverPieceMapper hcProductionInstructionChangeoverPieceMapper;

    @Resource
    private HcAdhesive2ReportMapper hcAdhesive2ReportMapper;

    @Resource
    private HcProductionInstructionRecipientMapper hcProductionInstructionRecipientMapper;

    @Resource
    private HcProductModelMaterialMapper hcProductModelMaterialMapper;

    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;

    @Resource
    private HcPlanOrderOperationStatusLogMapper hcPlanOrderOperationStatusLogMapper;

    @Resource
    private HcPlanOrderStatusLogMapper hcPlanOrderStatusLogMapper;

    @Resource
    private HcNgInventoryService hcNgInventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long issueProductionInstruction(HcProductionInstructionSaveReqVO reqVO) {
        HcProductionInstructionDO entity = BeanUtils.toBean(reqVO, HcProductionInstructionDO.class);
        normalizeInstructionScope(entity, reqVO);
        normalizeAndFillSnapshot(entity);
        normalizeInstructionScope(entity, reqVO);
        validateInventoryControlInstruction(entity);
        ensureNoOpenChangeoverInstruction(entity);
        List<HcPlanOrderOperationDO> actionTargets = resolveActionTargets(entity, reqVO);
        if (isStatusInstruction(entity.getInstructionType())) {
            applyStatusInstruction(entity, actionTargets);
        }
        entity.setInstructionNo(generateInstructionNo());
        entity.setInstructionBatchNo(firstNotBlank(entity.getInstructionBatchNo(), entity.getInstructionNo()));
        entity.setStatus(STATUS_ISSUED);
        entity.setIssuerId(firstNonNull(entity.getIssuerId(), SecurityFrameworkUtils.getLoginUserId()));
        entity.setIssuerName(firstNotBlank(entity.getIssuerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        entity.setIssuedTime(LocalDateTime.now());
        entity.setConfirmerId(null);
        entity.setConfirmerName(null);
        entity.setConfirmTime(null);
        entity.setRevokedBy(null);
        entity.setRevokedByName(null);
        entity.setRevokedTime(null);
        entity.setRevokeReason(null);
        hcProductionInstructionMapper.insert(entity);
        if (INSTRUCTION_TYPE_UNFREEZE_STOCK.equals(entity.getInstructionType())) {
            hcNgInventoryService.autoUnfreezeByInstruction(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductionInstruction(HcProductionInstructionSaveReqVO reqVO) {
        HcProductionInstructionDO old = validateExists(reqVO.getId());
        validateInventoryControlInstructionImmutable(old);
        if (!STATUS_ISSUED.equals(old.getStatus())) {
            throw invalidParamException("只有下达状态的生产指令允许修改");
        }
        HcProductionInstructionDO updateObj = BeanUtils.toBean(reqVO, HcProductionInstructionDO.class);
        normalizeInstructionScope(updateObj, reqVO);
        normalizeAndFillSnapshot(updateObj);
        normalizeInstructionScope(updateObj, reqVO);
        updateObj.setInstructionNo(old.getInstructionNo());
        updateObj.setStatus(old.getStatus());
        updateObj.setIssuerId(firstNonNull(updateObj.getIssuerId(), old.getIssuerId()));
        updateObj.setIssuerName(firstNotBlank(updateObj.getIssuerName(), old.getIssuerName()));
        updateObj.setIssuedTime(firstNonNull(updateObj.getIssuedTime(), old.getIssuedTime()));
        hcProductionInstructionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductionInstruction(Long id) {
        HcProductionInstructionDO old = validateExists(id);
        validateInventoryControlInstructionImmutable(old);
        if (STATUS_CONFIRMED.equals(old.getStatus())) {
            throw invalidParamException("已确认的生产指令不能删除，请使用撤下");
        }
        hcProductionInstructionMapper.deleteById(id);
    }

    @Override
    public HcProductionInstructionDO getProductionInstruction(Long id) {
        return validateExists(id);
    }

    @Override
    public List<HcProductionInstructionDO> getProductionInstructionList(HcProductionInstructionPageReqVO reqVO) {
        return hcProductionInstructionMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcProductionInstructionDO> getProductionInstructionPage(HcProductionInstructionPageReqVO pageReqVO) {
        return hcProductionInstructionMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcProductionInstructionDO> getOperationInstructionList(HcProductionInstructionOperationReqVO reqVO) {
        List<String> statuses = Boolean.TRUE.equals(reqVO.getIncludeConfirmed())
                ? List.of(STATUS_ISSUED, STATUS_CONFIRMED)
                : List.of(STATUS_ISSUED);
        return hcProductionInstructionMapper.selectOperationInstructionList(reqVO, statuses);
    }

    @Override
    public PageResult<HcProductionInstructionRespVO> getCurrentUserMessagePage(
            HcProductionInstructionMessagePageReqVO pageReqVO) {
        normalizeMessagePageReq(pageReqVO);
        Long total = hcProductionInstructionMapper.selectMessageCount(pageReqVO);
        if (total == null || total <= 0) {
            return new PageResult<>(List.of(), 0L);
        }
        int pageSize = PageParam.PAGE_SIZE_NONE.equals(pageReqVO.getPageSize())
                ? Math.toIntExact(Math.min(total, Integer.MAX_VALUE))
                : pageReqVO.getPageSize();
        int offset = PageParam.PAGE_SIZE_NONE.equals(pageReqVO.getPageSize())
                ? 0
                : (pageReqVO.getPageNo() - 1) * pageSize;
        List<HcProductionInstructionRespVO> rows = hcProductionInstructionMapper.selectMessagePage(
                pageReqVO, pageSize, offset);
        return new PageResult<>(rows, total);
    }

    @Override
    public Long getCurrentUserUnreadMessageCount(HcProductionInstructionMessagePageReqVO reqVO) {
        normalizeMessagePageReq(reqVO);
        Long count = hcProductionInstructionMapper.selectUnreadMessageCount(reqVO);
        return count == null ? 0L : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCurrentUserMessageRead(Long id) {
        HcProductionInstructionDO instruction = validateExists(id);
        markCurrentRecipientRead(instruction.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmProductionInstruction(Long id) {
        HcProductionInstructionDO old = validateExists(id);
        if (STATUS_REVOKED.equals(old.getStatus())) {
            throw invalidParamException("已撤下的生产指令不能确认");
        }
        if (STATUS_CONFIRMED.equals(old.getStatus())) {
            markCurrentRecipientRead(id);
            return;
        }
        HcProductionInstructionDO updateObj = new HcProductionInstructionDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_CONFIRMED);
        updateObj.setConfirmerId(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setConfirmerName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setConfirmTime(LocalDateTime.now());
        hcProductionInstructionMapper.updateById(updateObj);
        markCurrentRecipientRead(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeProductionInstruction(HcProductionInstructionRevokeReqVO reqVO) {
        HcProductionInstructionDO old = validateExists(reqVO.getId());
        validateInventoryControlInstructionImmutable(old);
        if (STATUS_REVOKED.equals(old.getStatus())) {
            return;
        }
        HcProductionInstructionDO updateObj = new HcProductionInstructionDO();
        updateObj.setId(reqVO.getId());
        updateObj.setStatus(STATUS_REVOKED);
        updateObj.setRevokedBy(SecurityFrameworkUtils.getLoginUserId());
        updateObj.setRevokedByName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setRevokedTime(LocalDateTime.now());
        updateObj.setRevokeReason(StrUtil.blankToDefault(trimToNull(reqVO.getRevokeReason()), "指令撤下"));
        hcProductionInstructionMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcProductionInstructionDO startChangeoverInstruction(HcProductionInstructionChangeoverStartReqVO reqVO) {
        HcProductionInstructionDO old = validateExists(reqVO.getId());
        // 与粘胶2首次报工确认使用相同锁顺序，禁止确认期间切换当前生产型号。
        if (hcPlanOrderOperationMapper.selectByIdForUpdate(old.getPlanOperationId()) == null) {
            throw invalidParamException("换型指令关联的计划工序不存在");
        }
        old = hcProductionInstructionMapper.selectByIdForUpdate(old.getId());
        if (old == null) {
            throw invalidParamException("生产指令不存在");
        }
        validateChangeoverInstruction(old);
        if (STATUS_REVOKED.equals(old.getStatus())) {
            throw invalidParamException("已撤下的换型指令不能执行");
        }
        String executeStatus = StrUtil.blankToDefault(old.getExecuteStatus(), EXECUTE_STATUS_PENDING);
        if (EXECUTE_STATUS_COMPLETED.equals(executeStatus) || EXECUTE_STATUS_EXECUTING.equals(executeStatus)) {
            return old;
        }
        ensureNoOtherExecutingChangeover(old);
        long completedQty = hcProductionInstructionChangeoverPieceMapper.selectList(
                new LambdaQueryWrapperX<HcProductionInstructionChangeoverPieceDO>()
                        .eq(HcProductionInstructionChangeoverPieceDO::getInstructionId, old.getId())
                        .last("FOR UPDATE")).size();
        HcProductionInstructionDO updateObj = new HcProductionInstructionDO();
        updateObj.setId(old.getId());
        updateObj.setStatus(STATUS_CONFIRMED);
        updateObj.setConfirmerId(firstNonNull(old.getConfirmerId(), SecurityFrameworkUtils.getLoginUserId()));
        updateObj.setConfirmerName(firstNotBlank(old.getConfirmerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setConfirmTime(firstNonNull(old.getConfirmTime(), LocalDateTime.now()));
        updateObj.setCompletedQty(Math.toIntExact(completedQty));
        updateObj.setExecuteStatus(EXECUTE_STATUS_EXECUTING);
        updateObj.setExecuteUserId(firstNonNull(old.getExecuteUserId(), reqVO.getExecuteUserId(),
                SecurityFrameworkUtils.getLoginUserId()));
        updateObj.setExecuteUserName(firstNotBlank(old.getExecuteUserName(), reqVO.getExecuteUserName(),
                SecurityFrameworkUtils.getLoginUserNickname(), "系统"));
        updateObj.setExecuteStartTime(firstNonNull(old.getExecuteStartTime(), LocalDateTime.now()));
        updateObj.setRemark(firstNotBlank(reqVO.getRemark(), old.getRemark()));
        hcProductionInstructionMapper.updateById(updateObj);
        return validateExists(old.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcProductionInstructionDO recordChangeoverPiece(HcProductionInstructionChangeoverPieceReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw invalidParamException("生产指令 ID 不能为空");
        }
        // 串行处理同一指令的记片与完成计数，避免并发最后一片超额或重复累计。
        HcProductionInstructionDO instruction = hcProductionInstructionMapper.selectByIdForUpdate(reqVO.getId());
        if (instruction == null) {
            throw invalidParamException("生产指令不存在");
        }
        validateChangeoverInstruction(instruction);
        if (STATUS_REVOKED.equals(instruction.getStatus())) {
            throw invalidParamException("已撤下的换型指令不能记录扫码片号");
        }
        String pieceNo = trimToNull(reqVO.getPieceNo());
        if (pieceNo == null) {
            throw invalidParamException("片号不能为空");
        }
        HcAdhesive2ReportDO adhesive2Report = validateChangeoverPieceReport(instruction, reqVO, pieceNo);
        // 外层报工事务可能已有一致性快照，记片和数量必须使用加锁当前读。
        List<HcProductionInstructionChangeoverPieceDO> recordedPieces =
                hcProductionInstructionChangeoverPieceMapper.selectList(
                        new LambdaQueryWrapperX<HcProductionInstructionChangeoverPieceDO>()
                                .eq(HcProductionInstructionChangeoverPieceDO::getInstructionId, instruction.getId())
                                .last("FOR UPDATE"));
        HcProductionInstructionChangeoverPieceDO existed = recordedPieces.stream()
                .filter(item -> sameText(item.getPieceNo(), pieceNo)
                        || Objects.equals(item.getAdhesive2ReportId(), adhesive2Report.getId()))
                .findFirst().orElse(null);
        if (existed != null) {
            if (Objects.equals(existed.getInstructionId(), instruction.getId())
                    && Objects.equals(existed.getAdhesive2ReportId(), adhesive2Report.getId())
                    && sameText(existed.getPieceNo(), pieceNo)
                    && PIECE_STATUS_DONE.equals(existed.getStatus())) {
                return instruction;
            }
            throw invalidParamException("当前换型片号已关联其他报工记录或记片状态异常，不能重复计数");
        }
        if (!EXECUTE_STATUS_EXECUTING.equals(StrUtil.blankToDefault(instruction.getExecuteStatus(), EXECUTE_STATUS_PENDING))) {
            throw invalidParamException("换型指令未开始执行或已完成，不能记录扫码片号");
        }
        HcProductionInstructionChangeoverPieceDO piece = HcProductionInstructionChangeoverPieceDO.builder()
                .tenantId(instruction.getTenantId())
                .instructionId(instruction.getId())
                .instructionNo(instruction.getInstructionNo())
                .planId(instruction.getPlanId())
                .planNo(instruction.getPlanNo())
                .planOperationId(instruction.getPlanOperationId())
                .segmentBatchNo(firstNotBlank(instruction.getSegmentBatchNo(), instruction.getBatchNo()))
                .pieceNo(pieceNo)
                .adhesive2ReportId(adhesive2Report.getId())
                .actualModelCode(adhesive2Report.getModelCode())
                .actualGlueBoardModel(adhesive2Report.getGlueBoardModel())
                .actualGlueBoardBatchNo(adhesive2Report.getGlueBoardBatchNo())
                .scanUserId(SecurityFrameworkUtils.getLoginUserId())
                .scanUserName(firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统"))
                .scanTime(LocalDateTime.now())
                .status(PIECE_STATUS_DONE)
                .remark(trimToNull(reqVO.getRemark()))
                .build();
        hcProductionInstructionChangeoverPieceMapper.insert(piece);
        long completed = recordedPieces.size() + 1L;
        HcProductionInstructionDO updateObj = new HcProductionInstructionDO();
        updateObj.setId(instruction.getId());
        updateObj.setCompletedQty(Math.toIntExact(completed));
        if (completed >= instruction.getTargetQty()) {
            updateObj.setExecuteStatus(EXECUTE_STATUS_COMPLETED);
            updateObj.setExecuteEndTime(LocalDateTime.now());
        }
        hcProductionInstructionMapper.updateById(updateObj);
        return validateExists(instruction.getId());
    }

    private HcAdhesive2ReportDO validateChangeoverPieceReport(HcProductionInstructionDO instruction,
            HcProductionInstructionChangeoverPieceReqVO reqVO, String pieceNo) {
        if (reqVO.getAdhesive2ReportId() == null) {
            throw invalidParamException("换型片数必须关联粘胶2报工记录");
        }
        HcAdhesive2ReportDO report = hcAdhesive2ReportMapper.selectById(reqVO.getAdhesive2ReportId());
        if (report == null) {
            throw invalidParamException("关联的粘胶2报工记录不存在");
        }
        if (!Objects.equals(instruction.getPlanOperationId(), report.getPlanOperationId())) {
            throw invalidParamException("粘胶2报工记录不属于当前换型指令工序");
        }
        if (!Objects.equals(instruction.getPlanId(), report.getPlanId())) {
            throw invalidParamException("粘胶2报工记录不属于当前换型指令计划");
        }
        String reportPieceNo = firstNotBlank(report.getProductionBatchNo(), report.getSourceProductionBatchNo());
        if (!sameText(pieceNo, reportPieceNo)) {
            throw invalidParamException("换型片号与粘胶2报工记录不一致");
        }
        String reportStatus = StrUtil.blankToDefault(report.getReportStatus(), "").toUpperCase(Locale.ROOT);
        if (!Set.of("CONFIRMED", "SUBMITTED").contains(reportStatus)) {
            throw invalidParamException("粘胶2报工尚未扫码确认，不能计入换型完成片数");
        }
        if (report.getConfirmerTime() == null || instruction.getExecuteStartTime() == null
                || report.getConfirmerTime().isBefore(instruction.getExecuteStartTime())) {
            throw invalidParamException("换型指令执行前已确认的历史片不能计入本次目标片数");
        }
        if (isAdhesive2PreProcessSelfCheckAbnormal(report)) {
            throw invalidParamException("压槽加工前自检异常不计入粘胶2换型完成片数");
        }
        if (!sameText(report.getModelCode(), instruction.getTargetModelCode())) {
            throw invalidParamException("粘胶2报工型号与换型指令目标型号不一致");
        }
        return report;
    }

    private boolean isAdhesive2PreProcessSelfCheckAbnormal(HcAdhesive2ReportDO report) {
        if (report == null || StrUtil.isBlank(report.getExtraJson())) {
            return false;
        }
        Map<String, Object> extra = JsonUtils.parseObjectQuietly(report.getExtraJson(),
                new TypeReference<Map<String, Object>>() {});
        if (extra == null) {
            return false;
        }
        String attributionType = StrUtil.trimToEmpty(String.valueOf(extra.get("ngAttributionType")));
        if (PRE_PROCESS_SELF_CHECK_ATTRIBUTION_TYPE.equalsIgnoreCase(attributionType)) {
            return true;
        }
        Object selected = extra.get("preProcessSelfCheckAbnormal");
        return Boolean.TRUE.equals(selected)
                || Integer.valueOf(1).equals(selected)
                || Set.of("1", "TRUE", "Y", "YES").contains(
                        StrUtil.trimToEmpty(String.valueOf(selected)).toUpperCase(Locale.ROOT));
    }

    private HcProductionInstructionDO validateExists(Long id) {
        if (id == null) {
            throw invalidParamException("生产指令ID不能为空");
        }
        HcProductionInstructionDO entity = hcProductionInstructionMapper.selectById(id);
        if (entity == null) {
            throw invalidParamException("生产指令不存在");
        }
        return entity;
    }

    private void normalizeAndFillSnapshot(HcProductionInstructionDO entity) {
        if (StrUtil.isBlank(entity.getInstructionContent())) {
            throw invalidParamException("指令内容不能为空");
        }
        entity.setInstructionContent(entity.getInstructionContent().trim());
        entity.setBatchNo(trimToNull(entity.getBatchNo()));
        entity.setPlanNo(trimToNull(entity.getPlanNo()));
        entity.setProcessCode(trimToUpper(entity.getProcessCode()));
        entity.setOperationCode(trimToUpper(entity.getOperationCode()));
        entity.setProcessName(trimToNull(entity.getProcessName()));
        entity.setOperationName(trimToNull(entity.getOperationName()));
        entity.setRemark(trimToNull(entity.getRemark()));

        if (entity.getPlanOperationId() != null) {
            HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(entity.getPlanOperationId());
            if (operation == null) {
                throw invalidParamException("计划工序不存在");
            }
            entity.setPlanId(firstNonNull(entity.getPlanId(), operation.getPlanId()));
            entity.setOperationCode(firstNotBlank(entity.getOperationCode(), operation.getOpCode()));
            entity.setOperationName(firstNotBlank(entity.getOperationName(), operation.getOpName()));
            entity.setProcessCode(firstNotBlank(entity.getProcessCode(), operation.getOpCode()));
            entity.setProcessName(firstNotBlank(entity.getProcessName(), operation.getOpName()));
            entity.setBatchNo(firstNotBlank(entity.getBatchNo(), operation.getProductionBatchNo(),
                    operation.getBatchNo(), operation.getParentProductionBatchNo()));
        }
        if (entity.getPlanId() != null) {
            HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(entity.getPlanId());
            if (planOrder == null) {
                throw invalidParamException("生产计划不存在");
            }
            entity.setPlanNo(firstNotBlank(entity.getPlanNo(), planOrder.getPlanNo()));
            entity.setBatchNo(firstNotBlank(entity.getBatchNo(), planOrder.getProductionBatchNo(),
                    planOrder.getBatchNo(), planOrder.getParentProductionBatchNo()));
            if (INSTRUCTION_TYPE_CHANGEOVER.equals(entity.getInstructionType())) {
                entity.setBeforeMaterialCode(firstNotBlank(planOrder.getMaterialCode(), entity.getBeforeMaterialCode()));
                entity.setBeforeModelCode(firstNotBlank(planOrder.getModelCode(), entity.getBeforeModelCode()));
            }
        }
        if (StrUtil.isBlank(entity.getBatchNo())) {
            throw invalidParamException("批次号不能为空");
        }
        if (!SCOPE_TYPE_PLAN.equals(entity.getScopeType())
                && StrUtil.isBlank(entity.getOperationName()) && StrUtil.isBlank(entity.getProcessName())) {
            throw invalidParamException("工序不能为空，请选择工序或指定计划工序");
        }
        entity.setOperationName(firstNotBlank(entity.getOperationName(), entity.getProcessName()));
        entity.setProcessName(firstNotBlank(entity.getProcessName(), entity.getOperationName()));
        entity.setOperationCode(firstNotBlank(entity.getOperationCode(), entity.getProcessCode()));
        entity.setProcessCode(firstNotBlank(entity.getProcessCode(), entity.getOperationCode()));
        normalizeChangeoverPayload(entity);
    }

    private void normalizeChangeoverPayload(HcProductionInstructionDO entity) {
        entity.setBeforeMaterialCode(trimToNull(entity.getBeforeMaterialCode()));
        entity.setTargetMaterialCode(trimToNull(entity.getTargetMaterialCode()));
        entity.setBeforeModelCode(trimToNull(entity.getBeforeModelCode()));
        entity.setTargetModelCode(trimToNull(entity.getTargetModelCode()));
        entity.setExecuteStatus(trimToNull(entity.getExecuteStatus()));
        if (!INSTRUCTION_TYPE_CHANGEOVER.equals(entity.getInstructionType())) {
            return;
        }
        if (entity.getPlanOperationId() == null) {
            throw invalidParamException("换型指令必须指定计划工序");
        }
        if (StrUtil.isBlank(entity.getTargetModelCode())) {
            throw invalidParamException("换型指令必须指定目标产品型号");
        }
        List<HcProductModelMaterialDO> modelMaterials = hcProductModelMaterialMapper
                .selectListByModelCode(entity.getTargetModelCode());
        if (StrUtil.isBlank(entity.getTargetMaterialCode())) {
            List<HcProductModelMaterialDO> defaults = modelMaterials.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getIsDefault()))
                    .toList();
            if (defaults.size() != 1) {
                throw invalidParamException("换型目标型号缺少唯一默认料号，请维护型号-物料映射后再下达指令");
            }
            entity.setTargetMaterialCode(defaults.get(0).getMaterialCode());
        } else if (modelMaterials.stream().noneMatch(item ->
                StrUtil.equalsIgnoreCase(entity.getTargetMaterialCode(), item.getMaterialCode()))) {
            throw invalidParamException("换型目标料号未关联目标产品型号，请维护型号-物料映射后再下达指令");
        }
        if (sameText(entity.getBeforeModelCode(), entity.getTargetModelCode())) {
            throw invalidParamException("换型指令目标型号不能与计划型号相同");
        }
        if (entity.getTargetQty() == null || entity.getTargetQty() <= 0) {
            throw invalidParamException("换型指令目标片数必须大于0");
        }
        entity.setCompletedQty(firstNonNull(entity.getCompletedQty(), 0));
        entity.setExecuteStatus(firstNotBlank(entity.getExecuteStatus(), EXECUTE_STATUS_PENDING));
        entity.setAutoRestoreFlag(Boolean.TRUE);
    }

    private void validateChangeoverInstruction(HcProductionInstructionDO instruction) {
        if (!INSTRUCTION_TYPE_CHANGEOVER.equals(instruction.getInstructionType())) {
            throw invalidParamException("当前指令不是换型指令");
        }
        if (instruction.getPlanOperationId() == null) {
            throw invalidParamException("换型指令必须指定计划工序");
        }
        if (instruction.getTargetQty() == null || instruction.getTargetQty() <= 0) {
            throw invalidParamException("换型指令目标片数未配置");
        }
        if (StrUtil.isBlank(instruction.getTargetModelCode())) {
            throw invalidParamException("换型指令目标产品型号未配置");
        }
    }

    private void ensureNoOtherExecutingChangeover(HcProductionInstructionDO instruction) {
        LambdaQueryWrapperX<HcProductionInstructionDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcProductionInstructionDO::getInstructionType, INSTRUCTION_TYPE_CHANGEOVER);
        queryWrapper.eq(HcProductionInstructionDO::getExecuteStatus, EXECUTE_STATUS_EXECUTING);
        queryWrapper.eqIfPresent(HcProductionInstructionDO::getPlanOperationId, instruction.getPlanOperationId());
        queryWrapper.ne(HcProductionInstructionDO::getId, instruction.getId());
        String segmentBatchNo = trimToNull(instruction.getSegmentBatchNo());
        if (StrUtil.isNotBlank(segmentBatchNo)) {
            queryWrapper.and(wrapper -> wrapper.eq(HcProductionInstructionDO::getSegmentBatchNo, segmentBatchNo)
                    .or()
                    .isNull(HcProductionInstructionDO::getSegmentBatchNo)
                    .or()
                    .eq(HcProductionInstructionDO::getSegmentBatchNo, ""));
        }
        HcProductionInstructionDO existed = hcProductionInstructionMapper.selectOne(queryWrapper.last("LIMIT 1 FOR UPDATE"));
        if (existed != null) {
            throw invalidParamException("当前工序已有执行中的换型指令，请先完成后再执行新的换型");
        }
    }

    private void ensureNoOpenChangeoverInstruction(HcProductionInstructionDO instruction) {
        if (!INSTRUCTION_TYPE_CHANGEOVER.equals(instruction.getInstructionType())) {
            return;
        }
        LambdaQueryWrapperX<HcProductionInstructionDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcProductionInstructionDO::getInstructionType, INSTRUCTION_TYPE_CHANGEOVER);
        queryWrapper.in(HcProductionInstructionDO::getExecuteStatus,
                List.of(EXECUTE_STATUS_PENDING, EXECUTE_STATUS_EXECUTING));
        queryWrapper.eq(HcProductionInstructionDO::getPlanOperationId, instruction.getPlanOperationId());
        String segmentBatchNo = trimToNull(instruction.getSegmentBatchNo());
        if (StrUtil.isNotBlank(segmentBatchNo)) {
            queryWrapper.and(wrapper -> wrapper.eq(HcProductionInstructionDO::getSegmentBatchNo, segmentBatchNo)
                    .or()
                    .isNull(HcProductionInstructionDO::getSegmentBatchNo)
                    .or()
                    .eq(HcProductionInstructionDO::getSegmentBatchNo, ""));
        }
        if (hcProductionInstructionMapper.selectOne(queryWrapper) != null) {
            throw invalidParamException("当前工序已有待执行或执行中的换型指令，请先处理后再下达");
        }
    }

    private void normalizeInstructionScope(HcProductionInstructionDO entity, HcProductionInstructionSaveReqVO reqVO) {
        entity.setInstructionType(normalizeInstructionType(entity.getInstructionType()));
        entity.setScopeType(normalizeScopeType(entity.getScopeType(), reqVO));
        entity.setInstructionBatchNo(trimToNull(entity.getInstructionBatchNo()));
        entity.setProductionBatchNo(trimToNull(entity.getProductionBatchNo()));
        entity.setSegmentBatchNo(trimToNull(entity.getSegmentBatchNo()));
        entity.setBatchNo(firstNotBlank(entity.getBatchNo(), entity.getSegmentBatchNo(), entity.getProductionBatchNo()));
    }

    private List<HcPlanOrderOperationDO> resolveActionTargets(HcProductionInstructionDO entity,
            HcProductionInstructionSaveReqVO reqVO) {
        if (!isStatusInstruction(entity.getInstructionType())) {
            return List.of();
        }
        List<HcPlanOrderOperationDO> targets = new ArrayList<>();
        if (reqVO.getOperationIds() != null && !reqVO.getOperationIds().isEmpty()) {
            List<Long> operationIds = reqVO.getOperationIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!operationIds.isEmpty()) {
                targets.addAll(hcPlanOrderOperationMapper.selectBatchIds(operationIds));
            }
            if (targets.size() != operationIds.size()) {
                throw invalidParamException("部分计划工序不存在，请刷新后重试");
            }
        } else if (entity.getPlanOperationId() != null) {
            HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(entity.getPlanOperationId());
            if (operation == null) {
                throw invalidParamException("计划工序不存在");
            }
            targets.add(operation);
        } else if (entity.getPlanId() != null) {
            targets.addAll(hcPlanOrderOperationMapper.selectListByPlanId(entity.getPlanId()));
        }
        if (targets.isEmpty()) {
            throw invalidParamException("没有可执行指令的计划工序");
        }
        fillOperationSnapshot(entity, targets);
        return targets;
    }

    private void fillOperationSnapshot(HcProductionInstructionDO entity, List<HcPlanOrderOperationDO> targets) {
        HcPlanOrderOperationDO first = targets.get(0);
        entity.setPlanId(firstNonNull(entity.getPlanId(), first.getPlanId()));
        entity.setProductionBatchNo(firstNotBlank(entity.getProductionBatchNo(),
                first.getParentProductionBatchNo(), first.getProductionBatchNo(), first.getBatchNo()));
        entity.setSegmentBatchNo(firstNotBlank(entity.getSegmentBatchNo(), first.getProductionBatchNo(), first.getBatchNo()));
        entity.setBatchNo(firstNotBlank(entity.getBatchNo(), entity.getSegmentBatchNo(), entity.getProductionBatchNo()));
        if (SCOPE_TYPE_OPERATION.equals(entity.getScopeType())) {
            if (targets.size() == 1) {
                entity.setPlanOperationId(firstNonNull(entity.getPlanOperationId(), first.getId()));
                entity.setOperationCode(firstNotBlank(entity.getOperationCode(), first.getOpCode()));
                entity.setOperationName(firstNotBlank(entity.getOperationName(), first.getOpName()));
                entity.setProcessCode(firstNotBlank(entity.getProcessCode(), first.getOpCode()));
                entity.setProcessName(firstNotBlank(entity.getProcessName(), first.getOpName()));
                return;
            }
            String names = targets.stream()
                    .map(HcPlanOrderOperationDO::getOpName)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("、"));
            entity.setPlanOperationId(null);
            entity.setOperationName(firstNotBlank(entity.getOperationName(), names));
            entity.setProcessName(firstNotBlank(entity.getProcessName(), names));
        }
    }

    private void applyStatusInstruction(HcProductionInstructionDO instruction, List<HcPlanOrderOperationDO> targets) {
        String targetStatus = targetOperationStatus(instruction.getInstructionType());
        Long operatorId = firstNonNull(instruction.getIssuerId(), SecurityFrameworkUtils.getLoginUserId());
        String operatorName = firstNotBlank(instruction.getIssuerName(), SecurityFrameworkUtils.getLoginUserNickname(), "系统");
        LocalDateTime operateTime = LocalDateTime.now();
        List<HcPlanOrderOperationDO> eligibleTargets = targets.stream()
                .filter(operation -> isEligibleActionTarget(operation.getOperationStatus(), instruction.getInstructionType()))
                .toList();
        if (eligibleTargets.isEmpty()) {
            throw invalidParamException(statusInstructionNoTargetMessage(instruction.getInstructionType()));
        }
        for (HcPlanOrderOperationDO operation : eligibleTargets) {
            String fromStatus = operation.getOperationStatus();
            HcPlanOrderOperationDO updateObj = new HcPlanOrderOperationDO();
            updateObj.setId(operation.getId());
            updateObj.setOperationStatus(targetStatus);
            updateObj.setStatusOperatorId(operatorId);
            updateObj.setStatusOperatorName(operatorName);
            updateObj.setStatusOperateTime(operateTime);
            if (INSTRUCTION_TYPE_PAUSE.equals(instruction.getInstructionType())) {
                updateObj.setPauseRemark(instruction.getInstructionContent());
            } else if (INSTRUCTION_TYPE_CANCEL.equals(instruction.getInstructionType())) {
                updateObj.setCancelReason(instruction.getInstructionContent());
            }
            hcPlanOrderOperationMapper.updateById(updateObj);
            insertOperationStatusLog(operation, instruction, fromStatus, targetStatus, operatorId, operatorName, operateTime);
        }
        if (SCOPE_TYPE_PLAN.equals(instruction.getScopeType())) {
            syncPlanStatus(instruction, operatorId, operatorName, operateTime);
        }
    }

    private void insertOperationStatusLog(HcPlanOrderOperationDO operation, HcProductionInstructionDO instruction,
            String fromStatus, String toStatus, Long operatorId, String operatorName, LocalDateTime operateTime) {
        HcPlanOrderOperationStatusLogDO log = new HcPlanOrderOperationStatusLogDO();
        log.setPlanId(operation.getPlanId());
        log.setPlanOperationId(operation.getId());
        log.setActionType(instruction.getInstructionType());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setReasonRemark(instruction.getInstructionContent());
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperateTime(operateTime);
        hcPlanOrderOperationStatusLogMapper.insert(log);
    }

    private void syncPlanStatus(HcProductionInstructionDO instruction, Long operatorId, String operatorName,
            LocalDateTime operateTime) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(instruction.getPlanId());
        if (planOrder == null) {
            throw invalidParamException("生产计划不存在");
        }
        String targetStatus = targetPlanStatus(instruction.getInstructionType());
        if (Objects.equals(planOrder.getPlanStatus(), targetStatus)) {
            return;
        }
        HcPlanOrderDO updateObj = new HcPlanOrderDO();
        updateObj.setId(planOrder.getId());
        updateObj.setPlanStatus(targetStatus);
        updateObj.setStatusOperatorId(operatorId);
        updateObj.setStatusOperatorName(operatorName);
        updateObj.setStatusOperateTime(operateTime);
        updateObj.setStatusRemark(instruction.getInstructionContent());
        if (PLAN_STATUS_RELEASED.equals(targetStatus) && planOrder.getReleasedAt() == null) {
            updateObj.setReleasedAt(operateTime);
        }
        hcPlanOrderMapper.updateById(updateObj);

        HcPlanOrderStatusLogDO log = new HcPlanOrderStatusLogDO();
        log.setPlanId(planOrder.getId());
        log.setPlanNo(planOrder.getPlanNo());
        log.setActionType(instruction.getInstructionType());
        log.setFromStatus(planOrder.getPlanStatus());
        log.setToStatus(targetStatus);
        log.setReasonRemark(instruction.getInstructionContent());
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperateTime(operateTime);
        hcPlanOrderStatusLogMapper.insert(log);
    }

    private String normalizeInstructionType(String instructionType) {
        String value = firstNotBlank(instructionType, INSTRUCTION_TYPE_DAILY);
        value = value.toUpperCase(Locale.ROOT);
        if (List.of(INSTRUCTION_TYPE_DAILY, INSTRUCTION_TYPE_PAUSE, INSTRUCTION_TYPE_RESUME,
                INSTRUCTION_TYPE_CANCEL, INSTRUCTION_TYPE_CHANGEOVER, INSTRUCTION_TYPE_FREEZE_STOCK,
                INSTRUCTION_TYPE_UNFREEZE_STOCK)
                .contains(value)) {
            return value;
        }
        throw invalidParamException("指令类型不支持");
    }

    private void validateInventoryControlInstruction(HcProductionInstructionDO instruction) {
        if (!List.of(INSTRUCTION_TYPE_FREEZE_STOCK, INSTRUCTION_TYPE_UNFREEZE_STOCK)
                .contains(instruction.getInstructionType())) {
            return;
        }
        if (instruction.getPlanId() == null || instruction.getPlanOperationId() == null) {
            throw invalidParamException("冻结或解冻指令必须指定计划和计划工序");
        }
        instruction.setScopeType(SCOPE_TYPE_OPERATION);
        if (StrUtil.isBlank(instruction.getInstructionContent())) {
            instruction.setInstructionContent(INSTRUCTION_TYPE_FREEZE_STOCK.equals(instruction.getInstructionType())
                    ? "冻结后新报工产出进入待上架冻结品" : "解冻冻结库存：合格品自动退中间边库，NG 退待上架不合格品");
        }
    }

    private void validateInventoryControlInstructionImmutable(HcProductionInstructionDO instruction) {
        if (instruction != null && List.of(INSTRUCTION_TYPE_FREEZE_STOCK, INSTRUCTION_TYPE_UNFREEZE_STOCK)
                .contains(instruction.getInstructionType())) {
            throw invalidParamException("已下达的冻结或解冻指令不可修改、删除或撤下；请下达相反指令调整库存状态");
        }
    }

    private String normalizeScopeType(String scopeType, HcProductionInstructionSaveReqVO reqVO) {
        String value = firstNotBlank(scopeType);
        if (value == null) {
            value = reqVO.getPlanOperationId() != null
                    || (reqVO.getOperationIds() != null && !reqVO.getOperationIds().isEmpty())
                    ? SCOPE_TYPE_OPERATION : SCOPE_TYPE_PLAN;
        }
        value = value.toUpperCase(Locale.ROOT);
        if (List.of(SCOPE_TYPE_PLAN, SCOPE_TYPE_OPERATION, SCOPE_TYPE_SEGMENT).contains(value)) {
            return value;
        }
        throw invalidParamException("指令范围不支持");
    }

    private boolean isStatusInstruction(String instructionType) {
        return INSTRUCTION_TYPE_PAUSE.equals(instructionType)
                || INSTRUCTION_TYPE_RESUME.equals(instructionType)
                || INSTRUCTION_TYPE_CANCEL.equals(instructionType);
    }

    private String targetOperationStatus(String instructionType) {
        if (INSTRUCTION_TYPE_PAUSE.equals(instructionType)) {
            return OP_STATUS_PAUSED;
        }
        if (INSTRUCTION_TYPE_RESUME.equals(instructionType)) {
            return OP_STATUS_RELEASED;
        }
        if (INSTRUCTION_TYPE_CANCEL.equals(instructionType)) {
            return OP_STATUS_CANCELLED;
        }
        throw invalidParamException("指令类型不支持状态更新");
    }

    private String targetPlanStatus(String instructionType) {
        if (INSTRUCTION_TYPE_PAUSE.equals(instructionType)) {
            return PLAN_STATUS_PAUSED;
        }
        if (INSTRUCTION_TYPE_RESUME.equals(instructionType)) {
            return PLAN_STATUS_RELEASED;
        }
        if (INSTRUCTION_TYPE_CANCEL.equals(instructionType)) {
            return PLAN_STATUS_CANCELLED;
        }
        throw invalidParamException("指令类型不支持计划状态更新");
    }

    private boolean isEligibleActionTarget(String currentStatus, String instructionType) {
        if (INSTRUCTION_TYPE_PAUSE.equals(instructionType)) {
            return !List.of(OP_STATUS_PAUSED, OP_STATUS_FINISHED, OP_STATUS_CANCELLED).contains(currentStatus);
        }
        if (INSTRUCTION_TYPE_RESUME.equals(instructionType)) {
            return OP_STATUS_PAUSED.equals(currentStatus);
        }
        if (INSTRUCTION_TYPE_CANCEL.equals(instructionType)) {
            return !List.of(OP_STATUS_FINISHED, OP_STATUS_CANCELLED).contains(currentStatus);
        }
        return false;
    }

    private String statusInstructionNoTargetMessage(String instructionType) {
        if (INSTRUCTION_TYPE_RESUME.equals(instructionType)) {
            return "没有暂停状态的工序可复工";
        }
        if (INSTRUCTION_TYPE_CANCEL.equals(instructionType)) {
            return "没有可作废取消的工序";
        }
        return "没有可暂停的工序";
    }

    private void normalizeMessagePageReq(HcProductionInstructionMessagePageReqVO reqVO) {
        if (reqVO == null) {
            throw invalidParamException("查询条件不能为空");
        }
        reqVO.setPlanNo(trimToNull(reqVO.getPlanNo()));
        reqVO.setBatchNo(trimToNull(reqVO.getBatchNo()));
        reqVO.setProcessCode(trimToNull(reqVO.getProcessCode()));
        reqVO.setProcessName(trimToNull(reqVO.getProcessName()));
        reqVO.setOperationCode(trimToNull(reqVO.getOperationCode()));
        reqVO.setOperationName(trimToNull(reqVO.getOperationName()));
        reqVO.setInstructionType(trimToUpper(reqVO.getInstructionType()));
        reqVO.setStatus(trimToUpper(reqVO.getStatus()));
        reqVO.setKeyword(trimToNull(reqVO.getKeyword()));
        reqVO.setReadStatus(null);
    }

    private Long getRequiredLoginUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw invalidParamException("请先登录后再查看生产指令消息");
        }
        return loginUserId;
    }

    private void markCurrentRecipientRead(Long instructionId) {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (instructionId == null || loginUserId == null) {
            return;
        }
        HcProductionInstructionRecipientDO recipient =
                hcProductionInstructionRecipientMapper.selectOneByInstructionIdAndRecipientId(instructionId, loginUserId);
        if (recipient == null || RECIPIENT_NOTIFY_READ.equals(recipient.getNotifyStatus())) {
            return;
        }
        HcProductionInstructionRecipientDO updateObj = new HcProductionInstructionRecipientDO();
        updateObj.setId(recipient.getId());
        updateObj.setNotifyStatus(RECIPIENT_NOTIFY_READ);
        updateObj.setNotifyTime(LocalDateTime.now());
        hcProductionInstructionRecipientMapper.updateById(updateObj);
    }

    private String generateInstructionNo() {
        String datePart = LocalDate.now().format(INSTRUCTION_NO_DATE_FORMATTER);
        String millisPart = String.valueOf(System.currentTimeMillis());
        return "PI" + datePart + millisPart.substring(Math.max(0, millisPart.length() - 6));
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        return StrUtil.blankToDefault(value == null ? null : value.trim(), null);
    }

    private static boolean sameText(String left, String right) {
        String normalizedLeft = trimToUpper(left);
        String normalizedRight = trimToUpper(right);
        return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
    }

    private static String trimToUpper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

}
