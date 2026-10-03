package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsSampleAbnormalLockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSecondDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsSampleAbnormalLockMapper;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/** 湿法、二磨留样异常允许继续加工，裁切报检及完工时执行质量门禁。 */
@Service
public class HcUpstreamSampleLockService {
    @Resource private QmsSampleAbnormalLockMapper lockMapper;
    @Resource private cn.iocoder.yudao.module.mes.service.qms.QmsNcPickQualificationService pickQualificationService;
    @Resource private HcAdhesive2ReportMapper adhesive2Mapper;
    @Resource private HcPressSlotReportMapper pressSlotMapper;
    @Resource private HcSlittingSliceRecordMapper sliceMapper;
    @Resource private HcAdhesiveReportMapper adhesiveMapper;
    @Resource private HcGrindingSecondDetailMapper grindingMapper;

    public String getCutRoundLockReason(HcCutRoundReportDO report) {
        Set<String> batches = new LinkedHashSet<>();
        add(batches, report.getSourceBatchNo(), report.getSourceProductionBatchNo(),
                report.getParentProductionBatchNo(), report.getSourceStockBatchNo());
        Long sliceId = report.getSourceSlittingSliceId();
        Long pressId = report.getSourcePressSlotReportId();
        if (report.getSourceAdhesive2ReportId() != null) {
            var source = adhesive2Mapper.selectById(report.getSourceAdhesive2ReportId());
            if (source != null) {
                add(batches, source.getSourceBatchNo(), source.getSourceProductionBatchNo(),
                        source.getParentProductionBatchNo(), source.getSourceStockBatchNo());
                if (sliceId == null) sliceId = source.getSourceSlittingSliceId();
                if (pressId == null) pressId = source.getSourcePressSlotReportId();
            }
        }
        if (sliceId == null && pressId != null) {
            var source = pressSlotMapper.selectById(pressId);
            if (source != null) {
                sliceId = source.getSourceSlittingSliceId();
                add(batches, source.getSourceBatchNo(), source.getSourceProductionBatchNo(),
                        source.getParentProductionBatchNo(), source.getSourceStockBatchNo());
            }
        }
        if (sliceId != null) {
            var slice = sliceMapper.selectById(sliceId);
            if (slice != null) addSliceSources(batches, slice);
        }
        if (batches.isEmpty()) add(batches, report.getProductionBatchNo());
        return reason(batches, report.getTenantId());
    }

    public String getSlittingLockReason(HcSlittingSliceRecordDO slice) {
        Set<String> batches = new LinkedHashSet<>();
        addSliceSources(batches, slice);
        return reason(batches, slice.getTenantId());
    }

    private void addSliceSources(Set<String> batches, HcSlittingSliceRecordDO slice) {
        add(batches, slice.getSourceBatchNo(), slice.getSourceProductionBatchNo(), slice.getSourceStockBatchNo());
        if (slice.getSourceAdhesiveReportId() == null) return;
        var adhesive = adhesiveMapper.selectById(slice.getSourceAdhesiveReportId());
        if (adhesive == null) return;
        add(batches, adhesive.getSourceBatchNo(), adhesive.getSourceProductionBatchNo(),
                adhesive.getProductionBatchNo(), adhesive.getParentProductionBatchNo(), adhesive.getSourceStockBatchNo());
        if (adhesive.getSourceGrindingSecondDetailId() == null) return;
        var grinding = grindingMapper.selectById(adhesive.getSourceGrindingSecondDetailId());
        if (grinding != null) add(batches, grinding.getMotherBatchNo(), grinding.getProductionBatchNo(),
                grinding.getParentProductionBatchNo(), grinding.getSourceProductionBatchNo());
    }

    private String reason(Set<String> segments, Long tenantId) {
        if (segments.isEmpty()) return null;
        Set<String> mothers = new LinkedHashSet<>(segments);
        for (String segment : segments) {
            if (segment.matches(".*[PQRS]$")) mothers.add(segment.substring(0, segment.length() - 1));
        }
        Long effectiveTenant = tenantId != null ? tenantId : TenantContextHolder.getTenantId();
        Set<String> reasons = new LinkedHashSet<>();
        for (QmsSampleAbnormalLockDO lock : lockMapper.selectEffectiveUpstreamLocks(mothers, segments, effectiveTenant)) {
            if (pickQualificationService.isLockQualified(lock, segments)) continue;
            reasons.add(StrUtil.blankToDefault(lock.getSourceProcessName(), lock.getSourceProcessCode())
                    + "留样NG，" + ("MOTHER_ROLL".equals(lock.getObjectType()) ? "母卷 " : "分段 ")
                    + lock.getObjectNo() + "，检验单 " + StrUtil.blankToDefault(lock.getAbnormalInspectionNo(), "-")
                    + "，锁定编号 " + StrUtil.blankToDefault(lock.getLockNo(), "-")
                    + "；允许继续加工，裁切禁止提交检验、本段完工及工单完工，等待对应复检合格解锁");
        }
        return reasons.isEmpty() ? null : String.join("；", reasons);
    }

    private void add(Set<String> batches, String... values) {
        for (String value : values) {
            String batch = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT)
                    .replaceAll("(?:-J\\d+|-S\\d+)+$", "")
                    .replaceFirst("([PQRS]\\d{3})[AB]$", "$1")
                    .replaceFirst("^(.+[PQRS])\\d{3}$", "$1");
            if (StrUtil.isNotBlank(batch)) batches.add(batch);
        }
    }
}
