package cn.iocoder.yudao.module.mes.service.qms.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFaiRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsFaiService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class QmsGlueBoardFaiTaskExecutionAdapter implements QmsTaskExecutionAdapter {

    private static final String CHECK_TYPE = "GLUE_BOARD_FAI";

    @Resource
    private QmsFaiService qmsFaiService;

    @Override
    public String getCheckType() {
        return CHECK_TYPE;
    }

    @Override
    public QmsTaskExecutionSnapshot load(Long executionId) {
        QmsFaiRespVO source = qmsFaiService.getFaiResp(executionId);
        if (source == null || !CHECK_TYPE.equalsIgnoreCase(StrUtil.trimToEmpty(source.getSourceModule()))) {
            return null;
        }
        return QmsTaskExecutionSnapshot.builder()
                .checkType(getCheckType()).objectType("GLUE_BOARD_BATCH")
                .sourceType(CHECK_TYPE)
                .sourceId(source.getSourceReportId() != null
                        ? source.getSourceReportId() : source.getGlueBoardStockId())
                .sourceNo(StrUtil.blankToDefault(source.getSourceReportNo(), source.getGluePlateBatchNo()))
                .executionId(source.getId()).executionNo(source.getFaiNo())
                .executionRoute("/mes/quality/glue-board-fai")
                .materialId(source.getMaterialId()).materialCode(source.getGlueBoardMaterialCode())
                .materialName(source.getMaterialName()).materialSpec(source.getSpecification())
                .productModel(source.getGlueBoardModel()).operationCode(source.getOperationCode())
                .operationName(source.getOperationName()).machineId(source.getMachineId())
                .machineCode(source.getMachineCode()).machineName(source.getMachineName())
                .lotNo(source.getGluePlateBatchNo())
                .checkQty(source.getSampleLength() != null ? source.getSampleLength() : source.getInspectionQty())
                .unit("m").standardId(source.getStandardId()).standardNo(source.getStandardNo())
                .standardName(source.getStandardNo()).standardVersion(source.getStandardVersion())
                .status(source.getStatus()).judgment(source.getJudgment())
                .inspectorName(StrUtil.blankToDefault(source.getQaInspectorName(), source.getOperatorName()))
                .inspectionTime(source.getInspectionTime())
                .itemCount(CollUtil.size(source.getItems())).abnormalCount(CollUtil.size(source.getAbnormals()))
                .build();
    }
}
