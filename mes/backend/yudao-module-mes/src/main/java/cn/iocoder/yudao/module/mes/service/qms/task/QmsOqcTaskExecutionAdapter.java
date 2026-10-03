package cn.iocoder.yudao.module.mes.service.qms.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcShippingDetailMapper;
import cn.iocoder.yudao.module.mes.service.qms.QmsOqcService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QmsOqcTaskExecutionAdapter implements QmsTaskExecutionAdapter {

    @Resource
    private QmsOqcService qmsOqcService;
    @Resource
    private QmsFqcOrderMapper fqcOrderMapper;
    @Resource
    private QmsFqcItemMapper fqcItemMapper;
    @Resource
    private QmsFqcShippingDetailMapper fqcShippingDetailMapper;

    @Override
    public String getCheckType() {
        return "OQC";
    }

    @Override
    public QmsTaskExecutionSnapshot load(Long executionId) {
        QmsFqcOrderDO shippingFqc = fqcOrderMapper.selectById(executionId);
        if (shippingFqc != null
                && QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC.equals(shippingFqc.getSourceModule())) {
            return loadShippingFqc(shippingFqc);
        }
        QmsOqcRespVO source = qmsOqcService.getOqcResp(executionId);
        if (source == null) {
            return null;
        }
        return QmsTaskExecutionSnapshot.builder()
                .checkType(getCheckType()).objectType("SHIPPING_BATCH")
                .sourceType("SHIPPING_NOTICE")
                .sourceId(source.getShippingNoticeItemId() != null
                        ? source.getShippingNoticeItemId() : source.getShippingNoticeId())
                .sourceNo(StrUtil.blankToDefault(source.getNoticeNo(), source.getShippingNo()))
                .executionId(source.getId()).executionNo(source.getOqcNo()).executionRoute("/mes/quality/oqc")
                .materialId(source.getMaterialId()).materialCode(source.getMaterialCode())
                .materialName(source.getMaterialName()).materialSpec(source.getSpecification())
                .productModel(source.getModelCode())
                .lotNo(StrUtil.blankToDefault(source.getCustomerBatchNo(), source.getBatchNo()))
                .checkQty(source.getShippingQty()).unit(source.getUnitName())
                .standardId(source.getStandardId()).standardNo(source.getStandardNo())
                .standardName(source.getStandardNo()).standardVersion(source.getStandardVersion())
                .status(source.getStatus()).judgment(source.getJudgment())
                .inspectorName(source.getInspectorName()).inspectionTime(source.getInspectionTime())
                .itemCount(CollUtil.size(source.getItems())).abnormalCount(CollUtil.size(source.getAbnormals()))
                .build();
    }

    private QmsTaskExecutionSnapshot loadShippingFqc(QmsFqcOrderDO source) {
        List<QmsFqcShippingDetailDO> details = fqcShippingDetailMapper.selectListByFqcId(source.getId());
        QmsFqcShippingDetailDO first = CollUtil.getFirst(details);
        Integer checkedQty = valueOrZero(source.getOkQty()) + valueOrZero(source.getNgQty());
        if (checkedQty <= 0) {
            checkedQty = source.getSampleQty();
        }
        if ((checkedQty == null || checkedQty <= 0) && CollUtil.isNotEmpty(details)) {
            checkedQty = details.stream()
                    .map(QmsFqcShippingDetailDO::getShippingQty)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
        }
        return QmsTaskExecutionSnapshot.builder()
                .checkType(getCheckType()).objectType("SHIPPING_BATCH")
                .sourceType(QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC)
                .sourceId(first == null ? source.getSourceReportId() : first.getShippingNoticeId())
                .sourceNo(first == null ? source.getSourceReportNo() : first.getShippingNoticeNo())
                .executionId(source.getId()).executionNo(source.getFqcNo())
                .executionRoute("/mes/quality/fg-shipping-fqc")
                .materialId(source.getMaterialId())
                .materialCode(StrUtil.blankToDefault(source.getMaterialCode(),
                        first == null ? null : first.getMaterialCode()))
                .materialName(StrUtil.blankToDefault(source.getMaterialName(),
                        first == null ? null : first.getMaterialName()))
                .materialSpec(StrUtil.blankToDefault(source.getSpecification(),
                        first == null ? null : first.getProductSize()))
                .productModel(StrUtil.blankToDefault(source.getProductModel(),
                        first == null ? null : first.getModelCode()))
                .operationCode(source.getOperationCode()).operationName(source.getOperationName())
                .machineId(source.getMachineId()).machineCode(source.getMachineCode())
                .machineName(source.getMachineName())
                .lotNo(StrUtil.blankToDefault(source.getProductBatchNo(), source.getBatchNo()))
                .checkQty(checkedQty == null ? null : BigDecimal.valueOf(checkedQty)).unit("片")
                .standardId(source.getStandardId()).standardNo(source.getStandardNo())
                .standardName(source.getStandardNo()).standardVersion(source.getStandardVersion())
                .status(source.getStatus()).judgment(source.getJudgment())
                .inspectorName(source.getInspectorName()).inspectionTime(source.getInspectionTime())
                .itemCount(CollUtil.size(fqcItemMapper.selectListByFqcId(source.getId())))
                .abnormalCount(valueOrZero(source.getNgQty()))
                .build();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
