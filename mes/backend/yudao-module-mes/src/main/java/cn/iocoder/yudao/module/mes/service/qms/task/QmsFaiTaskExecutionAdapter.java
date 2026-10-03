package cn.iocoder.yudao.module.mes.service.qms.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsFaiService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class QmsFaiTaskExecutionAdapter implements QmsTaskExecutionAdapter {

    private static final String GLUE_BOARD_FAI = "GLUE_BOARD_FAI";

    @Resource
    private QmsFaiService qmsFaiService;

    @Override
    public String getCheckType() {
        return "FAI";
    }

    @Override
    public QmsTaskExecutionSnapshot load(Long executionId) {
        QmsFaiRespVO source = qmsFaiService.getFaiResp(executionId);
        if (source == null || GLUE_BOARD_FAI.equalsIgnoreCase(StrUtil.trimToEmpty(source.getSourceModule()))) {
            return null;
        }
        return QmsTaskExecutionSnapshot.builder()
                .checkType(getCheckType()).objectType("PRODUCTION_BATCH")
                .sourceType(StrUtil.blankToDefault(source.getSourceModule(), "PLAN_ORDER"))
                .sourceId(source.getSourceReportId() != null ? source.getSourceReportId() : source.getPlanOrderId())
                .sourceNo(StrUtil.blankToDefault(source.getSourceReportNo(), source.getWorkOrderNo()))
                .executionId(source.getId()).executionNo(source.getFaiNo()).executionRoute("/mes/quality/fai")
                .materialId(source.getMaterialId()).materialCode(source.getMaterialCode())
                .materialName(source.getMaterialName()).materialSpec(source.getSpecification())
                .productModel(source.getProductModel()).operationCode(source.getOperationCode())
                .operationName(source.getOperationName()).machineId(source.getMachineId())
                .machineCode(source.getMachineCode()).machineName(source.getMachineName())
                .lotNo(source.getProductBatchNo()).checkQty(source.getInspectionQty())
                .standardId(source.getStandardId()).standardNo(source.getStandardNo())
                .standardName(source.getStandardNo()).standardVersion(source.getStandardVersion())
                .status(source.getStatus()).judgment(source.getJudgment())
                .inspectorName(StrUtil.blankToDefault(source.getQaInspectorName(), source.getOperatorName()))
                .inspectionTime(source.getInspectionTime())
                .itemCount(CollUtil.size(source.getItems())).abnormalCount(CollUtil.size(source.getAbnormals()))
                .build();
    }
}
