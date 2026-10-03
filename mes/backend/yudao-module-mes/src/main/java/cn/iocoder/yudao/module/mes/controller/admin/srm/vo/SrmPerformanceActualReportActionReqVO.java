package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

public final class SrmPerformanceActualReportActionReqVO {

    private SrmPerformanceActualReportActionReqVO() {
    }

    @Data
    public static class Confirm {
        @NotNull(message = "实际上报单ID不能为空")
        private Long reportId;
        private String confirmOpinion;
    }

}
