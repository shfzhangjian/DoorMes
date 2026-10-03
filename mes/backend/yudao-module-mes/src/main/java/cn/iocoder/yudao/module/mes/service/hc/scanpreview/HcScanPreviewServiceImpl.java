package cn.iocoder.yudao.module.mes.service.hc.scanpreview;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo.HcScanPreviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo.HcScanPreviewRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.scanpreview.HcScanPreviewMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcScanPreviewServiceImpl implements HcScanPreviewService {

    @Resource
    private HcScanPreviewMapper scanPreviewMapper;

    @Override
    public PageResult<HcScanPreviewRecordRespVO> getPage(HcScanPreviewPageReqVO reqVO) {
        Long tenantId = currentTenantId();
        int pageNo = Math.max(reqVO.getPageNo() == null ? 1 : reqVO.getPageNo(), 1);
        int pageSize = Math.min(Math.max(reqVO.getPageSize() == null ? 20 : reqVO.getPageSize(), 1), 200);
        int offset = (pageNo - 1) * pageSize;
        Long total = scanPreviewMapper.countPage(reqVO, tenantId);
        if (total == null || total <= 0) {
            return PageResult.empty();
        }
        List<HcScanPreviewRecordRespVO> rows = scanPreviewMapper.selectPage(reqVO, tenantId, offset, pageSize);
        rows.forEach(this::fillQrValues);
        return new PageResult<>(rows, total);
    }

    private void fillQrValues(HcScanPreviewRecordRespVO row) {
        String primaryCode = firstNotBlank(row.getProductionBatchNo(), row.getBizNo(), row.getSourceBatchNo(), row.getPlanNo());
        String planNo = trimToEmpty(row.getPlanNo());
        String reportQrValue = buildReportQrValue(row.getSourceType(), planNo, primaryCode);
        row.setReportQrValue(reportQrValue);
        row.setReportQrText(firstNotBlank(primaryCode, reportQrValue, "-"));
        row.setInspectionQrValue(firstNotBlank(row.getInspectionNo(), null));
        row.setHasInspection(isNotBlank(row.getInspectionNo()));
    }

    private String buildReportQrValue(String sourceType, String planNo, String code) {
        String codeText = trimToEmpty(code);
        if (planNo.isEmpty() && codeText.isEmpty()) {
            return "";
        }
        if (planNo.isEmpty()) {
            return codeText;
        }
        if (codeText.isEmpty()) {
            return planNo;
        }
        return "ADHESIVE_REPORT".equals(sourceType) ? planNo + "," + codeText : planNo + ";" + codeText;
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

}
