package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewPlanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewPlanRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewMonthPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewParticipantDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewPlanLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewReplyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewStatusLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierReviewYearPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierReviewMonthPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierReviewParticipantMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierReviewPlanLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierReviewReplyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierReviewStatusLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmSupplierReviewYearPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_REVIEW_EXECUTION_VIEW_ALL_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_REVIEW_MONTH_PLAN_CLEAR_NOT_ALLOWED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_REVIEW_MONTH_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_REVIEW_PLAN_DELETE_FORBIDDEN;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_REVIEW_PLAN_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_REVIEW_PLAN_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_SUPPLIER_REVIEW_PLAN_SUPPLIER_REQUIRED;

@Service
@Validated
public class SrmSupplierReviewPlanServiceImpl implements SrmSupplierReviewPlanService {

    public static final String STATUS_PLAN = "PLAN";
    public static final String STATUS_EXECUTING = "EXECUTING";
    public static final String STATUS_CHANGED = "CHANGED";
    public static final String STATUS_CANCELED = "CANCELED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** 查询范围：我相关的计划（默认） */
    public static final String SCOPE_MINE = "MINE";
    /** 查询范围：全部计划（需权限 mes:srm-audit-review-record:view-all） */
    public static final String SCOPE_ALL = "ALL";

    private static final Set<String> VALID_STATUSES = Set.of(
            STATUS_PLAN, STATUS_EXECUTING, STATUS_CHANGED, STATUS_CANCELED, STATUS_COMPLETED, STATUS_ARCHIVED);
    private static final String BIZ_TYPE_REVIEW_PLAN = "SRM_SUPPLIER_REVIEW_PLAN";
    private static final String BIZ_TYPE_REVIEW_REPLY = "SRM_SUPPLIER_REVIEW_REPLY";
    private static final String RELATION_LEAD = "LEAD";
    private static final String RELATION_RELATED = "RELATED";
    private static final String DELETE_ANNUAL_PLAN_PERMISSION = "mes:srm-audit-review-plan:delete-year";
    private static final String VIEW_ALL_REVIEW_RECORD_PERMISSION = "mes:srm-audit-review-record:view-all";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String SRM_SUPPLIER_SUPER_ADMIN_ROLE = "srm_supplier_super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";

    @Resource
    private SrmSupplierReviewYearPlanMapper yearPlanMapper;
    @Resource
    private SrmSupplierReviewPlanLineMapper lineMapper;
    @Resource
    private SrmSupplierReviewMonthPlanMapper monthPlanMapper;
    @Resource
    private SrmSupplierReviewParticipantMapper participantMapper;
    @Resource
    private SrmSupplierReviewReplyMapper replyMapper;
    @Resource
    private SrmSupplierReviewStatusLogMapper statusLogMapper;
    @Resource
    private SrmAttachmentMapper attachmentMapper;
    @Resource
    private MesSupplierMapper supplierMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long initYearPlan(Integer planYear) {
        SrmSupplierReviewYearPlanDO existing = yearPlanMapper.selectByPlanYear(planYear);
        if (existing != null) {
            return existing.getId();
        }
        CurrentUser currentUser = getCurrentUser();
        SrmSupplierReviewYearPlanDO plan = new SrmSupplierReviewYearPlanDO();
        plan.setPlanNo(String.format("SRM-RP-%d", planYear));
        plan.setPlanYear(planYear);
        plan.setPlanTitle(String.format("供应商%d年度评审计划", planYear));
        plan.setPreparedDept(defaultString(currentUser.deptName(), "采购部"));
        plan.setPreparedBy(currentUser.userName());
        plan.setCompletionSummary("未安排");
        plan.setVersion(1);
        yearPlanMapper.insert(plan);
        return plan.getId();
    }

