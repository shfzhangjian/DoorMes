package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HcProcessReportServiceImplPressSlotNgRestartTest {
    private final HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();
    private final LocalDateTime base = LocalDateTime.of(2026, 9, 10, 10, 0);

    private QmsFaiOrderDO order(long id, String type, String status, String result, int submitted, int feedback) {
        QmsFaiOrderDO order = new QmsFaiOrderDO();
        order.setId(id);
        order.setSourceReportNo(type.equals("PROCESS") ? "PLAN-06-PROCESS-CHECK-PIECE" :
                type.equals("RELEASE") ? "PLAN-06-ABNORMAL-RELEASE-PIECE" : "PLAN-06");
        order.setTriggerReason(type.equals("RESTART") ? "PROCESS_CHECK_NG_RESTART" : "NEW_ORDER");
        order.setStatus(status);
        order.setJudgment(result);
        order.setSubmissionTime(base.plusMinutes(submitted));
        order.setQaTime(base.plusMinutes(feedback));
        return order;
    }

    private QmsFaiOrderDO pending(QmsFaiOrderDO... orders) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod("findPressSlotPendingRestartNg", List.class);
        method.setAccessible(true);
        return (QmsFaiOrderDO) method.invoke(service, List.of(orders));
    }

    @Test void processNgRequiresNewFirstInspectionDespitePreviousOk() throws Exception {
        var ng = order(2, "PROCESS", "COMPLETED", "NG", 10, 20);
        assertSame(ng, pending(order(1, "FIRST", "COMPLETED", "OK", 0, 1), ng));
    }
    @Test void submittingNewFirstInspectionImmediatelyResumes() throws Exception {
        var ng = order(2, "PROCESS", "COMPLETED", "NG", 10, 20);
        for (String status : List.of("PENDING", "INSPECTING", "WAITING_QA", "COMPLETED")) {
            assertNull(pending(ng, order(3, "RESTART", status, "PENDING", 21, 21)));
        }
    }
    @Test void repeatedNgRequiresAnotherFirstInspectionAndCanResumeAgain() throws Exception {
        var original = order(2, "PROCESS", "COMPLETED", "NG", 10, 20);
        var second = order(3, "RESTART", "COMPLETED", "NG", 21, 25);
        var third = order(4, "RESTART", "REJECTED", "NG", 26, 30);
        assertSame(second, pending(original, second));
        assertSame(third, pending(original, second, third));
        assertNull(pending(original, second, third, order(5, "RESTART", "PENDING", "PENDING", 31, 31)));
    }
    @Test void canceledOrRejectedRestartDoesNotResume() throws Exception {
        var ng = order(2, "PROCESS", "COMPLETED", "NG", 10, 20);
        for (String status : List.of("CANCELED", "REJECTED")) {
            assertSame(ng, pending(ng, order(3, "RESTART", status, "PENDING", 21, 21)));
        }
    }
    @Test void riskRetestOrAnotherProcessCheckCannotReplaceRestart() throws Exception {
        var ng = order(2, "PROCESS", "COMPLETED", "NG", 10, 20);
        assertSame(ng, pending(ng, order(3, "RELEASE", "COMPLETED", "OK", 21, 22)));
        assertSame(ng, pending(ng, order(3, "PROCESS", "COMPLETED", "OK", 21, 22)));
    }
    @Test void firstInspectionAfterNgSubmissionResumesEvenBeforeFeedback() throws Exception {
        var ng = order(2, "PROCESS", "COMPLETED", "NG", 10, 20);
        assertNull(pending(ng, order(3, "RESTART", "PENDING", "PENDING", 19, 19)));
    }
    @Test void laterFirstInspectionCoversMultipleEarlierSamplesWithLateFeedback() throws Exception {
        var first = order(2, "PROCESS", "REJECTED", "NG", 10, 40);
        var second = order(3, "PROCESS", "REJECTED", "NG", 20, 39);
        var third = order(4, "PROCESS", "REJECTED", "NG", 30, 38);
        assertSame(third, pending(first, second, third));
        assertNull(pending(first, second, third, order(5, "RESTART", "COMPLETED", "OK", 35, 50)));
        var next = order(6, "PROCESS", "REJECTED", "NG", 45, 55);
        assertSame(next, pending(first, second, third, order(5, "RESTART", "COMPLETED", "OK", 35, 50), next));
    }
    @Test void sameSecondOlderFirstInspectionCannotResume() throws Exception {
        var ng = order(3, "PROCESS", "REJECTED", "NG", 10, 20);
        assertSame(ng, pending(order(2, "RESTART", "COMPLETED", "OK", 10, 30), ng));
    }
    @Test void sameSecondNewerFirstInspectionResumesButNgItselfCannot() throws Exception {
        var ng = order(2, "RESTART", "COMPLETED", "NG", 10, 10);
        assertSame(ng, pending(ng));
        assertNull(pending(ng, order(3, "RESTART", "PENDING", "PENDING", 10, 10)));
    }
    @Test void pendingAndOkProcessChecksKeepOriginalBehavior() throws Exception {
        assertNull(pending(order(2, "PROCESS", "PENDING", "PENDING", 10, 10)));
        assertNull(pending(order(2, "PROCESS", "COMPLETED", "OK", 10, 20)));
        assertNull(pending(order(2, "PROCESS", "CANCELED", "NG", 10, 20)));
        assertNull(pending(order(2, "FIRST", "COMPLETED", "NG", 10, 20)));
    }
    @Test void ngConclusionRequiresRestartBeforeFinalCompletion() throws Exception {
        var ng = order(2, "RESTART", "WAITING_QA", "NG", 10, 20);
        assertSame(ng, pending(ng));
        assertNull(pending(ng, order(3, "RESTART", "PENDING", "PENDING", 21, 21)));
    }
}
