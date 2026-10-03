package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class SrmPreliminaryEvaluationSaveReqVO {

    private Long id;
    @NotEmpty(message = "初评单号不能为空")
    private String evaluationNo;
    private Long supplierId;
    @NotEmpty(message = "供应商编号不能为空")
    private String supplierCode;
    @NotEmpty(message = "供应商名称不能为空")
    private String supplierName;
    private String supplierSourceType;
    @NotNull(message = "请选择初评项目")
    private Long projectId;
    private String projectCode;
    private String projectName;
    @NotNull(message = "请选择已发布的评估模板")
    private Long templateVersionId;
    private BigDecimal totalScoreBaseline;
    private BigDecimal qualificationScoreSnapshot;
    private String remark;
    private Integer version;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        private Long templateItemId;
        private String groupCodeSnapshot;
        private String indicatorCodeSnapshot;
        private Long scorerUserId;
        private String scorerCandidateUserIds;
    }

}