    @Override
    public SrmSupplierReviewPlanRespVO.YearPlan getYearPlan(Integer planYear) {
        SrmSupplierReviewYearPlanDO plan = yearPlanMapper.selectByPlanYear(planYear);
        if (plan == null) {
            SrmSupplierReviewPlanRespVO.YearPlan empty = new SrmSupplierReviewPlanRespVO.YearPlan();
            empty.setPlanYear(planYear);
            empty.setPlanNo(String.format("SRM-RP-%d", planYear));
            empty.setPlanTitle(String.format("供应商%d年度评审计划", planYear));
            empty.setCompletionSummary("未安排");
            empty.setPreparedDept("采购部");
            return empty;
        }
        return buildYearResp(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSupplierLine(SrmSupplierReviewPlanReqVO.AddSupplierLine reqVO) {
        Long yearPlanId = initYearPlan(reqVO.getPlanYear());
        SrmSupplierReviewYearPlanDO plan = yearPlanMapper.selectById(yearPlanId);
        MesSupplierDO supplier = resolveSupplier(reqVO);
        if (supplier == null && StrUtil.isBlank(reqVO.getSupplierCode()) && StrUtil.isBlank(reqVO.getSupplierName())) {
            throw exception(SRM_SUPPLIER_REVIEW_PLAN_SUPPLIER_REQUIRED);
        }

        Long supplierId = supplier == null ? reqVO.getSupplierId() : supplier.getId();
        String supplierCode = firstNotBlank(reqVO.getSupplierCode(), supplier == null ? null : supplier.getSupplierCode());
        SrmSupplierReviewPlanLineDO duplicate = lineMapper.selectDuplicate(yearPlanId, supplierId, supplierCode);
        if (duplicate != null) {
            return duplicate.getId();
        }

        SrmSupplierReviewPlanLineDO line = new SrmSupplierReviewPlanLineDO();
        line.setYearPlanId(yearPlanId);
        line.setPlanYear(plan.getPlanYear());
        line.setRowNo(lineMapper.selectNextRowNo(yearPlanId));
        fillLineSupplier(line, reqVO, supplier);
        line.setCompletionStatus("未安排");
        line.setVersion(1);
        lineMapper.insert(line);

        for (int month = 1; month <= 12; month++) {
            SrmSupplierReviewMonthPlanDO monthPlan = new SrmSupplierReviewMonthPlanDO();
            monthPlan.setYearPlanId(yearPlanId);
            monthPlan.setLineId(line.getId());
            monthPlan.setPlanYear(plan.getPlanYear());
            monthPlan.setPlanMonth(month);
            monthPlan.setPlannedFlag(false);
            monthPlan.setVersion(1);
            monthPlanMapper.insert(monthPlan);
        }
        refreshYearAndLineSummary(yearPlanId, line.getId());
        return line.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLineContact(SrmSupplierReviewPlanReqVO.UpdateLineContact reqVO) {
        SrmSupplierReviewPlanLineDO line = lineMapper.selectById(reqVO.getId());
        if (line == null) {
            throw exception(SRM_SUPPLIER_REVIEW_PLAN_NOT_EXISTS);
        }
        for (SrmSupplierReviewPlanLineDO sameSupplierLine : selectSameSupplierLines(line)) {
            SrmSupplierReviewPlanLineDO update = new SrmSupplierReviewPlanLineDO();
            update.setId(sameSupplierLine.getId());
            update.setContactPerson(reqVO.getContactPerson());
            update.setRemark(reqVO.getRemark());
            lineMapper.updateById(update);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SrmSupplierReviewPlanRespVO.DeleteAnnualPlansResult deleteAnnualPlans(
            SrmSupplierReviewPlanReqVO.DeleteAnnualPlans reqVO) {
        assertDeleteAnnualPlanPermission();
        SrmSupplierReviewPlanLineDO line = lineMapper.selectById(reqVO.getLineId());
        if (line == null || (line.getPlanYear() != null && !Objects.equals(line.getPlanYear(), reqVO.getPlanYear()))) {
            return buildDeleteAnnualResult(reqVO.getLineId(), reqVO.getPlanYear(), null, null, null,
                    0, 0, 0, 0, 0, 0);
        }

        Integer planYear = line.getPlanYear() == null ? reqVO.getPlanYear() : line.getPlanYear();
        Long yearPlanId = line.getYearPlanId();
        List<Long> lineIds = selectSameSupplierLines(line).stream()
                .map(SrmSupplierReviewPlanLineDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<SrmSupplierReviewMonthPlanDO> months = monthPlanMapper.selectListByLineIds(lineIds);
        List<Long> monthIds = months.stream().map(SrmSupplierReviewMonthPlanDO::getId).toList();

        int deletedParticipantCount = participantMapper.deleteByMonthPlanIds(monthIds);
        int deletedReplyCount = replyMapper.deleteByMonthPlanIds(monthIds);
        int deletedStatusLogCount = statusLogMapper.deleteByMonthPlanIds(monthIds);
        int deletedAttachmentCount = attachmentMapper.deleteByBizIds(BIZ_TYPE_REVIEW_PLAN, monthIds)
                + attachmentMapper.deleteByBizIds(BIZ_TYPE_REVIEW_REPLY, monthIds);
        int deletedMonthCount = monthPlanMapper.deleteByLineIds(lineIds);
        int deletedLineCount = 0;
        for (Long lineId : lineIds) {
            deletedLineCount += lineMapper.deleteById(lineId);
        }
        if (yearPlanId != null) {
            refreshYearSummary(yearPlanId);
        }
        return buildDeleteAnnualResult(line.getId(), planYear, line.getSupplierCode(), line.getMaterialCode(),
                yearPlanId,
                deletedLineCount, deletedMonthCount, deletedParticipantCount, deletedReplyCount,
                deletedStatusLogCount, deletedAttachmentCount);
    }

    @Override
    public SrmSupplierReviewPlanRespVO.MonthPlan getMonthPlan(Long id) {
        SrmSupplierReviewMonthPlanDO monthPlan = monthPlanMapper.selectById(id);
        if (monthPlan == null) {
            throw exception(SRM_SUPPLIER_REVIEW_MONTH_PLAN_NOT_EXISTS);
        }
        return buildMonthResp(monthPlan, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMonthPlan(SrmSupplierReviewPlanReqVO.MonthSave reqVO) {
        SrmSupplierReviewMonthPlanDO old = monthPlanMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(SRM_SUPPLIER_REVIEW_MONTH_PLAN_NOT_EXISTS);
        }
        SrmSupplierReviewMonthPlanDO update = new SrmSupplierReviewMonthPlanDO();
        update.setId(old.getId());
        update.setPlannedFlag(true);
        update.setExecutionStatus(StrUtil.blankToDefault(old.getExecutionStatus(), STATUS_PLAN));
        update.setPlanDesc(reqVO.getPlanDesc());
        update.setLeadUserId(reqVO.getLeadUserId());
        update.setLeadUserName(reqVO.getLeadUserName());
        update.setAuditDate(reqVO.getAuditDate());
        update.setAuditCategory(reqVO.getAuditCategory());
        update.setAuditDesc(reqVO.getAuditDesc());
        update.setAuditAttachment(reqVO.getAuditAttachment());
        update.setApproverUserId(reqVO.getApproverUserId());
        update.setApproverUserName(reqVO.getApproverUserName());
        update.setApprovalOpinion(reqVO.getApprovalOpinion());
        update.setRemark(reqVO.getRemark());
        fillRelatedUsers(update, reqVO.getRelatedUsers());
        monthPlanMapper.updateById(update);
        syncParticipants(old, update, reqVO.getRelatedUsers());
        refreshYearAndLineSummary(old.getYearPlanId(), old.getLineId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSiteInspection(SrmSupplierReviewPlanReqVO.SiteInspectionSave reqVO) {
        SrmSupplierReviewMonthPlanDO old = monthPlanMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(SRM_SUPPLIER_REVIEW_MONTH_PLAN_NOT_EXISTS);
        }
        SrmSupplierReviewMonthPlanDO update = new SrmSupplierReviewMonthPlanDO();
        update.setId(old.getId());
        update.setPlannedFlag(true);
        // 上传现场考察资料视为进入执行状态；已归档/完成/取消的状态保持不变
        update.setExecutionStatus(StrUtil.isBlank(old.getExecutionStatus())
                ? STATUS_EXECUTING
                : old.getExecutionStatus());
        update.setAuditDate(reqVO.getAuditDate());
        update.setAuditCategory(reqVO.getAuditCategory());
        update.setAuditDesc(reqVO.getAuditDesc());
        update.setAuditAttachment(reqVO.getAuditAttachment());
        monthPlanMapper.updateById(update);
        refreshYearAndLineSummary(old.getYearPlanId(), old.getLineId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearMonthPlan(Long id) {
        SrmSupplierReviewMonthPlanDO old = monthPlanMapper.selectById(id);
        if (old == null) {
            throw exception(SRM_SUPPLIER_REVIEW_MONTH_PLAN_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(old.getPlannedFlag())) {
            return;
        }
        if (StrUtil.isNotBlank(old.getExecutionStatus()) && !STATUS_PLAN.equals(old.getExecutionStatus())) {
            throw exception(SRM_SUPPLIER_REVIEW_MONTH_PLAN_CLEAR_NOT_ALLOWED);
        }
        if (CollUtil.isNotEmpty(replyMapper.selectListByMonthPlanId(id))) {
            throw exception(SRM_SUPPLIER_REVIEW_MONTH_PLAN_CLEAR_NOT_ALLOWED);
        }
        monthPlanMapper.clearMonthPlan(id);
        participantMapper.deleteByMonthPlanId(id);
        attachmentMapper.deleteByBiz(BIZ_TYPE_REVIEW_PLAN, id);
        refreshYearAndLineSummary(old.getYearPlanId(), old.getLineId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustMonthStatus(SrmSupplierReviewPlanReqVO.StatusAdjust reqVO) {
        SrmSupplierReviewMonthPlanDO old = monthPlanMapper.selectById(reqVO.getId());
        if (old == null) {
            throw exception(SRM_SUPPLIER_REVIEW_MONTH_PLAN_NOT_EXISTS);
        }
        if (!VALID_STATUSES.contains(reqVO.getExecutionStatus())) {
            throw exception(SRM_SUPPLIER_REVIEW_PLAN_STATUS_INVALID, reqVO.getExecutionStatus());
        }
        SrmSupplierReviewMonthPlanDO update = new SrmSupplierReviewMonthPlanDO();
        update.setId(old.getId());
        update.setPlannedFlag(true);
        update.setExecutionStatus(reqVO.getExecutionStatus());
        update.setStatusRemark(reqVO.getStatusRemark());
        update.setUpdateDescription(reqVO.getUpdateDescription());
        monthPlanMapper.updateById(update);
        insertStatusLog(old, reqVO.getExecutionStatus(), reqVO.getStatusRemark(), reqVO.getUpdateDescription());
        refreshYearAndLineSummary(old.getYearPlanId(), old.getLineId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReply(SrmSupplierReviewPlanReqVO.ReplyCreate reqVO) {
        SrmSupplierReviewMonthPlanDO monthPlan = monthPlanMapper.selectById(reqVO.getMonthPlanId());
        if (monthPlan == null) {
            throw exception(SRM_SUPPLIER_REVIEW_MONTH_PLAN_NOT_EXISTS);
        }
        CurrentUser currentUser = getCurrentUser();
        SrmSupplierReviewReplyDO reply = new SrmSupplierReviewReplyDO();
        reply.setYearPlanId(monthPlan.getYearPlanId());
        reply.setLineId(monthPlan.getLineId());
        reply.setMonthPlanId(monthPlan.getId());
        reply.setPlanYear(monthPlan.getPlanYear());
        reply.setPlanMonth(monthPlan.getPlanMonth());
        reply.setReplyTime(LocalDateTime.now());
        reply.setReviewDate(reqVO.getReviewDate());
        reply.setRecorderUserId(currentUser.userId());
        reply.setRecorderUserName(defaultString(reqVO.getRecorderUserName(),
                currentUser.userName()));
        reply.setReviewResult(reqVO.getReviewResult());
        reply.setRemark(reqVO.getRemark());
        replyMapper.insert(reply);

        if (StrUtil.isBlank(monthPlan.getExecutionStatus()) || STATUS_PLAN.equals(monthPlan.getExecutionStatus())) {
            SrmSupplierReviewMonthPlanDO update = new SrmSupplierReviewMonthPlanDO();
            update.setId(monthPlan.getId());
            update.setPlannedFlag(true);
            update.setExecutionStatus(STATUS_EXECUTING);
            update.setStatusRemark("执行回复上报");
            update.setUpdateDescription("执行人员上报供方评审回复");
            monthPlanMapper.updateById(update);
            insertStatusLog(monthPlan, STATUS_EXECUTING, "执行回复上报", "执行人员上报供方评审回复");
        }
        refreshYearAndLineSummary(monthPlan.getYearPlanId(), monthPlan.getLineId());
        return reply.getId();
    }

    @Override
    public PageResult<SrmSupplierReviewPlanRespVO.ExecutionItem> getExecutionPage(
            SrmSupplierReviewExecutionPageReqVO reqVO) {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        List<SrmSupplierReviewMonthPlanDO> sourceMonths;
        // ===== 全部计划（ALL）：需权限校验，跳过身份过滤 =====
        if (SCOPE_ALL.equalsIgnoreCase(reqVO.getScope())) {
            assertViewAllReviewRecordPermission(currentUserId);
            sourceMonths = monthPlanMapper.selectList();
        } else {
            // ===== 我相关的计划（MINE，默认）：牵头人/相关人员/审核人/修改人 并集 =====
            Set<Long> monthIds = collectMyReviewMonthIds(currentUserId);
            if (CollUtil.isEmpty(monthIds)) {
                return PageResult.empty();
            }
            sourceMonths = monthPlanMapper.selectListByIds(monthIds);
        }
        List<SrmSupplierReviewMonthPlanDO> months = sourceMonths.stream()
                .filter(month -> Boolean.TRUE.equals(month.getPlannedFlag()))
                .filter(month -> reqVO.getPlanYear() == null || Objects.equals(reqVO.getPlanYear(), month.getPlanYear()))
                .filter(month -> reqVO.getPlanMonth() == null || Objects.equals(reqVO.getPlanMonth(), month.getPlanMonth()))
                .filter(month -> isStatusMatch(month.getExecutionStatus(), reqVO))
                .toList();
        if (CollUtil.isEmpty(months)) {
            return PageResult.empty();
        }
        Map<Long, SrmSupplierReviewPlanLineDO> lineMap = lineMapper.selectListByIds(months.stream()
                        .map(SrmSupplierReviewMonthPlanDO::getLineId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SrmSupplierReviewPlanLineDO::getId, Function.identity(), (a, b) -> a));
        Map<Long, MesSupplierDO> supplierMap = loadSupplierMap(lineMap.values());
        List<SrmSupplierReviewPlanRespVO.ExecutionItem> items = months.stream()
                .map(month -> buildExecutionItem(month, lineMap.get(month.getLineId()), supplierMap))
                .filter(item -> StrUtil.isBlank(reqVO.getSupplierCode())
                        || StrUtil.containsIgnoreCase(item.getSupplierCode(), reqVO.getSupplierCode()))
                .filter(item -> StrUtil.isBlank(reqVO.getSupplierName())
                        || StrUtil.containsIgnoreCase(item.getSupplierName(), reqVO.getSupplierName()))
                .filter(item -> StrUtil.isBlank(reqVO.getMaterialCode())
                        || StrUtil.containsIgnoreCase(item.getMaterialCode(), reqVO.getMaterialCode()))
                .sorted(Comparator.comparing(SrmSupplierReviewPlanRespVO.ExecutionItem::getPlanYear,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SrmSupplierReviewPlanRespVO.ExecutionItem::getPlanMonth,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SrmSupplierReviewPlanRespVO.ExecutionItem::getSupplierName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.max(1, reqVO.getPageSize());
        int from = Math.min((pageNo - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageResult<>(items.subList(from, to), (long) items.size());
    }

    /**
     * 收集“我相关”的月份计划 ID：牵头执行人(lead_user_id)、相关人员(participant.user_id)、
     * 审核人(approver_user_id)、修改人(updater=当前用户ID字符串) 并集。
     */
    private Set<Long> collectMyReviewMonthIds(Long currentUserId) {
        Set<Long> monthIds = new LinkedHashSet<>();
        if (currentUserId == null) {
            return monthIds;
        }
        participantMapper.selectListByUserId(currentUserId).stream()
                .map(SrmSupplierReviewParticipantDO::getMonthPlanId)
                .filter(Objects::nonNull)
                .forEach(monthIds::add);
        monthPlanMapper.selectList(SrmSupplierReviewMonthPlanDO::getLeadUserId, currentUserId)
                .forEach(month -> monthIds.add(month.getId()));
        monthPlanMapper.selectList(SrmSupplierReviewMonthPlanDO::getApproverUserId, currentUserId)
                .forEach(month -> monthIds.add(month.getId()));
        monthPlanMapper.selectList(SrmSupplierReviewMonthPlanDO::getUpdater, String.valueOf(currentUserId))
                .forEach(month -> monthIds.add(month.getId()));
        return monthIds;
    }

    private void assertViewAllReviewRecordPermission(Long userId) {
        if (!canViewAllReviewRecord(userId)) {
            throw exception(SRM_SUPPLIER_REVIEW_EXECUTION_VIEW_ALL_FORBIDDEN);
        }
    }

    private boolean canViewAllReviewRecord(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyPermissions(userId, VIEW_ALL_REVIEW_RECORD_PERMISSION)
                || permissionApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE, SRM_SUPPLIER_SUPER_ADMIN_ROLE)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private SrmSupplierReviewPlanRespVO.YearPlan buildYearResp(SrmSupplierReviewYearPlanDO plan) {
        SrmSupplierReviewPlanRespVO.YearPlan resp = new SrmSupplierReviewPlanRespVO.YearPlan();
        resp.setId(plan.getId());
        resp.setPlanNo(plan.getPlanNo());
        resp.setPlanYear(plan.getPlanYear());
        resp.setPlanTitle(plan.getPlanTitle());
        resp.setCompletionSummary(plan.getCompletionSummary());
        resp.setPreparedDept(plan.getPreparedDept());
        resp.setPreparedBy(plan.getPreparedBy());
        resp.setConfirmedBy(plan.getConfirmedBy());
        resp.setApprovedBy(plan.getApprovedBy());
        resp.setRemark(plan.getRemark());
        resp.setVersion(plan.getVersion());
        resp.setCreateTime(plan.getCreateTime());
        resp.setUpdateTime(plan.getUpdateTime());

        List<SrmSupplierReviewPlanLineDO> lines = lineMapper.selectListByYearPlanId(plan.getId());
        Map<Long, List<SrmSupplierReviewMonthPlanDO>> monthMap = monthPlanMapper.selectListByYearPlanId(plan.getId())
                .stream().collect(Collectors.groupingBy(SrmSupplierReviewMonthPlanDO::getLineId,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, List<String>> materialCodeMap = loadSupplierMaterialCodeMap(lines);
        resp.setLines(groupSupplierLines(lines).stream()
                .map(group -> {
                    SrmSupplierReviewPlanLineDO line = group.get(0);
                    String supplierCode = supplierCodeKey(line.getSupplierCode());
                    return buildLineResp(line, mergeLineMonths(group, monthMap),
                            StrUtil.isBlank(supplierCode) ? List.of()
                                    : materialCodeMap.getOrDefault(supplierCode, List.of()));
                })
                .toList());
        return resp;
    }

    private SrmSupplierReviewPlanRespVO.Line buildLineResp(SrmSupplierReviewPlanLineDO line,
                                                          List<SrmSupplierReviewMonthPlanDO> months,
                                                          List<String> materialCodes) {
        SrmSupplierReviewPlanRespVO.Line resp = new SrmSupplierReviewPlanRespVO.Line();
        resp.setId(line.getId());
        resp.setYearPlanId(line.getYearPlanId());
        resp.setPlanYear(line.getPlanYear());
        resp.setRowNo(line.getRowNo());
        resp.setSupplierId(line.getSupplierId());
        resp.setSupplierCode(line.getSupplierCode());
        resp.setSupplierName(line.getSupplierName());
        resp.setContactPerson(line.getContactPerson());
        List<String> safeMaterialCodes = materialCodes == null ? List.of() : materialCodes;
        resp.setMaterialCodes(safeMaterialCodes);
        resp.setMaterialCode(CollUtil.isEmpty(safeMaterialCodes) ? line.getMaterialCode()
                : String.join("\n", safeMaterialCodes));
        resp.setMaterialName(line.getMaterialName());
        resp.setModel(line.getModel());
        resp.setApplicableProduct(line.getApplicableProduct());
        resp.setProvidedProduct(line.getProvidedProduct());
        List<SrmSupplierReviewMonthPlanDO> normalizedMonths = normalizeMonths(line, months);
        resp.setCompletionStatus(buildLineCompletionStatus(normalizedMonths));
        resp.setLatestAuditDate(buildLineLatestAuditDate(normalizedMonths, line.getLatestAuditDate()));
        resp.setRemark(line.getRemark());
        resp.setVersion(line.getVersion());
        resp.setMonths(normalizedMonths.stream().map(month -> buildMonthResp(month, false)).toList());
        return resp;
    }

    private String buildLineCompletionStatus(List<SrmSupplierReviewMonthPlanDO> months) {
        long plannedCount = CollUtil.emptyIfNull(months).stream()
                .filter(month -> Boolean.TRUE.equals(month.getPlannedFlag()))
                .count();
        if (plannedCount == 0) {
            return "未安排";
        }
        long completedCount = CollUtil.emptyIfNull(months).stream()
                .filter(month -> STATUS_COMPLETED.equals(month.getExecutionStatus())
                        || STATUS_ARCHIVED.equals(month.getExecutionStatus()))
                .count();
        return String.format("已完成 %d/%d", completedCount, plannedCount);
    }

    private LocalDate buildLineLatestAuditDate(List<SrmSupplierReviewMonthPlanDO> months, LocalDate defaultDate) {
        return CollUtil.emptyIfNull(months).stream()
                .map(SrmSupplierReviewMonthPlanDO::getAuditDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(defaultDate);
    }

    private List<List<SrmSupplierReviewPlanLineDO>> groupSupplierLines(List<SrmSupplierReviewPlanLineDO> lines) {
        if (CollUtil.isEmpty(lines)) {
            return List.of();
        }
        Map<String, List<SrmSupplierReviewPlanLineDO>> grouped = new LinkedHashMap<>();
        for (SrmSupplierReviewPlanLineDO line : lines) {
            grouped.computeIfAbsent(supplierLineKey(line), key -> new ArrayList<>()).add(line);
        }
        return new ArrayList<>(grouped.values());
    }

    private List<SrmSupplierReviewMonthPlanDO> mergeLineMonths(List<SrmSupplierReviewPlanLineDO> lines,
                                                              Map<Long, List<SrmSupplierReviewMonthPlanDO>> monthMap) {
        Map<Integer, List<SrmSupplierReviewMonthPlanDO>> grouped = new LinkedHashMap<>();
        for (SrmSupplierReviewPlanLineDO line : CollUtil.emptyIfNull(lines)) {
            for (SrmSupplierReviewMonthPlanDO month : CollUtil.emptyIfNull(monthMap.get(line.getId()))) {
                if (month.getPlanMonth() != null) {
                    grouped.computeIfAbsent(month.getPlanMonth(), key -> new ArrayList<>()).add(month);
                }
            }
        }
        List<SrmSupplierReviewMonthPlanDO> merged = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            SrmSupplierReviewMonthPlanDO monthPlan = selectDisplayMonthPlan(grouped.get(month));
            if (monthPlan != null) {
                merged.add(monthPlan);
            }
        }
        return merged;
    }

    private SrmSupplierReviewMonthPlanDO selectDisplayMonthPlan(List<SrmSupplierReviewMonthPlanDO> months) {
        if (CollUtil.isEmpty(months)) {
            return null;
        }
        return months.stream()
                .filter(month -> Boolean.TRUE.equals(month.getPlannedFlag()))
                .findFirst()
                .orElse(months.get(0));
    }

    private Map<String, List<String>> loadSupplierMaterialCodeMap(Collection<SrmSupplierReviewPlanLineDO> lines) {
        if (CollUtil.isEmpty(lines)) {
            return Map.of();
        }
        Set<String> supplierCodes = lines.stream()
                .map(SrmSupplierReviewPlanLineDO::getSupplierCode)
                .map(this::supplierCodeKey)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(supplierCodes)) {
            return Map.of();
        }
        List<MesSupplierDO> suppliers = supplierMapper.selectList(new LambdaQueryWrapperX<MesSupplierDO>()
                .in(MesSupplierDO::getSupplierCode, supplierCodes)
                .orderByAsc(MesSupplierDO::getSupplierCode)
                .orderByAsc(MesSupplierDO::getMaterialCode)
                .orderByAsc(MesSupplierDO::getId));
        Map<String, LinkedHashSet<String>> grouped = new LinkedHashMap<>();
        for (MesSupplierDO supplier : suppliers) {
            String supplierCode = supplierCodeKey(supplier.getSupplierCode());
            String materialCode = StrUtil.trim(supplier.getMaterialCode());
            if (StrUtil.isNotBlank(supplierCode) && StrUtil.isNotBlank(materialCode)) {
                grouped.computeIfAbsent(supplierCode, key -> new LinkedHashSet<>()).add(materialCode);
            }
        }
        return grouped.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue()),
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    private SrmSupplierReviewPlanRespVO.MonthPlan buildMonthResp(SrmSupplierReviewMonthPlanDO month,
                                                                 boolean withDetail) {
        SrmSupplierReviewPlanRespVO.MonthPlan resp = new SrmSupplierReviewPlanRespVO.MonthPlan();
        resp.setId(month.getId());
        resp.setYearPlanId(month.getYearPlanId());
        resp.setLineId(month.getLineId());
        resp.setPlanYear(month.getPlanYear());
        resp.setPlanMonth(month.getPlanMonth());
        resp.setPlannedFlag(month.getPlannedFlag());
        resp.setExecutionStatus(month.getExecutionStatus());
        resp.setExecutionStatusName(statusText(month.getExecutionStatus()));
        resp.setPlanDesc(month.getPlanDesc());
        resp.setLeadUserId(month.getLeadUserId());
        resp.setLeadUserName(month.getLeadUserName());
        resp.setRelatedUserIds(month.getRelatedUserIds());
        resp.setRelatedUserNames(month.getRelatedUserNames());
        resp.setAuditDate(month.getAuditDate());
        resp.setAuditCategory(month.getAuditCategory());
        resp.setAuditCategoryName(auditCategoryText(month.getAuditCategory()));
        resp.setAuditDesc(month.getAuditDesc());
        resp.setAuditAttachment(month.getAuditAttachment());
        resp.setApproverUserId(month.getApproverUserId());
        resp.setApproverUserName(month.getApproverUserName());
        resp.setApprovalOpinion(month.getApprovalOpinion());
        resp.setApprovalTime(month.getApprovalTime());
        resp.setStatusRemark(month.getStatusRemark());
        resp.setUpdateDescription(month.getUpdateDescription());
        resp.setRemark(month.getRemark());
        resp.setVersion(month.getVersion());
        if (withDetail) {
            resp.setParticipants(participantMapper.selectListByMonthPlanId(month.getId()).stream()
                    .map(this::buildParticipantResp).toList());
            resp.setReplies(replyMapper.selectListByMonthPlanId(month.getId()).stream()
                    .map(this::buildReplyResp).toList());
            resp.setStatusLogs(statusLogMapper.selectListByMonthPlanId(month.getId()).stream()
                    .map(this::buildStatusLogResp).toList());
        }
        return resp;
    }

    private List<SrmSupplierReviewMonthPlanDO> normalizeMonths(SrmSupplierReviewPlanLineDO line,
                                                              List<SrmSupplierReviewMonthPlanDO> months) {
        Map<Integer, SrmSupplierReviewMonthPlanDO> monthMap = CollUtil.emptyIfNull(months).stream()
                .collect(Collectors.toMap(SrmSupplierReviewMonthPlanDO::getPlanMonth, Function.identity(), (a, b) -> a));
        List<SrmSupplierReviewMonthPlanDO> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            SrmSupplierReviewMonthPlanDO monthPlan = monthMap.get(month);
            if (monthPlan == null) {
                monthPlan = new SrmSupplierReviewMonthPlanDO();
                monthPlan.setYearPlanId(line.getYearPlanId());
                monthPlan.setLineId(line.getId());
                monthPlan.setPlanYear(line.getPlanYear());
                monthPlan.setPlanMonth(month);
                monthPlan.setPlannedFlag(false);
            }
            result.add(monthPlan);
        }
        return result;
    }

    private List<SrmSupplierReviewPlanLineDO> selectSameSupplierLines(SrmSupplierReviewPlanLineDO line) {
        if (line == null) {
            return List.of();
        }
        if (line.getYearPlanId() == null) {
            return List.of(line);
        }
        List<SrmSupplierReviewPlanLineDO> sameLines = lineMapper.selectListByYearPlanId(line.getYearPlanId())
                .stream()
                .filter(candidate -> isSameSupplierLine(line, candidate))
                .toList();
        return CollUtil.isEmpty(sameLines) ? List.of(line) : sameLines;
    }

    private boolean isSameSupplierLine(SrmSupplierReviewPlanLineDO source, SrmSupplierReviewPlanLineDO candidate) {
        if (source == null || candidate == null) {
            return false;
        }
        String sourceCode = supplierCodeKey(source.getSupplierCode());
        String candidateCode = supplierCodeKey(candidate.getSupplierCode());
        if (StrUtil.isNotBlank(sourceCode) && StrUtil.isNotBlank(candidateCode)) {
            return Objects.equals(sourceCode, candidateCode);
        }
        if (source.getSupplierId() != null && candidate.getSupplierId() != null) {
            return Objects.equals(source.getSupplierId(), candidate.getSupplierId());
        }
        return Objects.equals(source.getId(), candidate.getId());
    }

    private String supplierLineKey(SrmSupplierReviewPlanLineDO line) {
        String supplierCode = supplierCodeKey(line.getSupplierCode());
        if (StrUtil.isNotBlank(supplierCode)) {
            return "CODE:" + supplierCode;
        }
        if (line.getSupplierId() != null) {
            return "ID:" + line.getSupplierId();
        }
        return "LINE:" + line.getId();
    }

    private String supplierCodeKey(String supplierCode) {
        return StrUtil.trim(supplierCode);
    }

    private SrmSupplierReviewPlanRespVO.Participant buildParticipantResp(SrmSupplierReviewParticipantDO participant) {
        SrmSupplierReviewPlanRespVO.Participant resp = new SrmSupplierReviewPlanRespVO.Participant();
        resp.setId(participant.getId());
        resp.setMonthPlanId(participant.getMonthPlanId());
        resp.setRelationType(participant.getRelationType());
        resp.setUserId(participant.getUserId());
        resp.setUserName(participant.getUserName());
        resp.setDeptName(participant.getDeptName());
        return resp;
    }

    private SrmSupplierReviewPlanRespVO.Reply buildReplyResp(SrmSupplierReviewReplyDO reply) {
        SrmSupplierReviewPlanRespVO.Reply resp = new SrmSupplierReviewPlanRespVO.Reply();
        resp.setId(reply.getId());
        resp.setMonthPlanId(reply.getMonthPlanId());
        resp.setReplyTime(reply.getReplyTime());
        resp.setReviewDate(reply.getReviewDate());
        resp.setRecorderUserId(reply.getRecorderUserId());
        resp.setRecorderUserName(reply.getRecorderUserName());
        resp.setReviewResult(reply.getReviewResult());
        resp.setRemark(reply.getRemark());
        return resp;
    }

    private SrmSupplierReviewPlanRespVO.StatusLog buildStatusLogResp(SrmSupplierReviewStatusLogDO log) {
        SrmSupplierReviewPlanRespVO.StatusLog resp = new SrmSupplierReviewPlanRespVO.StatusLog();
        resp.setId(log.getId());
        resp.setMonthPlanId(log.getMonthPlanId());
        resp.setFromStatus(log.getFromStatus());
        resp.setFromStatusName(statusText(log.getFromStatus()));
        resp.setToStatus(log.getToStatus());
        resp.setToStatusName(statusText(log.getToStatus()));
        resp.setReason(log.getReason());
        resp.setUpdateDescription(log.getUpdateDescription());
        resp.setOperatorUserId(log.getOperatorUserId());
        resp.setOperatorUserName(log.getOperatorUserName());
        resp.setCreateTime(log.getCreateTime());
        return resp;
    }

    private SrmSupplierReviewPlanRespVO.ExecutionItem buildExecutionItem(SrmSupplierReviewMonthPlanDO month,
                                                                        SrmSupplierReviewPlanLineDO line,
                                                                        Map<Long, MesSupplierDO> supplierMap) {
        SrmSupplierReviewPlanRespVO.ExecutionItem resp = new SrmSupplierReviewPlanRespVO.ExecutionItem();
        resp.setMonthPlanId(month.getId());
        resp.setLineId(month.getLineId());
        resp.setPlanYear(month.getPlanYear());
        resp.setPlanMonth(month.getPlanMonth());
        resp.setExecutionStatus(month.getExecutionStatus());
        resp.setExecutionStatusName(statusText(month.getExecutionStatus()));
        resp.setPlanDesc(month.getPlanDesc());
        resp.setLeadUserName(month.getLeadUserName());
        resp.setRelatedUserNames(month.getRelatedUserNames());
        resp.setAuditDate(month.getAuditDate());
        resp.setAuditCategory(month.getAuditCategory());
        resp.setAuditCategoryName(auditCategoryText(month.getAuditCategory()));
        resp.setAuditDesc(month.getAuditDesc());
        resp.setAuditAttachment(month.getAuditAttachment());
        resp.setUpdateTime(month.getUpdateTime());
        if (line != null) {
            resp.setSupplierCode(line.getSupplierCode());
            resp.setSupplierName(line.getSupplierName());
            resp.setContactPerson(line.getContactPerson());
            resp.setMaterialCode(line.getMaterialCode());
            resp.setMaterialName(line.getMaterialName());
            resp.setModel(line.getModel());
            resp.setApplicableProduct(line.getApplicableProduct());
            resp.setProvidedProduct(line.getProvidedProduct());
            if (line.getSupplierId() != null && supplierMap != null) {
                MesSupplierDO supplier = supplierMap.get(line.getSupplierId());
                if (supplier != null) {
                    resp.setUseDepartment(supplier.getUsingDepartment());
                }
            }
        }
        return resp;
    }

    private void fillRelatedUsers(SrmSupplierReviewMonthPlanDO update,
                                  List<SrmSupplierReviewPlanReqVO.UserSnapshot> users) {
        List<SrmSupplierReviewPlanReqVO.UserSnapshot> validUsers = CollUtil.emptyIfNull(users).stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(SrmSupplierReviewPlanReqVO.UserSnapshot::getId, Function.identity(),
                                (first, ignored) -> first, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())));
        update.setRelatedUserIds(validUsers.stream()
                .map(user -> String.valueOf(user.getId()))
                .collect(Collectors.joining(",")));
        update.setRelatedUserNames(validUsers.stream()
                .map(user -> defaultString(user.getName(), "-"))
                .collect(Collectors.joining("、")));
    }

    private void syncParticipants(SrmSupplierReviewMonthPlanDO old, SrmSupplierReviewMonthPlanDO update,
                                  List<SrmSupplierReviewPlanReqVO.UserSnapshot> relatedUsers) {
        participantMapper.deleteByMonthPlanId(old.getId());
        if (update.getLeadUserId() != null) {
            participantMapper.insert(buildParticipant(old, RELATION_LEAD, update.getLeadUserId(),
                    update.getLeadUserName(), null));
        }
        CollUtil.emptyIfNull(relatedUsers).stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(SrmSupplierReviewPlanReqVO.UserSnapshot::getId, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new))
                .values()
                .forEach(user -> participantMapper.insert(buildParticipant(old, RELATION_RELATED,
                        user.getId(), user.getName(), user.getDeptName())));
    }

    private SrmSupplierReviewParticipantDO buildParticipant(SrmSupplierReviewMonthPlanDO month, String relationType,
                                                           Long userId, String userName, String deptName) {
        SrmSupplierReviewParticipantDO participant = new SrmSupplierReviewParticipantDO();
        participant.setYearPlanId(month.getYearPlanId());
        participant.setLineId(month.getLineId());
        participant.setMonthPlanId(month.getId());
        participant.setPlanYear(month.getPlanYear());
        participant.setPlanMonth(month.getPlanMonth());
        participant.setRelationType(relationType);
        participant.setUserId(userId);
        participant.setUserName(defaultString(userName, queryUserName(userId)));
        participant.setDeptName(defaultString(deptName, queryDeptNameByUserId(userId)));
        return participant;
    }

    private void fillLineSupplier(SrmSupplierReviewPlanLineDO line,
                                  SrmSupplierReviewPlanReqVO.AddSupplierLine reqVO,
                                  MesSupplierDO supplier) {
        line.setSupplierId(supplier == null ? reqVO.getSupplierId() : supplier.getId());
        line.setSupplierCode(firstNotBlank(reqVO.getSupplierCode(), supplier == null ? null : supplier.getSupplierCode()));
        line.setSupplierName(firstNotBlank(reqVO.getSupplierName(), supplier == null ? null : supplier.getSupplierName()));
        line.setContactPerson(firstNotBlank(reqVO.getContactPerson(), supplier == null ? null : supplier.getContactPerson()));
        line.setMaterialCode(null);
        line.setMaterialName(null);
        line.setModel(null);
        line.setApplicableProduct(null);
        line.setProvidedProduct(null);
    }

    private MesSupplierDO resolveSupplier(SrmSupplierReviewPlanReqVO.AddSupplierLine reqVO) {
        if (StrUtil.isNotBlank(reqVO.getSupplierCode()) && StrUtil.isNotBlank(reqVO.getMaterialCode())) {
            MesSupplierDO supplierByMaterial = supplierMapper.selectOne(new LambdaQueryWrapperX<MesSupplierDO>()
                    .eq(MesSupplierDO::getSupplierCode, reqVO.getSupplierCode())
                    .eq(MesSupplierDO::getMaterialCode, reqVO.getMaterialCode())
                    .last("LIMIT 1"));
            if (supplierByMaterial != null) {
                return supplierByMaterial;
            }
        }
        if (reqVO.getSupplierId() != null) {
            MesSupplierDO supplierById = supplierMapper.selectById(reqVO.getSupplierId());
            if (supplierById != null) {
                return supplierById;
            }
        }
        if (StrUtil.isNotBlank(reqVO.getSupplierCode())) {
            MesSupplierDO supplier = supplierMapper.selectFirstOne(MesSupplierDO::getSupplierCode, reqVO.getSupplierCode());
            if (supplier != null) {
                return supplier;
            }
        }
        if (StrUtil.isNotBlank(reqVO.getSupplierName())) {
            return supplierMapper.selectFirstOne(MesSupplierDO::getSupplierName, reqVO.getSupplierName());
        }
        return null;
    }

    private void insertStatusLog(SrmSupplierReviewMonthPlanDO month, String toStatus,
                                 String reason, String updateDescription) {
        CurrentUser currentUser = getCurrentUser();
        SrmSupplierReviewStatusLogDO log = new SrmSupplierReviewStatusLogDO();
        log.setYearPlanId(month.getYearPlanId());
        log.setLineId(month.getLineId());
        log.setMonthPlanId(month.getId());
        log.setPlanYear(month.getPlanYear());
        log.setPlanMonth(month.getPlanMonth());
        log.setFromStatus(month.getExecutionStatus());
        log.setToStatus(toStatus);
        log.setReason(reason);
        log.setUpdateDescription(updateDescription);
        log.setOperatorUserId(currentUser.userId());
        log.setOperatorUserName(currentUser.userName());
        statusLogMapper.insert(log);
    }

    private void refreshYearAndLineSummary(Long yearPlanId, Long lineId) {
        List<SrmSupplierReviewMonthPlanDO> lineMonths = monthPlanMapper.selectListByLineId(lineId);
        long plannedCount = lineMonths.stream().filter(month -> Boolean.TRUE.equals(month.getPlannedFlag())).count();
        long completedCount = lineMonths.stream()
                .filter(month -> STATUS_COMPLETED.equals(month.getExecutionStatus())
                        || STATUS_ARCHIVED.equals(month.getExecutionStatus()))
                .count();
        LocalDate latestAuditDate = lineMonths.stream()
                .map(SrmSupplierReviewMonthPlanDO::getAuditDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        SrmSupplierReviewPlanLineDO lineUpdate = new SrmSupplierReviewPlanLineDO();
        lineUpdate.setId(lineId);
        lineUpdate.setLatestAuditDate(latestAuditDate);
        lineUpdate.setCompletionStatus(plannedCount == 0
                ? "未安排"
                : String.format("已完成 %d/%d", completedCount, plannedCount));
        lineMapper.updateById(lineUpdate);

        refreshYearSummary(yearPlanId);
    }

    private void refreshYearSummary(Long yearPlanId) {
        List<SrmSupplierReviewMonthPlanDO> yearMonths = monthPlanMapper.selectListByYearPlanId(yearPlanId);
        long yearPlanned = yearMonths.stream().filter(month -> Boolean.TRUE.equals(month.getPlannedFlag())).count();
        long yearCompleted = yearMonths.stream()
                .filter(month -> STATUS_COMPLETED.equals(month.getExecutionStatus())
                        || STATUS_ARCHIVED.equals(month.getExecutionStatus()))
                .count();
        SrmSupplierReviewYearPlanDO yearUpdate = new SrmSupplierReviewYearPlanDO();
        yearUpdate.setId(yearPlanId);
        yearUpdate.setCompletionSummary(yearPlanned == 0
                ? "未安排"
                : String.format("已完成 %d/%d", yearCompleted, yearPlanned));
        yearPlanMapper.updateById(yearUpdate);
    }

    private SrmSupplierReviewPlanRespVO.DeleteAnnualPlansResult buildDeleteAnnualResult(Long lineId,
                                                                                       Integer planYear,
                                                                                       String supplierCode,
                                                                                       String materialCode,
                                                                                       Long yearPlanId,
                                                                                       int deletedLineCount,
                                                                                       int deletedMonthCount,
                                                                                       int deletedParticipantCount,
                                                                                       int deletedReplyCount,
                                                                                       int deletedStatusLogCount,
                                                                                       int deletedAttachmentCount) {
        SrmSupplierReviewPlanRespVO.DeleteAnnualPlansResult result =
                new SrmSupplierReviewPlanRespVO.DeleteAnnualPlansResult();
        result.setLineId(lineId);
        result.setPlanYear(planYear);
        result.setSupplierCode(supplierCode);
        result.setMaterialCode(materialCode);
        result.setYearPlanId(yearPlanId);
        result.setDeletedLineCount(deletedLineCount);
        result.setDeletedMonthCount(deletedMonthCount);
        result.setDeletedParticipantCount(deletedParticipantCount);
        result.setDeletedReplyCount(deletedReplyCount);
        result.setDeletedStatusLogCount(deletedStatusLogCount);
        result.setDeletedAttachmentCount(deletedAttachmentCount);
        return result;
    }

    private void assertDeleteAnnualPlanPermission() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (!canDeleteAnnualPlan(userId)) {
            throw exception(SRM_SUPPLIER_REVIEW_PLAN_DELETE_FORBIDDEN);
        }
    }

    private boolean canDeleteAnnualPlan(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyPermissions(userId, DELETE_ANNUAL_PLAN_PERMISSION)
                || permissionApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE, SRM_SUPPLIER_SUPER_ADMIN_ROLE)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private String queryUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user == null ? null : user.getNickname();
    }

    private String queryDeptNameByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || user.getDeptId() == null) {
            return null;
        }
        DeptRespDTO dept = deptApi.getDept(user.getDeptId());
        return dept == null ? null : dept.getName();
    }

    private CurrentUser getCurrentUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        Long deptId = SecurityFrameworkUtils.getLoginUserDeptId();
        if (userId != null && (StrUtil.isBlank(userName) || deptId == null)) {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user != null) {
                userName = defaultString(userName, user.getNickname());
                deptId = deptId == null ? user.getDeptId() : deptId;
            }
        }
        String deptName = null;
        if (deptId != null) {
            DeptRespDTO dept = deptApi.getDept(deptId);
            deptName = dept == null ? null : dept.getName();
        }
        return new CurrentUser(userId, defaultString(userName, "当前用户"), deptName);
    }

    private static String statusText(String status) {
        return switch (StrUtil.blankToDefault(status, "")) {
            case STATUS_PLAN -> "计划";
            case STATUS_EXECUTING -> "执行";
            case STATUS_CHANGED -> "变更";
            case STATUS_CANCELED -> "取消";
            case STATUS_COMPLETED -> "完成";
            case STATUS_ARCHIVED -> "归档";
            default -> "";
        };
    }

    /**
     * 判断月份计划状态是否命中单状态或多状态过滤；支持逗号分隔及 executionStatuses 列表；未配置任何状态过滤时返回 true。
     */
    private static boolean isStatusMatch(String executionStatus, SrmSupplierReviewExecutionPageReqVO reqVO) {
        Set<String> statusFilter = new HashSet<>();
        if (StrUtil.isNotBlank(reqVO.getExecutionStatus())) {
            for (String s : reqVO.getExecutionStatus().split(",")) {
                String trimmed = s.trim();
                if (StrUtil.isNotBlank(trimmed)) {
                    statusFilter.add(trimmed);
                }
            }
        }
        if (CollUtil.isNotEmpty(reqVO.getExecutionStatuses())) {
            reqVO.getExecutionStatuses().stream()
                    .filter(StrUtil::isNotBlank)
                    .flatMap(s -> java.util.Arrays.stream(s.split(",")))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .forEach(statusFilter::add);
        }
        if (statusFilter.isEmpty()) {
            return true;
        }
        return statusFilter.contains(executionStatus);
    }

    /**
     * 批量加载计划行对应的供应商主数据，用于回填“使用部门”。
     */
    private Map<Long, MesSupplierDO> loadSupplierMap(Collection<SrmSupplierReviewPlanLineDO> lines) {
        if (CollUtil.isEmpty(lines)) {
            return Map.of();
        }
        Set<Long> supplierIds = lines.stream()
                .map(SrmSupplierReviewPlanLineDO::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(supplierIds)) {
            return Map.of();
        }
        return supplierMapper.selectBatchIds(supplierIds).stream()
                .collect(Collectors.toMap(MesSupplierDO::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 审核类别字典文案（与 system_dict_data mes_srm_site_inspection_audit_category 保持一致）。
     */
    private static String auditCategoryText(String category) {
        return switch (StrUtil.blankToDefault(category, "")) {
            case "CERTIFICATION_AUDIT" -> "认证审核";
            case "ANNUAL_AUDIT" -> "年度审核";
            case "IRREGULAR_AUDIT" -> "不定期审核";
            default -> "";
        };
    }

    private static String defaultString(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private record CurrentUser(Long userId, String userName, String deptName) {
    }

}
