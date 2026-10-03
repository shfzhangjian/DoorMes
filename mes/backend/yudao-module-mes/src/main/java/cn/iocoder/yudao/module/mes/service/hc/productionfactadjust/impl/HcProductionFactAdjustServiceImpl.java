package cn.iocoder.yudao.module.mes.service.hc.productionfactadjust.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.CreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.DetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.InstructionOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.OperationOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.OrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.PreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.ProductOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO.SegmentOptionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionfactadjust.HcProductionFactAdjustDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productionfactadjust.HcProductionFactAdjustOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust.HcProductionFactAdjustAdhesive2Row;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust.HcProductionFactAdjustDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust.HcProductionFactAdjustExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust.HcProductionFactAdjustImpactRow;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust.HcProductionFactAdjustOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust.HcProductionFactAdjustProductRow;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust.HcProductionFactAdjustScopeRow;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionMapper;
import cn.iocoder.yudao.module.mes.service.hc.productionfactadjust.HcProductionFactAdjustService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * 生产事实调账服务。
 *
 * <p>首期只允许在粘胶2换型指令仍处于执行中、且后续质量/包装节点未锁定时，回修因提前扫码造成的产品快照偏差。
 * 计划主数据及不可变库存流水均不在本服务的更新范围内。</p>
 */
@Service
@Validated
public class HcProductionFactAdjustServiceImpl implements HcProductionFactAdjustService {

    private static final String ADJUST_TYPE = "CHANGEOVER_PRODUCT_SNAPSHOT";
    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String OPERATION_ADHESIVE2 = "ADHESIVE2";
    private static final String INSTRUCTION_CHANGEOVER = "CHANGEOVER";
    private static final String INSTRUCTION_CONFIRMED = "CONFIRMED";
    private static final String EXECUTE_EXECUTING = "EXECUTING";
    private static final String EXECUTE_COMPLETED = "COMPLETED";
    private static final DateTimeFormatter ORDER_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private HcProductionFactAdjustExecutionMapper executionMapper;
    @Resource
    private HcProductionFactAdjustOrderMapper orderMapper;
    @Resource
    private HcProductionFactAdjustDetailMapper detailMapper;
    @Resource
    private HcProductionInstructionMapper instructionMapper;

    @Override
    public List<OperationOptionRespVO> getOperationOptions(String planNo) {
        if (StrUtil.isBlank(planNo)) {
            return List.of();
        }
        return executionMapper.selectOperationOptionList(planNo.trim());
    }

    @Override
    public List<SegmentOptionRespVO> getSegmentOptions(Long planOperationId) {
        if (planOperationId == null) {
            return List.of();
        }
        return executionMapper.selectSegmentOptionList(planOperationId);
    }

    @Override
    public List<ProductOptionRespVO> getProductOptions(String keyword) {
        return BeanUtils.toBean(executionMapper.selectTargetProductList(StrUtil.trim(keyword)), ProductOptionRespVO.class);
    }

    @Override
    public List<InstructionOptionRespVO> getInstructionOptions(Long planOperationId, String segmentBatchNo) {
        if (planOperationId == null || StrUtil.isBlank(segmentBatchNo)) {
            return List.of();
        }
        return executionMapper.selectInstructionOptionList(planOperationId, segmentBatchNo.trim());
    }

