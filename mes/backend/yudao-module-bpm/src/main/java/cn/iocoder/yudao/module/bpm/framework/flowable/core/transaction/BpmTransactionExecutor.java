package cn.iocoder.yudao.module.bpm.framework.flowable.core.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 使用独立物理事务执行 BPM 的事务后置操作。
 */
@Component
public class BpmTransactionExecutor {

    private final TransactionTemplate requiresNewTransactionTemplate;

    public BpmTransactionExecutor(PlatformTransactionManager transactionManager) {
        requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void executeRequiresNew(Runnable action) {
        requiresNewTransactionTemplate.executeWithoutResult(status -> action.run());
    }

}
