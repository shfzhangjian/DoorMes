package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - IPQC检验标准带出 Response VO")
@Data
public class QmsIpqcStandardRespVO {
    private Long standardId;
    private String standardNo;
    private String standardName;
    private String version;
    private String applyType;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private String processCode;
    private String processName;
    private List<StandardItem> items;

    @Data
    public static class StandardItem {
        private Long standardItemId;
        private String category;
        private String inspectionItem;
        private String itemType;
        private BigDecimal targetValue;
        private String standardDesc;
        private String inspectionMethod;
        private String testFrequencyJudgement;
        private String testTool;
        private Integer sampleSize;
        private BigDecimal minValueLimit;
        private BigDecimal maxValueLimit;
        private Boolean isSpc;
        private Integer sort;
    }
}
