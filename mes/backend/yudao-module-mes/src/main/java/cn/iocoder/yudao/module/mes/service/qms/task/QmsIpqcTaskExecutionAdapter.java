package cn.iocoder.yudao.module.mes.service.qms.task;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIpqcRespVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsIpqcService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class QmsIpqcTaskExecutionAdapter implements QmsTaskExecutionAdapter {

    @Resource
    private QmsIpqcService qmsIpqcService;

    @Override
    public String getCheckType() {
        return "IPQC";
    }

    @Override
    public QmsTaskExecutionSnapshot load(Long executionId) {
        QmsIpqcRespVO source = qmsIpqcService.getIpqcResp(executionId);
        if (source == null) {
            return null;
        }
        return QmsTaskExecutionSnapshot.builder()
                .checkType(getCheckType()).objectType("PLAN_OPERATION")
                .sourceType("PLAN_ORDER").sourceId(source.getPlanOrderId()).sourceNo(source.getWorkOrderNo())
                .executionId(source.getId()).executionNo(source.getIpqcNo()).executionRoute("/mes/quality/ipqc")
                .materialId(source.getMaterialId()).materialCode(source.getMaterialCode())
                .materialName(source.getMaterialName()).materialSpec(source.getSpecification())
                .operationCode(source.getOperationCode()).operationName(source.getOperationName())
                .machineId(source.getMachineId()).machineCode(source.getMachineCode()).machineName(source.getMachineName())
                .standardId(source.getStandardId()).standardNo(source.getStandardNo())
                .standardName(source.getStandardNo()).standardVersion(source.getStandardVersion())
                .status(source.getStatus()).judgment(source.getJudgment())
                .inspectorName(source.getInspectorName()).inspectionTime(source.getInspectionTime())
                .itemCount(CollUtil.size(source.getItems())).abnormalCount(CollUtil.size(source.getAbnormals()))
                .build();
    }
}
