package cn.iocoder.yudao.module.mes.service.qms.task;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsIqcService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class QmsIqcTaskExecutionAdapter implements QmsTaskExecutionAdapter {

    @Resource
    private QmsIqcService qmsIqcService;

    @Override
    public String getCheckType() {
        return "IQC";
    }

    @Override
    public QmsTaskExecutionSnapshot load(Long executionId) {
        QmsIqcRespVO source = qmsIqcService.getIqcResp(executionId);
        if (source == null) {
            return null;
        }
        return QmsTaskExecutionSnapshot.builder()
                .checkType(getCheckType()).objectType("RECEIPT_BATCH")
                .sourceType("RECEIPT").sourceId(source.getReceiptId()).sourceNo(source.getReceiptNo())
                .executionId(source.getId()).executionNo(source.getIqcNo()).executionRoute("/mes/quality/iqc")
                .materialId(source.getMaterialId()).materialCode(source.getMaterialCode())
                .materialName(source.getMaterialName()).materialSpec(source.getSpecification())
                .productModel(source.getProductModelCode()).lotNo(source.getBatchNo())
                .checkQty(source.getReceiveQty()).unit(source.getUnit())
                .standardId(source.getStandardId()).standardNo(source.getStandardNo())
                .standardName(source.getStandardName()).standardVersion(source.getStandardVersion())
                .status(source.getStatus()).judgment(source.getJudgment())
                .inspectorName(source.getInspectorName()).inspectionTime(source.getInspectionTime())
                .itemCount(CollUtil.size(source.getItems())).abnormalCount(CollUtil.size(source.getAbnormals()))
                .build();
    }
}
