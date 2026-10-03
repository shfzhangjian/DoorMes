package cn.iocoder.yudao.module.mes.service.qms.task;

public interface QmsTaskExecutionAdapter {

    String getCheckType();

    QmsTaskExecutionSnapshot load(Long executionId);
}