    @Override
    public PreviewRespVO preview(PreviewReqVO reqVO) {
        return buildPreview(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateReqVO reqVO) {
        PreviewRespVO preview = buildPreview(reqVO);
        ensureEligible(preview);
        Long loginUserId = requireLoginUserId();
        String loginUserName = loginUserName();
        HcProductionFactAdjustOrderDO entity = new HcProductionFactAdjustOrderDO();
        entity.setAdjustNo(generateAdjustNo());
        entity.setAdjustType(ADJUST_TYPE);
        entity.setStatus(STATUS_PENDING_APPROVAL);
        entity.setPlanId(preview.getPlanId());
        entity.setPlanNo(preview.getPlanNo());
        entity.setPlanOperationId(preview.getPlanOperationId());
        entity.setOperationCode(preview.getOperationCode());
        entity.setOperationName(preview.getOperationName());
        entity.setSegmentBatchNo(preview.getSegmentBatchNo());
        entity.setInstructionId(preview.getInstructionId());
        entity.setInstructionNo(preview.getInstructionNo());
        HcProductionFactAdjustProductRow source = executionMapper.selectTargetProduct(preview.getSourceModelCode(), preview.getSourceMaterialCode());
        entity.setSourceProductModelId(source == null ? null : source.getProductModelId());
        entity.setSourceModelCode(preview.getSourceModelCode());
        entity.setSourceMaterialId(source == null ? null : source.getMaterialId());
        entity.setSourceMaterialCode(preview.getSourceMaterialCode());
        entity.setSourceMaterialName(preview.getSourceMaterialName());
        ProductOptionRespVO target = preview.getTargetProduct();
        entity.setTargetProductModelId(target.getProductModelId());
        entity.setTargetModelCode(target.getModelCode());
        entity.setTargetMaterialId(target.getMaterialId());
        entity.setTargetMaterialCode(target.getMaterialCode());
        entity.setTargetMaterialName(target.getMaterialName());
        entity.setTargetSpecification(target.getSpecification());
        entity.setAdjustReason(reqVO.getAdjustReason().trim());
        entity.setEvidenceRemark(StrUtil.trim(reqVO.getEvidenceRemark()));
        entity.setImpactSummaryJson(JsonUtils.toJsonString(preview));
        entity.setBeforeSnapshotJson(JsonUtils.toJsonString(Map.of("preview", preview)));
        entity.setApplicantId(loginUserId);
        entity.setApplicantName(loginUserName);
        entity.setAppliedTime(LocalDateTime.now());
        entity.setTenantId(TenantContextHolder.getTenantId());
        orderMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(ApproveReqVO reqVO) {
        HcProductionFactAdjustOrderDO order = requireOrder(reqVO.getId());
        if (!STATUS_PENDING_APPROVAL.equals(order.getStatus())) {
            throw invalidParamException("仅待审核的生产事实调账单允许审核");
        }
        Long loginUserId = requireLoginUserId();
        if (Objects.equals(order.getApplicantId(), loginUserId)) {
            throw invalidParamException("申请人不能审核本人发起的调账单");
        }
        HcProductionFactAdjustOrderDO update = new HcProductionFactAdjustOrderDO();
        update.setId(order.getId());
        update.setStatus(Boolean.TRUE.equals(reqVO.getApproved()) ? STATUS_APPROVED : STATUS_REJECTED);
        update.setApproverId(loginUserId);
        update.setApproverName(loginUserName());
        update.setApprovedTime(LocalDateTime.now());
        update.setApproveRemark(StrUtil.trim(reqVO.getApproveRemark()));
        orderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(ExecuteReqVO reqVO) {
        HcProductionFactAdjustOrderDO order = requireOrder(reqVO.getId());
        if (!STATUS_APPROVED.equals(order.getStatus())) {
            throw invalidParamException("仅审核通过的生产事实调账单允许执行");
        }
        PreviewReqVO previewReq = new PreviewReqVO();
        previewReq.setPlanNo(order.getPlanNo());
        previewReq.setPlanOperationId(order.getPlanOperationId());
        previewReq.setSegmentBatchNo(order.getSegmentBatchNo());
        previewReq.setInstructionId(order.getInstructionId());
        previewReq.setTargetModelCode(order.getTargetModelCode());
        previewReq.setTargetMaterialCode(order.getTargetMaterialCode());
        PreviewRespVO preview = buildPreview(previewReq);
        ensureEligible(preview);

        HcProductionFactAdjustProductRow target = executionMapper.selectTargetProduct(order.getTargetModelCode(), order.getTargetMaterialCode());
        if (target == null) {
            throw invalidParamException("目标型号与料号组合已失效，请重新发起调账单");
        }
        List<HcProductionFactAdjustAdhesive2Row> rows = executionMapper.selectAdhesive2Rows(
                order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo());
        HcProductionInstructionDO instruction = requireActiveInstruction(order, rows.size());
        String executorName = loginUserName();
        String mark = buildMark(order.getAdjustNo());
        executionMapper.updateAdhesive2Reports(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(), target,
                order.getInstructionId(), order.getInstructionNo(), order.getAdjustNo(), mark, executorName);
        executionMapper.updateCutRoundReports(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(), target,
                order.getInstructionId(), order.getInstructionNo(), order.getAdjustNo(), mark, executorName);
        executionMapper.updateOutputStocks(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(), target, mark, executorName);
        executionMapper.updateCutInspectionDetails(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(), target, mark, executorName);
        executionMapper.updateFqcSubmissionDetails(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(), target, mark, executorName);
        executionMapper.updateFqcOrders(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(), target, mark, executorName);
        executionMapper.updateFaiOrders(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(), target, mark, executorName);
        executionMapper.updateProcessForms(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(), target,
                order.getAdjustNo(), mark, executorName);
        executionMapper.insertChangeoverPieces(order.getPlanId(), order.getPlanOperationId(), order.getSegmentBatchNo(),
                order.getInstructionId(), target, mark, executorName);
        insertDetailRows(order, rows, target, executorName);
        completeInstruction(instruction, rows.size(), executorName);

        HcProductionFactAdjustOrderDO update = new HcProductionFactAdjustOrderDO();
        update.setId(order.getId());
        update.setStatus(STATUS_EXECUTED);
        update.setExecutorId(requireLoginUserId());
        update.setExecutorName(executorName);
        update.setExecutedTime(LocalDateTime.now());
        update.setExecutionRemark(StrUtil.trim(reqVO.getExecutionRemark()));
        update.setAfterSnapshotJson(JsonUtils.toJsonString(Map.of(
                "targetModelCode", target.getModelCode(), "targetMaterialCode", target.getMaterialCode(),
                "executedPieceCount", rows.size(), "executedTime", update.getExecutedTime().toString())));
        orderMapper.updateById(update);
    }

    @Override
    public PageResult<OrderRespVO> getPage(PageReqVO reqVO) {
        return BeanUtils.toBean(orderMapper.selectPage(reqVO), OrderRespVO.class);
    }

    @Override
    public List<DetailRespVO> getDetailList(Long orderId) {
        requireOrder(orderId);
        return BeanUtils.toBean(detailMapper.selectByOrderId(orderId), DetailRespVO.class);
    }

    private PreviewRespVO buildPreview(PreviewReqVO reqVO) {
        HcProductionFactAdjustScopeRow scope = executionMapper.selectScope(reqVO.getPlanNo().trim(), reqVO.getPlanOperationId());
        if (scope == null) {
            throw invalidParamException("计划号与计划工序不匹配");
        }
        if (!OPERATION_ADHESIVE2.equals(scope.getOperationCode())) {
            throw invalidParamException("首期生产事实调账仅支持粘胶2计划工序");
        }
        HcProductionFactAdjustProductRow source = executionMapper.selectSourceProduct(
                scope.getPlanId(), scope.getPlanOperationId(), reqVO.getSegmentBatchNo().trim());
        HcProductionFactAdjustProductRow target = executionMapper.selectTargetProduct(reqVO.getTargetModelCode().trim(), reqVO.getTargetMaterialCode().trim());
        HcProductionFactAdjustImpactRow impact = executionMapper.selectImpact(
                scope.getPlanId(), scope.getPlanOperationId(), reqVO.getSegmentBatchNo().trim());
        HcProductionInstructionDO instruction = instructionMapper.selectById(reqVO.getInstructionId());
        List<String> blockers = new ArrayList<>();
        validateInstruction(scope, reqVO.getSegmentBatchNo().trim(), target, instruction, blockers);
        if (source == null || zero(source.getVariantCount()) == 0) {
            blockers.add("当前范围不存在已提交的粘胶2报工记录");
        } else if (source.getVariantCount() > 1) {
            blockers.add("当前范围存在多个原产品快照，需拆分范围后调账");
        }
        if (target == null) {
            blockers.add("目标型号与料号不是启用的有效组合");
        }
        if (source != null && target != null && Objects.equals(source.getModelCode(), target.getModelCode())
                && Objects.equals(source.getMaterialCode(), target.getMaterialCode())) {
            blockers.add("目标产品与当前产品快照一致，无需调账");
        }
        if (zero(impact.getPackagingCount()) > 0 || zero(impact.getFinishedStockCount()) > 0) {
            blockers.add("已进入包装或成品入库，首期不允许自动回修");
        }
        if (zero(impact.getUnsafeFqcOrderCount()) > 0 || zero(impact.getUnsafeFaiOrderCount()) > 0) {
            blockers.add("存在已锁定或已录入的质量单据，首期不允许自动回修");
        }

        PreviewRespVO result = new PreviewRespVO();
        result.setPlanId(scope.getPlanId());
        result.setPlanNo(scope.getPlanNo());
        result.setPlanOperationId(scope.getPlanOperationId());
        result.setOperationCode(scope.getOperationCode());
        result.setOperationName(scope.getOperationName());
        result.setSegmentBatchNo(reqVO.getSegmentBatchNo().trim());
        result.setInstructionId(reqVO.getInstructionId());
        result.setInstructionNo(instruction == null ? null : instruction.getInstructionNo());
        result.setSourceModelCode(source == null ? null : source.getModelCode());
        result.setSourceMaterialCode(source == null ? null : source.getMaterialCode());
        result.setSourceMaterialName(source == null ? null : source.getMaterialName());
        result.setTargetProduct(BeanUtils.toBean(target, ProductOptionRespVO.class));
        result.setAdhesive2ReportCount(zero(impact.getAdhesive2ReportCount()));
        result.setCutRoundReportCount(zero(impact.getCutRoundReportCount()));
        result.setOutputStockCount(zero(impact.getOutputStockCount()));
        result.setCutInspectionDetailCount(zero(impact.getCutInspectionDetailCount()));
        result.setFqcOrderCount(zero(impact.getFqcOrderCount()));
        result.setFqcSubmissionDetailCount(zero(impact.getFqcSubmissionDetailCount()));
        result.setFaiOrderCount(zero(impact.getFaiOrderCount()));
        result.setProcessFormCount(zero(impact.getProcessFormCount()));
        result.setPackagingCount(zero(impact.getPackagingCount()));
        result.setFinishedStockCount(zero(impact.getFinishedStockCount()));
        result.setUnsafeFqcOrderCount(zero(impact.getUnsafeFqcOrderCount()));
        result.setUnsafeFaiOrderCount(zero(impact.getUnsafeFaiOrderCount()));
        result.setBlockingReasons(blockers);
        result.setEligible(blockers.isEmpty());
        return result;
    }

    private void validateInstruction(HcProductionFactAdjustScopeRow scope, String segmentBatchNo,
                                     HcProductionFactAdjustProductRow target, HcProductionInstructionDO instruction,
                                     List<String> blockers) {
        if (instruction == null || Boolean.TRUE.equals(instruction.getDeleted())) {
            blockers.add("换型指令不存在");
            return;
        }
        if (!INSTRUCTION_CHANGEOVER.equals(instruction.getInstructionType()) || !INSTRUCTION_CONFIRMED.equals(instruction.getStatus())) {
            blockers.add("所选指令不是已确认的换型指令");
        }
        if (!Objects.equals(scope.getPlanId(), instruction.getPlanId())
                || !Objects.equals(scope.getPlanOperationId(), instruction.getPlanOperationId())
                || !Objects.equals(segmentBatchNo, instruction.getSegmentBatchNo())) {
            blockers.add("换型指令与计划工序或分段批号不匹配");
        }
        if (!EXECUTE_EXECUTING.equals(instruction.getExecuteStatus()) || zero(instruction.getCompletedQty()) != 0) {
            blockers.add("换型指令必须处于执行中且尚未记录换型后片数");
        }
        if (target != null && (!Objects.equals(target.getModelCode(), instruction.getTargetModelCode())
                || !Objects.equals(target.getMaterialCode(), instruction.getTargetMaterialCode()))) {
            blockers.add("目标型号、料号必须与换型指令完全一致");
        }
    }

    private HcProductionInstructionDO requireActiveInstruction(HcProductionFactAdjustOrderDO order, int reportCount) {
        HcProductionInstructionDO instruction = instructionMapper.selectById(order.getInstructionId());
        if (instruction == null || !EXECUTE_EXECUTING.equals(instruction.getExecuteStatus())
                || zero(instruction.getCompletedQty()) != 0 || !INSTRUCTION_CONFIRMED.equals(instruction.getStatus())) {
            throw invalidParamException("换型指令状态已变化，请重新预览后发起调账");
        }
        if (!Objects.equals(instruction.getTargetModelCode(), order.getTargetModelCode())
                || !Objects.equals(instruction.getTargetMaterialCode(), order.getTargetMaterialCode())
                || !Objects.equals(instruction.getPlanId(), order.getPlanId())
                || !Objects.equals(instruction.getPlanOperationId(), order.getPlanOperationId())
                || !Objects.equals(instruction.getSegmentBatchNo(), order.getSegmentBatchNo())) {
            throw invalidParamException("换型指令范围或目标已变化，请重新发起调账");
        }
        if (instruction.getTargetQty() == null || instruction.getTargetQty() != reportCount) {
            throw invalidParamException("换型指令目标数量与待回修粘胶2报工数量不一致");
        }
        return instruction;
    }

    private void insertDetailRows(HcProductionFactAdjustOrderDO order, List<HcProductionFactAdjustAdhesive2Row> rows,
                                  HcProductionFactAdjustProductRow target, String executorName) {
        int index = 1;
        for (HcProductionFactAdjustAdhesive2Row row : rows) {
            HcProductionFactAdjustDetailDO detail = new HcProductionFactAdjustDetailDO();
            detail.setAdjustOrderId(order.getId());
            detail.setSeqNo(index++);
            detail.setAdhesive2ReportId(row.getId());
            detail.setProductionBatchNo(row.getProductionBatchNo());
            detail.setOldModelCode(row.getModelCode());
            detail.setOldMaterialCode(row.getMaterialCode());
            detail.setNewModelCode(target.getModelCode());
            detail.setNewMaterialCode(target.getMaterialCode());
            detail.setExecutionStatus(STATUS_EXECUTED);
            detail.setExecutionRemark("已按换型指令回修产品快照；执行人：" + executorName);
            detail.setTenantId(row.getTenantId());
            detailMapper.insert(detail);
        }
    }

    private void completeInstruction(HcProductionInstructionDO instruction, int completedQty, String executorName) {
        HcProductionInstructionDO update = new HcProductionInstructionDO();
        update.setId(instruction.getId());
        update.setCompletedQty(completedQty);
        update.setExecuteStatus(EXECUTE_COMPLETED);
        update.setExecuteEndTime(LocalDateTime.now());
        update.setExecuteUserId(instruction.getExecuteUserId() == null ? requireLoginUserId() : instruction.getExecuteUserId());
        update.setExecuteUserName(StrUtil.blankToDefault(instruction.getExecuteUserName(), executorName));
        instructionMapper.updateById(update);
    }

    private HcProductionFactAdjustOrderDO requireOrder(Long id) {
        HcProductionFactAdjustOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw invalidParamException("生产事实调账单不存在");
        }
        return order;
    }

    private void ensureEligible(PreviewRespVO preview) {
        if (!preview.isEligible()) {
            throw invalidParamException("当前范围不满足调账条件：" + String.join("；", preview.getBlockingReasons()));
        }
    }

    private Long requireLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw invalidParamException("请先登录后再执行生产事实调账");
        }
        return userId;
    }

    private String loginUserName() {
        return StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    private String generateAdjustNo() {
        return "PFA" + LocalDateTime.now().format(ORDER_NO_TIME_FORMATTER) + String.format("%03d", (int) (Math.random() * 1000));
    }

    private String buildMark(String adjustNo) {
        return "生产事实调账[" + adjustNo + "]：换型后实物已确认，回修产品快照";
    }

    private static int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
