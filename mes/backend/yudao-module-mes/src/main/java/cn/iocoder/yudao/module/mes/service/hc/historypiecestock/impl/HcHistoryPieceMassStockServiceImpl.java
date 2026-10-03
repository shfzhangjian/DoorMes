package cn.iocoder.yudao.module.mes.service.hc.historypiecestock.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockGoodStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.historypiecestock.vo.HcHistoryPieceMassStockRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.historypiecestock.HcHistoryPieceMassStockMapper;
import cn.iocoder.yudao.module.mes.service.hc.historypiecestock.HcHistoryPieceMassStockService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcHistoryPieceMassStockServiceImpl implements HcHistoryPieceMassStockService {

    private static final long DEFAULT_TENANT_ID = 1L;
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    @Resource
    private HcHistoryPieceMassStockMapper hcHistoryPieceMassStockMapper;

    @Override
    public PageResult<HcHistoryPieceMassStockRespVO> getPage(HcHistoryPieceMassStockPageReqVO pageReqVO) {
        int pageNo = Math.max(pageReqVO.getPageNo() == null ? DEFAULT_PAGE_NO : pageReqVO.getPageNo(), 1);
        int pageSize = Math.min(
                Math.max(pageReqVO.getPageSize() == null ? DEFAULT_PAGE_SIZE : pageReqVO.getPageSize(), 1),
                MAX_PAGE_SIZE);
        Long tenantId = currentTenantId();
        long total = hcHistoryPieceMassStockMapper.selectCount(tenantId, pageReqVO);
        if (total <= 0) {
            return new PageResult<>(List.of(), 0L);
        }
        List<HcHistoryPieceMassStockRespVO> list = hcHistoryPieceMassStockMapper.selectPage(
                tenantId, pageReqVO, (pageNo - 1) * pageSize, pageSize);
        return new PageResult<>(list, total);
    }

    @Override
    public List<HcHistoryPieceMassStockGoodStockRespVO> getGoodStockList(String modelCode, String segmentBatchNo) {
        String normalizedSegmentBatchNo = StrUtil.trimToNull(segmentBatchNo);
        if (normalizedSegmentBatchNo == null) {
            throw invalidParamException("分段批号不能为空");
        }
        return hcHistoryPieceMassStockMapper.selectGoodStockList(currentTenantId(),
                StrUtil.trimToNull(modelCode), normalizedSegmentBatchNo);
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

}
