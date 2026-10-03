package cn.iocoder.yudao.module.mes.service.hc.massstock.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockManualSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockGoodStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockShippingDetailRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.massstock.HcMassStockManualDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.massstock.HcMassStockManualMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.massstock.HcMassStockMapper;
import cn.iocoder.yudao.module.mes.service.hc.massstock.HcMassStockService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcMassStockServiceImpl implements HcMassStockService {

    private static final long DEFAULT_TENANT_ID = 1L;
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    @Resource
    private HcMassStockMapper hcMassStockMapper;
    @Resource
    private HcMassStockManualMapper hcMassStockManualMapper;

    @Override
    public PageResult<HcMassStockRespVO> getMassStockPage(HcMassStockPageReqVO pageReqVO) {
        Long tenantId = currentTenantId();
        int pageNo = Math.max(pageReqVO.getPageNo() == null ? DEFAULT_PAGE_NO : pageReqVO.getPageNo(), 1);
        int pageSize = Math.min(Math.max(pageReqVO.getPageSize() == null ? DEFAULT_PAGE_SIZE : pageReqVO.getPageSize(), 1),
                MAX_PAGE_SIZE);
        long total = hcMassStockMapper.selectCount(tenantId, pageReqVO);
        if (total <= 0) {
            return new PageResult<>(List.of(), 0L);
        }
        int offset = (pageNo - 1) * pageSize;
        List<HcMassStockRespVO> list = hcMassStockMapper.selectPage(tenantId, pageReqVO, offset, pageSize);
        return new PageResult<>(list, total);
    }

    @Override
    public List<HcMassStockRespVO> getMassStockList(HcMassStockPageReqVO reqVO) {
        return hcMassStockMapper.selectList(currentTenantId(), reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveManual(HcMassStockManualSaveReqVO reqVO) {
        Long tenantId = currentTenantId();
        String modelCode = normalizeRequired(reqVO.getModelCode(), "型号不能为空");
        String motherBatchNo = normalizeRequired(reqVO.getMotherBatchNo(), "母卷批号不能为空");
        String motherSegmentBatchNo = normalizeRequired(reqVO.getMotherSegmentBatchNo(), "母卷段号不能为空");
        String semResult = StrUtil.trimToNull(reqVO.getSemResult());
        String remark = StrUtil.trimToNull(reqVO.getRemark());

        HcMassStockManualDO existed = hcMassStockManualMapper.selectByStockKey(tenantId, modelCode,
                motherBatchNo, motherSegmentBatchNo);
        if (existed == null) {
            HcMassStockManualDO manual = HcMassStockManualDO.builder()
                    .tenantId(tenantId)
                    .modelCode(modelCode)
                    .motherBatchNo(motherBatchNo)
                    .motherSegmentBatchNo(motherSegmentBatchNo)
                    .semResult(semResult)
                    .remark(remark)
                    .build();
            hcMassStockManualMapper.insert(manual);
            return true;
        }
        existed.setSemResult(semResult);
        existed.setRemark(remark);
        hcMassStockManualMapper.updateById(existed);
        return true;
    }

    @Override
    public List<HcMassStockGoodStockRespVO> getGoodStockList(String modelCode, String motherBatchNo,
                                                            String motherSegmentBatchNo) {
        return hcMassStockMapper.selectGoodStockList(currentTenantId(),
                StrUtil.trimToNull(modelCode),
                normalizeRequired(motherBatchNo, "母卷批号不能为空"),
                normalizeRequired(motherSegmentBatchNo, "母卷段号不能为空"));
    }

    @Override
    public List<HcMassStockShippingDetailRespVO> getShippingDetailList(String metric, String modelCode,
                                                                      String motherBatchNo,
                                                                      String motherSegmentBatchNo) {
        Long tenantId = currentTenantId();
        String normalizedMetric = normalizeRequired(metric, "明细类型不能为空").toLowerCase(Locale.ROOT);
        String normalizedModelCode = StrUtil.trimToNull(modelCode);
        String normalizedMotherBatchNo = normalizeRequired(motherBatchNo, "母卷批号不能为空");
        String normalizedMotherSegmentBatchNo = normalizeRequired(motherSegmentBatchNo, "母卷段号不能为空");
        if ("demand".equals(normalizedMetric)) {
            return hcMassStockMapper.selectDemandDetailList(tenantId, normalizedModelCode,
                    normalizedMotherBatchNo, normalizedMotherSegmentBatchNo);
        }
        if ("picked".equals(normalizedMetric)) {
            return hcMassStockMapper.selectPickedDetailList(tenantId, normalizedModelCode,
                    normalizedMotherBatchNo, normalizedMotherSegmentBatchNo);
        }
        if ("inspection".equals(normalizedMetric)) {
            return hcMassStockMapper.selectInspectionDetailList(tenantId, normalizedModelCode,
                    normalizedMotherBatchNo, normalizedMotherSegmentBatchNo);
        }
        throw invalidParamException("明细类型不正确");
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        try {
            return TenantContextHolder.getRequiredTenantId();
        } catch (Exception ignored) {
            return DEFAULT_TENANT_ID;
        }
    }

    private String normalizeRequired(String value, String message) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            throw invalidParamException(message);
        }
        return text;
    }

}
