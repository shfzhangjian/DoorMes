package cn.iocoder.yudao.module.mes.service.qms.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsFqcService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class QmsFqcTaskExecutionAdapter implements QmsTaskExecutionAdapter {

    @Resource
    private QmsFqcService qmsFqcService;

    @Override
    public String getCheckType() {
        return "FQC";
    }

    @Override
    public QmsTaskExecutionSnapshot load(Long executionId) {
        QmsFqcRespVO source = qmsFqcService.getFqcResp(executionId);
        if (source == null) {
            return null;
        }
        return QmsTaskExecutionSnapshot.builder()
                .checkType(getCheckType()).objectType("PRODUCTION_BATCH")
                .sourceType(StrUtil.blankToDefault(source.getSourceModule(), "PLAN_ORDER"))
                .sourceId(source.getSourceReportId() != null ? source.getSourceReportId() : source.getPlanOrderId())
                .sourceNo(StrUtil.blankToDefault(source.getSourceReportNo(), source.getWorkOrderNo()))
                .executionId(source.getId()).executionNo(source.getFqcNo()).executionRoute("/mes/quality/fqc")
                .materialId(source.getMaterialId()).materialCode(source.getMaterialCode())
                .materialName(source.getMaterialName()).materialSpec(source.getSpecification())
                .productModel(source.getProductModel()).operationCode(source.getOperationCode())
                .operationName(source.getOperationName()).machineId(source.getMachineId())
                .machineCode(source.getMachineCode()).machineName(source.getMachineName())
                .lotNo(StrUtil.blankToDefault(source.getProductBatchNo(), source.getBatchNo()))
                .checkQty(source.getProduceQty()).unit(source.getUnitName())
                .standardId(source.getStandardId()).standardNo(source.getStandardNo())
                .standardName(source.getStandardNo()).standardVersion(source.getStandardVersion())
                .status(source.getStatus()).judgment(source.getJudgment())
                .inspectorName(source.getInspectorName()).inspectionTime(source.getInspectionTime())
                .itemCount(CollUtil.size(source.getItems())).abnormalCount(CollUtil.size(source.getAbnormals()))
                .build();
    }
}
