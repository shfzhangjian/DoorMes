package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiSheetImportBatchDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiSheetImportBatchMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QmsFaiImportBatchSummaryTest {
    @InjectMocks private QmsFaiServiceImpl service;
    @Mock private QmsFaiSheetImportBatchMapper qmsFaiSheetImportBatchMapper;

    @Test void longMessagesKeepFullDetailsWithoutOverflowingSummary() throws Exception {
        verifySummary("SUCCESS", 200, 0, 200);
    }

    @Test void failedImportAlsoKeepsBoundedSummaryAndFullDetails() throws Exception {
        verifySummary("FAILED", 0, 200, 0);
    }

    private void verifySummary(String status, int success, int failure, int warning) throws Exception {
        QmsFaiOrderDO order = new QmsFaiOrderDO();
        order.setId(1098L);
        order.setFaiNo("TEST-FAI-1098");
        var plan = new QmsFaiItemWorkbookService.ItemImportPlan(order, Map.of(), "test-hash",
                "test.xlsx", false, false);
        plan.successCount = success;
        plan.failureCount = failure;
        plan.warningCount = warning;
        for (int i = 1; i <= 200; i++) {
            plan.messages.add("第" + i + "行检验项目：校验消息，保留完整原因用于追溯");
        }
        String details = JsonUtils.toJsonString(plan.messages);
        assertTrue(details.length() > 1000);
        var method = QmsFaiServiceImpl.class.getDeclaredMethod("saveItemImportBatch",
                QmsFaiItemWorkbookService.ItemImportPlan.class, String.class, String.class);
        method.setAccessible(true);
        method.invoke(service, plan, "TEST-IMPORT", status);
        var captor = ArgumentCaptor.forClass(QmsFaiSheetImportBatchDO.class);
        verify(qmsFaiSheetImportBatchMapper).insert(captor.capture());
        var batch = captor.getValue();
        assertTrue(batch.getValidateSummary().length() <= 1000);
        assertEquals("成功 " + success + " 行，失败 " + failure + " 行，警告 " + warning + " 条",
                batch.getValidateSummary());
        assertEquals(details, batch.getErrorSummary());
        assertEquals(status, batch.getStatus());
    }
}
